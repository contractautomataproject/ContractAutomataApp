
package io.github.contractautomata.catapp;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import io.github.contractautomata.catlib.automaton.label.action.Action;
import org.w3c.dom.Document;

import com.mxgraph.canvas.mxICanvas;
import com.mxgraph.canvas.mxSvgCanvas;
import com.mxgraph.io.mxCodec;
import com.mxgraph.io.mxGdCodec;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxCellRenderer.CanvasFactory;
import com.mxgraph.util.mxDomUtils;
import com.mxgraph.util.mxResources;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.util.png.mxPngEncodeParam;
import com.mxgraph.util.png.mxPngImageEncoder;
import com.mxgraph.util.png.mxPngTextDecoder;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStyleRegistry;

import io.github.contractautomata.catapp.castate.MxState;
import io.github.contractautomata.catapp.converters.MxeConverter;
import io.github.contractautomata.catlib.automaton.Automaton;
import io.github.contractautomata.catlib.automaton.label.CALabel;
import io.github.contractautomata.catlib.automaton.state.State;
import io.github.contractautomata.catlib.converters.AutDataConverter;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition;

/**
 *
 */
public class EditorActions
{
	/**
	 * 
	 * @return Returns the graph for the given action event.
	 */
	public static BasicGraphEditor getEditor(ActionEvent e)
	{
		if (e.getSource() instanceof Component)
		{
			Component component = (Component) e.getSource();

			while (component != null
					&& !(component instanceof BasicGraphEditor))
			{
				component = component.getParent();
			}

			return (BasicGraphEditor) component;
		}

		return null;
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class ExitAction extends AbstractAction
	{
		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e)
		{
			BasicGraphEditor editor = getEditor(e);

			if (editor != null)
			{
				editor.exit();
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class ScaleAction extends AbstractAction
	{
		/**
		 * 
		 */
		protected final double scale;

		/**
		 * 
		 */
		public ScaleAction(double scale)
		{
			this.scale = scale;
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e)
		{
			if (e.getSource() instanceof mxGraphComponent)
			{
				mxGraphComponent graphComponent = (mxGraphComponent) e
						.getSource();
				double scale = this.scale;

				if (scale == 0)
				{
					String value = (String) JOptionPane.showInputDialog(
							graphComponent, mxResources.get("value"),
							mxResources.get("scale") + " (%)",
							JOptionPane.PLAIN_MESSAGE, null, null, "");

					if (value != null)
					{
						scale = Double.parseDouble(value.replace("%", "")) / 100;
					}
				}

				if (scale > 0)
				{
					graphComponent.zoomTo(scale, graphComponent.isCenterZoom());
				}
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class PageSetupAction extends AbstractAction
	{
		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e)
		{
			if (e.getSource() instanceof mxGraphComponent)
			{
				mxGraphComponent graphComponent = (mxGraphComponent) e
						.getSource();
				PrinterJob pj = PrinterJob.getPrinterJob();
				PageFormat format = pj.pageDialog(graphComponent
						.getPageFormat());

				if (format != null)
				{
					graphComponent.setPageFormat(format);
					graphComponent.zoomAndCenter();
				}
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")

	public static class PrintAction extends AbstractAction
	{
		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e)
		{
			if (e.getSource() instanceof mxGraphComponent)
			{
				mxGraphComponent graphComponent = (mxGraphComponent) e
						.getSource();
				PrinterJob pj = PrinterJob.getPrinterJob();

				if (pj.printDialog())
				{
					PageFormat pf = graphComponent.getPageFormat();
					Paper paper = new Paper();
					double margin = 36;
					paper.setImageableArea(margin, margin, paper.getWidth()
							- margin * 2, paper.getHeight() - margin * 2);
					pf.setPaper(paper);
					pj.setPrintable(graphComponent, pf);

					try
					{
						pj.print();
					}
					catch (PrinterException e2)
					{
						e2.printStackTrace();
						System.out.println(e2.toString());
					}
				}
			}
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class SaveAction extends AbstractAction
	{
		/**
		 * 
		 */
		protected final boolean showDialog;

		/**
		 * 
		 */
		protected String lastDir = null;

		/**
		 * 
		 */
		public SaveAction(boolean showDialog)
		{
			this.showDialog = showDialog;
		}

		/**
		 * Saves XML+PNG format.
		 */
		protected void saveXmlPng(BasicGraphEditor editor, String filename,
				Color bg) throws IOException
		{

			saveMxe(editor,filename+".mxe"); //necessary for instantiating lastaut
			new File(filename+".mxe").delete();

			mxGraphComponent graphComponent = editor.getGraphComponent();
			mxGraph graph = graphComponent.getGraph();

			// Creates the image for the PNG file
			BufferedImage image = mxCellRenderer.createBufferedImage(graph,
					null, 1, bg, graphComponent.isAntiAlias(), null,
					graphComponent.getCanvas());

			// Creates the URL-encoded XML data
			mxCodec codec = new mxCodec();
			String xml = URLEncoder.encode(
					mxXmlUtils.getXml(codec.encode(graph.getModel())), "UTF-8");
			mxPngEncodeParam param = mxPngEncodeParam
					.getDefaultEncodeParam(image);
			param.setCompressedText(new String[] { "mxGraphModel", xml });

			// Saves as a PNG file
			try (FileOutputStream outputStream = new FileOutputStream(new File(filename))) {
				mxPngImageEncoder encoder = new mxPngImageEncoder(outputStream,
						param);

				if (image != null) {
					encoder.encode(image);
					editor.setModified(false);
					editor.setCurrentFile(new File(filename));
				} else {
					JOptionPane.showMessageDialog(graphComponent,
							mxResources.get("noImageData"));
				}
			}
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e)
		{
			BasicGraphEditor editor = getEditor(e);

			if (editor != null)
			{
				mxGraphComponent graphComponent = editor.getGraphComponent();
				//				mxGraph graph = graphComponent.getGraph();
				String filename = null;
				String ext = null;
				//				boolean dialogShown = false;

				if (showDialog || editor.getCurrentFile() == null)
				{
					String wd;

					if (lastDir != null)
					{
						wd = lastDir;
					}
					else if (editor.getCurrentFile() != null)
					{
						wd = editor.getCurrentFile().getParent();
					}
					else
					{
						wd = System.getProperty("user.dir");
					}

					JFileChooser fc = new JFileChooser(wd);

					FileFilter selectedFilter = null;
					DefaultFileFilter defaultFilter = new DefaultFileFilter(".mxe",
							"Contract Automata mxGraph Editor " + mxResources.get("file")
							+ " (.mxe)");

					fc.setFileFilter(defaultFilter);
					// Adds the default file format
					fc.addChoosableFileFilter(defaultFilter);
					

					fc.addChoosableFileFilter(new DefaultFileFilter(".data",
							"Contract Automata Textual Representation " + mxResources.get("file") + " (.data)"));

					fc.addChoosableFileFilter(new DefaultFileFilter(".png",
							"PNG " + mxResources.get("file") + " (.png)"));

					// Adds special vector graphics formats
					fc.addChoosableFileFilter(new DefaultFileFilter(".svg",
							"SVG " + mxResources.get("file") + " (.svg)"));


					// Adds filter that accepts all supported image formats
					//					fc.addChoosableFileFilter(new DefaultFileFilter.ImageFileFilter(
					//							mxResources.get("allImages")));
					int rc = fc.showDialog(null, mxResources.get("save"));
					//	dialogShown = true;

					if (rc != JFileChooser.APPROVE_OPTION)
					{
						return;
					}
					else
					{
						lastDir = fc.getSelectedFile().getParent();
					}

					filename = fc.getSelectedFile().getAbsolutePath();
					selectedFilter = fc.getFileFilter();

					if (selectedFilter instanceof DefaultFileFilter)
					{
						ext = ((DefaultFileFilter) selectedFilter)
								.getExtension();

						if (!filename.toLowerCase().endsWith(ext))
						{
							filename += ext;
						}
					}

					if (new File(filename).exists()
							&& JOptionPane.showConfirmDialog(graphComponent,
									mxResources.get("overwriteExistingFile")) != JOptionPane.YES_OPTION)
					{
						return;
					}
				}
				else
				{
					filename = editor.getCurrentFile().getAbsolutePath();
				}

				try
				{
					ext=filename.substring(filename.lastIndexOf('.') + 1);	
					if (ext.equalsIgnoreCase("svg"))
					{
						mxGraph graph = editor.getGraphComponent().getGraph();
						graph.selectAll();

						saveMxe(editor,filename+".mxe"); //necessary for instantiating lastaut

						//cannot draw custom shapes in SVG, so it draws a small arrow to the initial state
						
						Consumer<mxCell> resetGeometry = c -> c.setGeometry(new mxGeometry(c.getGeometry().getX()+MxState.initialStateWidthIncrement,c.getGeometry().getY(),
								c.getGeometry().getWidth()-MxState.initialStateWidthIncrement,c.getGeometry().getHeight()));
						
						String in = (String) mxStyleRegistry.getValue("SHAPE_INITIALSTATE");
						String infin = (String) mxStyleRegistry.getValue("SHAPE_INITIALFINALSTATE");
						List<mxCell> tempInitial = Arrays.stream(graph.getSelectionCells())
						.map(c->(mxCell)c)
						.filter(c->c.getStyle()!=null)
						.filter(c->c.getStyle().contains(in))
						.peek(resetGeometry)
						.peek(c->c.setStyle(MxState.nodestylevalue))
						.collect(Collectors.toList());

						List<mxCell> tempInitialFinal = Arrays.stream(graph.getSelectionCells())
						.map(c->(mxCell)c)
						.filter(c->c.getStyle()!=null)
						.filter(c->c.getStyle().contains(infin))
						.peek(resetGeometry)
						.peek(c->c.setStyle(MxState.finalnodestylevalue))
						.collect(Collectors.toList());
						
						mxCell initial=Stream.concat(tempInitial.stream(),tempInitialFinal.stream())
						.findFirst().orElseThrow(RuntimeException::new); //the initial state cell found
				
						mxCell edgeinit = (mxCell) graph.insertEdge(graph.getDefaultParent(), null, null, null, null, EditorToolBar.edgestylevalue);
						edgeinit.getGeometry().getSourcePoint().setX(initial.getGeometry().getX()-MxState.initialStateWidthIncrement);
						edgeinit.getGeometry().getSourcePoint().setY(initial.getGeometry().getCenterY());
						edgeinit.getGeometry().getTargetPoint().setX(initial.getGeometry().getX());
						edgeinit.getGeometry().getTargetPoint().setY(initial.getGeometry().getCenterY());
						edgeinit.setValue("");
						graph.selectAll();
						
						mxSvgCanvas canvas = (mxSvgCanvas) mxCellRenderer
								.drawCells(editor.getGraphComponent().getGraph(), null, 1, null,
										new CanvasFactory()
										{
											public mxICanvas createCanvas(
													int width, int height)
											{
												mxSvgCanvas canvas = new mxSvgCanvas(
														mxDomUtils.createSvgDocument(
																width, height));
												canvas.setEmbedded(true);

												return canvas;
											}
										});
						
						mxUtils.writeFile(mxXmlUtils.getXml(canvas.getDocument()),
								filename);
						
						Consumer<mxCell> setGeometry = c -> c.setGeometry(new mxGeometry(c.getGeometry().getX()-MxState.initialStateWidthIncrement,c.getGeometry().getY(),
								c.getGeometry().getWidth()+MxState.initialStateWidthIncrement,c.getGeometry().getHeight()));
				
						
						//restoring
						tempInitial.parallelStream()
						.peek(setGeometry)
						.forEach(c->c.setStyle(MxState.initialnodestylevalue));

						tempInitialFinal.parallelStream()
						.peek(setGeometry)
						.forEach(c->c.setStyle(MxState.initialfinalnodestylevalue));
						
						graph.removeCells(new Object[] {edgeinit});
						graph.clearSelection();
						graph.refresh();
						editor.setModified(false);

						//deleting the temporary mxe file
						File f = new File(filename+".mxe");
						f.delete();
						editor.setCurrentFile(new File(filename));

					}
					else if (ext.equalsIgnoreCase("mxe")
							|| ext.equalsIgnoreCase("xml"))
					{
						saveMxe(editor,filename);
					}
					else if (ext.equalsIgnoreCase("data")) 
					{
						saveMxe(editor,filename+".mxe"); //necessary for instantiating lastaut
						try {
							Automaton<String,Action,State<String>,ModalTransition<String, Action,State<String>,CALabel>> aut=((App) editor).lastaut;
							new AutDataConverter<>(CALabel::new).exportMSCA(filename,aut);
							editor.setModified(false);
							JOptionPane.showMessageDialog(editor.getGraphComponent(),"The automaton has been stored with filename "+filename,"Success!",JOptionPane.PLAIN_MESSAGE);
						} catch (IOException e1) {
							JOptionPane.showMessageDialog(editor.getGraphComponent(),"File not found"+e1,mxResources.get("error"),JOptionPane.ERROR_MESSAGE);
						}
						//deleting the temporary mxe file
						File f = new File(filename+".mxe");
						f.delete();
						editor.setCurrentFile(new File(filename));
					}
					Color bg = null;

					if ((!ext.equalsIgnoreCase("gif") && !ext
							.equalsIgnoreCase("png"))
							|| JOptionPane.showConfirmDialog(
									graphComponent, mxResources
									.get("transparentBackground")) != JOptionPane.YES_OPTION)
					{
						bg = graphComponent.getBackground();
					}

					if (//selectedFilter == xmlPngFilter ||
							(editor.getCurrentFile() != null
							&& ext.equalsIgnoreCase("png")))// && !dialogShown))
					{
						saveXmlPng(editor, filename, bg);
					}
				}
				catch (Throwable ex)
				{
					ex.printStackTrace();
					JOptionPane.showMessageDialog(graphComponent,
							ex.toString(), mxResources.get("error"),
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}

		private void saveMxe(BasicGraphEditor editor, String filename) throws IOException {
			mxCodec codec = new mxCodec();
			mxGraph graph = editor.getGraphComponent().getGraph();

			String xml = mxXmlUtils.getXml(codec.encode(graph
					.getModel()));

			mxUtils.writeFile(xml, filename);

			if (editor instanceof App)
			{
				App app = ((App)editor);
				EditorMenuBar menuBar = (EditorMenuBar) app.getMenuFrame().getJMenuBar();
				try {
					app.lastaut=new MxeConverter().importMSCA(filename);
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(editor.getGraphComponent(),menuBar.getErrorMsg()+System.lineSeparator()+ex.getMessage(), mxResources.get("error"),JOptionPane.ERROR_MESSAGE);
					throw new IOException(ex);
				}
			}
			editor.setModified(false);
			editor.setCurrentFile(new File(filename));
		}
	}

	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class HistoryAction extends AbstractAction
	{
		/**
		 * 
		 */
		protected final boolean undo;

		/**
		 * 
		 */
		public HistoryAction(boolean undo)
		{
			this.undo = undo;
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e)
		{
			BasicGraphEditor editor = getEditor(e);

			if (editor != null)
			{
				if (undo)
				{
					editor.getUndoManager().undo();
				}
				else
				{
					editor.getUndoManager().redo();
				}
			}
		}
	}


	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class NewAction extends AbstractAction
	{
		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e)
		{
			BasicGraphEditor editor = getEditor(e);

			if (editor != null)
			{
				if (!editor.isModified()
						|| JOptionPane.showConfirmDialog(editor,
								mxResources.get("loseChanges")) == JOptionPane.YES_OPTION)
				{
					mxGraph graph = editor.getGraphComponent().getGraph();

					// Check modified flag and display save dialog
					mxCell root = new mxCell();
					root.insert(new mxCell());
					graph.getModel().setRoot(root);

					editor.setModified(false);
					editor.setCurrentFile(null);
					editor.getGraphComponent().zoomAndCenter();
				}
			}
		}
	}


	/**
	 *
	 */
	@SuppressWarnings("serial")
	public static class OpenAction extends AbstractAction
	{
		/**
		 * 
		 */
		protected String lastDir;

		/**
		 * 
		 */
		protected void resetEditor(BasicGraphEditor editor)
		{
			editor.setModified(false);
			editor.getUndoManager().clear();
			editor.getGraphComponent().zoomAndCenter();
		}

		/**
		 * Reads XML+PNG format.
		 */
//		protected void openXmlPng(BasicGraphEditor editor, File file)
//				throws IOException
//		{
//			Map<String, String> text = mxPngTextDecoder
//					.decodeCompressedText(new FileInputStream(file));
//
//			if (text != null)
//			{
//				String value = text.get("mxGraphModel");
//
//				if (value != null)
//				{
//					Document document = mxXmlUtils.parseXml(URLDecoder.decode(
//							value, "UTF-8"));
//					mxCodec codec = new mxCodec(document);
//					codec.decode(document.getDocumentElement(), editor
//							.getGraphComponent().getGraph().getModel());
//					editor.setCurrentFile(file);
//					resetEditor(editor);
//					return;
//				}
//			}
//
//			JOptionPane.showMessageDialog(editor,
//					mxResources.get("imageContainsNoDiagramData"));
//		}

		/**
		 *
		 */
		protected void openGD(BasicGraphEditor editor, File file,
				String gdText)
		{
			mxGraph graph = editor.getGraphComponent().getGraph();

			// Replaces file extension with .mxe
			String filename = file.getName();
			filename = filename.substring(0, filename.length() - 4) + ".mxe";

			if (new File(filename).exists()
					&& JOptionPane.showConfirmDialog(editor,
							mxResources.get("overwriteExistingFile")) != JOptionPane.YES_OPTION)
			{
				return;
			}

			((mxGraphModel) graph.getModel()).clear();
			mxGdCodec.decode(gdText, graph);
			editor.getGraphComponent().zoomAndCenter();
			editor.setCurrentFile(new File(lastDir + "/" + filename));
		}

		/**
		 * 
		 */
		public void actionPerformed(ActionEvent e)
		{
			BasicGraphEditor editor = getEditor(e);

			if (editor != null)
			{
				if (!editor.isModified()
						|| JOptionPane.showConfirmDialog(editor,
								mxResources.get("loseChanges")) == JOptionPane.YES_OPTION)
				{
					mxGraph graph = editor.getGraphComponent().getGraph();

					if (graph != null)
					{
						String wd = (lastDir != null) ? lastDir : System
								.getProperty("user.dir");

						JFileChooser fc = new JFileChooser(wd);

						// Adds file filter for supported file format
						DefaultFileFilter defaultFilter = new DefaultFileFilter(
								".mxe", mxResources.get("allSupportedFormats")
								+ " (.mxe, .png, .data)")
						{

							public boolean accept(File file)
							{
								String lcase = file.getName().toLowerCase();

								return super.accept(file)
										|| lcase.endsWith(".png")
										|| lcase.endsWith(".data");
							}
						};
						fc.addChoosableFileFilter(defaultFilter);

						fc.addChoosableFileFilter(new DefaultFileFilter(".mxe",
								"Contract Automata mxGraph Editor " + mxResources.get("file")
								+ " (.mxe)"));
						fc.addChoosableFileFilter(new DefaultFileFilter(".data",
								"Contract Automata Textual Representation " + mxResources.get("file")
								+ " (.data)"));
//						fc.addChoosableFileFilter(new DefaultFileFilter(".png",
//								"PNG+XML  " + mxResources.get("file")
//								+ " (.png)"));

						// Adds file filter for VDX import
//						fc.addChoosableFileFilter(new DefaultFileFilter(".vdx",
//								"XML Drawing  " + mxResources.get("file")
//								+ " (.vdx)"));

						// Adds file filter for GD import
//						fc.addChoosableFileFilter(new DefaultFileFilter(".txt",
//								"Graph Drawing  " + mxResources.get("file")
//								+ " (.txt)"));

						fc.setFileFilter(defaultFilter);

						int rc = fc.showDialog(null,
								mxResources.get("openFile"));

						if (rc == JFileChooser.APPROVE_OPTION)
						{
							lastDir = fc.getSelectedFile().getParent();

							try
							{
//								if (fc.getSelectedFile().getAbsolutePath()
//										.toLowerCase().endsWith(".png"))
//								{
//									openXmlPng(editor, fc.getSelectedFile());
//								}
								//else
									if (fc.getSelectedFile().getAbsolutePath()
								.toLowerCase().endsWith(".data"))
								{
									EditorMenuBar menuBar = (EditorMenuBar) ((App) editor).getMenuFrame().getJMenuBar();
									menuBar.lastDir = fc.getSelectedFile().getParent();	
									Automaton<String,Action,State<String>,ModalTransition<String,Action,State<String>,CALabel>> aut;
									try {
										String filename = fc.getSelectedFile().toString();
										aut = new AutDataConverter<>(CALabel::new).importMSCA(filename);
										filename = filename.substring(0,filename.lastIndexOf("."));
										new MxeConverter().exportMSCA(filename,aut); //when importing it also exports

										filename=filename.endsWith(".mxe")?filename:(filename+".mxe"); 
										File file = new File(filename);
										((App) editor).lastaut=aut;
										menuBar.loadMorphStore(file.getName(), editor, file);
										editor.setCurrentFile(file);
									} catch (Exception e1) {
										JOptionPane.showMessageDialog(editor.getGraphComponent(),e1.toString(),
												mxResources.get("error"),JOptionPane.ERROR_MESSAGE);
									}
									resetEditor(editor);
								}
//								else if (fc.getSelectedFile().getAbsolutePath()
//										.toLowerCase().endsWith(".txt"))
//								{
//									openGD(editor, fc.getSelectedFile(),
//											mxUtils.readFile(fc
//													.getSelectedFile()
//													.getAbsolutePath()));
//								}
								else {
									Document document = mxXmlUtils
											.parseXml(mxUtils.readFile(fc
													.getSelectedFile()
													.getAbsolutePath()));

									mxCodec codec = new mxCodec(document);
									codec.decode(
											document.getDocumentElement(),
											graph.getModel());
									
									editor.setCurrentFile(fc
											.getSelectedFile());
								}
							}
							catch (IOException ex)
							{
								ex.printStackTrace();
								JOptionPane.showMessageDialog(
										editor.getGraphComponent(),
										ex.toString(),
										mxResources.get("error"),
										JOptionPane.ERROR_MESSAGE);
							}
						}
					}
				}
			}
		}
	}

}























//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class ToggleRulersItem extends JCheckBoxMenuItem
//{
//	/**
//	 * 
//	 */
//	public ToggleRulersItem(final BasicGraphEditor editor, String name)
//	{
//		super(name);
//		setSelected(editor.getGraphComponent().getColumnHeader() != null);
//
//		addActionListener(new ActionListener()
//		{
//			/**
//			 * 
//			 */
//			public void actionPerformed(ActionEvent e)
//			{
//				mxGraphComponent graphComponent = editor
//						.getGraphComponent();
//
//				if (graphComponent.getColumnHeader() != null)
//				{
//					graphComponent.setColumnHeader(null);
//					graphComponent.setRowHeader(null);
//				}
//				else
//				{
//					graphComponent.setColumnHeaderView(new EditorRuler(
//							graphComponent,
//							EditorRuler.ORIENTATION_HORIZONTAL));
//					graphComponent.setRowHeaderView(new EditorRuler(
//							graphComponent,
//							EditorRuler.ORIENTATION_VERTICAL));
//				}
//			}
//		});
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class ToggleGridItem extends JCheckBoxMenuItem
//{
//	/**
//	 * 
//	 */
//	public ToggleGridItem(final BasicGraphEditor editor, String name)
//	{
//		super(name);
//		setSelected(true);
//
//		addActionListener(new ActionListener()
//		{
//			/**
//			 * 
//			 */
//			public void actionPerformed(ActionEvent e)
//			{
//				mxGraphComponent graphComponent = editor
//						.getGraphComponent();
//				mxGraph graph = graphComponent.getGraph();
//				boolean enabled = !graph.isGridEnabled();
//
//				graph.setGridEnabled(enabled);
//				graphComponent.setGridVisible(enabled);
//				graphComponent.repaint();
//				setSelected(enabled);
//			}
//		});
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class ToggleOutlineItem extends JCheckBoxMenuItem
//{
//	/**
//	 * 
//	 */
//	public ToggleOutlineItem(final BasicGraphEditor editor, String name)
//	{
//		super(name);
//		setSelected(true);
//
//		addActionListener(new ActionListener()
//		{
//			/**
//			 * 
//			 */
//			public void actionPerformed(ActionEvent e)
//			{
//				final mxGraphOutline outline = editor.getGraphOutline();
//				outline.setVisible(!outline.isVisible());
//				outline.revalidate();
//
//				SwingUtilities.invokeLater(new Runnable()
//				{
//					/*
//					 * (non-Javadoc)
//					 * @see java.lang.Runnable#run()
//					 */
//					public void run()
//					{
//						if (outline.getParent() instanceof JSplitPane)
//						{
//							if (outline.isVisible())
//							{
//								((JSplitPane) outline.getParent())
//								.setDividerLocation(editor
//										.getHeight() - 300);
//								((JSplitPane) outline.getParent())
//								.setDividerSize(6);
//							}
//							else
//							{
//								((JSplitPane) outline.getParent())
//								.setDividerSize(0);
//							}
//						}
//					}
//				});
//			}
//		});
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class StylesheetAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	protected String stylesheet;
//
//	/**
//	 * 
//	 */
//	public StylesheetAction(String stylesheet)
//	{
//		this.stylesheet = stylesheet;
//	}
//
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		if (e.getSource() instanceof mxGraphComponent)
//		{
//			mxGraphComponent graphComponent = (mxGraphComponent) e
//					.getSource();
//			mxGraph graph = graphComponent.getGraph();
//			mxCodec codec = new mxCodec();
//			Document doc = mxUtils.loadDocument(EditorActions.class
//					.getResource(stylesheet).toString());
//
//			if (doc != null)
//			{
//				codec.decode(doc.getDocumentElement(),
//						graph.getStylesheet());
//				graph.refresh();
//			}
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class ZoomPolicyAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	protected int zoomPolicy;
//
//	/**
//	 * 
//	 */
//	public ZoomPolicyAction(int zoomPolicy)
//	{
//		this.zoomPolicy = zoomPolicy;
//	}
//
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		if (e.getSource() instanceof mxGraphComponent)
//		{
//			mxGraphComponent graphComponent = (mxGraphComponent) e
//					.getSource();
//			graphComponent.setPageVisible(true);
//			graphComponent.setZoomPolicy(zoomPolicy);
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class GridStyleAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	protected int style;
//
//	/**
//	 * 
//	 */
//	public GridStyleAction(int style)
//	{
//		this.style = style;
//	}
//
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		if (e.getSource() instanceof mxGraphComponent)
//		{
//			mxGraphComponent graphComponent = (mxGraphComponent) e
//					.getSource();
//			graphComponent.setGridStyle(style);
//			graphComponent.repaint();
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class GridColorAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		if (e.getSource() instanceof mxGraphComponent)
//		{
//			mxGraphComponent graphComponent = (mxGraphComponent) e
//					.getSource();
//			Color newColor = JColorChooser.showDialog(graphComponent,
//					mxResources.get("gridColor"),
//					graphComponent.getGridColor());
//
//			if (newColor != null)
//			{
//				graphComponent.setGridColor(newColor);
//				graphComponent.repaint();
//			}
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class SelectShortestPathAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	protected boolean directed;
//
//	/**
//	 * 
//	 */
//	public SelectShortestPathAction(boolean directed)
//	{
//		this.directed = directed;
//	}
//
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		if (e.getSource() instanceof mxGraphComponent)
//		{
//			mxGraphComponent graphComponent = (mxGraphComponent) e
//					.getSource();
//			mxGraph graph = graphComponent.getGraph();
//			mxIGraphModel model = graph.getModel();
//
//			Object source = null;
//			Object target = null;
//
//			Object[] cells = graph.getSelectionCells();
//
//			for (int i = 0; i < cells.length; i++)
//			{
//				if (model.isVertex(cells[i]))
//				{
//					if (source == null)
//					{
//						source = cells[i];
//					}
//					else if (target == null)
//					{
//						target = cells[i];
//					}
//				}
//
//				if (source != null && target != null)
//				{
//					break;
//				}
//			}
//
//			if (source != null && target != null)
//			{
//				int steps = graph.getChildEdges(graph.getDefaultParent()).length;
//				Object[] path = mxGraphAnalysis.getInstance()
//						.getShortestPath(graph, source, target,
//								new mxDistanceCostFunction(), steps,
//								directed);
//				graph.setSelectionCells(path);
//			}
//			else
//			{
//				JOptionPane.showMessageDialog(graphComponent,
//						mxResources.get("noSourceAndTargetSelected"));
//			}
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class SelectSpanningTreeAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	protected boolean directed;
//
//	/**
//	 * 
//	 */
//	public SelectSpanningTreeAction(boolean directed)
//	{
//		this.directed = directed;
//	}
//
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		if (e.getSource() instanceof mxGraphComponent)
//		{
//			mxGraphComponent graphComponent = (mxGraphComponent) e
//					.getSource();
//			mxGraph graph = graphComponent.getGraph();
//			mxIGraphModel model = graph.getModel();
//
//			Object parent = graph.getDefaultParent();
//			Object[] cells = graph.getSelectionCells();
//
//			for (int i = 0; i < cells.length; i++)
//			{
//				if (model.getChildCount(cells[i]) > 0)
//				{
//					parent = cells[i];
//					break;
//				}
//			}
//
//			Object[] v = graph.getChildVertices(parent);
//			Object[] mst = mxGraphAnalysis.getInstance()
//					.getMinimumSpanningTree(graph, v,
//							new mxDistanceCostFunction(), directed);
//			graph.setSelectionCells(mst);
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class ToggleDirtyAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		if (e.getSource() instanceof mxGraphComponent)
//		{
//			mxGraphComponent graphComponent = (mxGraphComponent) e
//					.getSource();
//			graphComponent.showDirtyRectangle = !graphComponent.showDirtyRectangle;
//		}
//	}
//
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class ToggleConnectModeAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		if (e.getSource() instanceof mxGraphComponent)
//		{
//			mxGraphComponent graphComponent = (mxGraphComponent) e
//					.getSource();
//			mxConnectionHandler handler = graphComponent
//					.getConnectionHandler();
//			handler.setHandleEnabled(!handler.isHandleEnabled());
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class ToggleCreateTargetItem extends JCheckBoxMenuItem
//{
//	/**
//	 * 
//	 */
//	public ToggleCreateTargetItem(final BasicGraphEditor editor, String name)
//	{
//		super(name);
//		setSelected(true);
//
//		addActionListener(new ActionListener()
//		{
//			/**
//			 * 
//			 */
//			public void actionPerformed(ActionEvent e)
//			{
//				mxGraphComponent graphComponent = editor
//						.getGraphComponent();
//
//				if (graphComponent != null)
//				{
//					mxConnectionHandler handler = graphComponent
//							.getConnectionHandler();
//					handler.setCreateTarget(!handler.isCreateTarget());
//					setSelected(handler.isCreateTarget());
//				}
//			}
//		});
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class PromptPropertyAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	protected Object target;
//
//	/**
//	 * 
//	 */
//	protected String fieldname, message;
//
//	/**
//	 * 
//	 */
//	public PromptPropertyAction(Object target, String message)
//	{
//		this(target, message, message);
//	}
//
//	/**
//	 * 
//	 */
//	public PromptPropertyAction(Object target, String message,
//			String fieldname)
//	{
//		this.target = target;
//		this.message = message;
//		this.fieldname = fieldname;
//	}
//
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		if (e.getSource() instanceof Component)
//		{
//			try
//			{
//				Method getter = target.getClass().getMethod(
//						"get" + fieldname);
//				Object current = getter.invoke(target);
//
//				// : Support other atomic types
//				if (current instanceof Integer)
//				{
//					Method setter = target.getClass().getMethod(
//							"set" + fieldname, new Class[] { int.class });
//
//					String value = (String) JOptionPane.showInputDialog(
//							(Component) e.getSource(), "Value", message,
//							JOptionPane.PLAIN_MESSAGE, null, null, current);
//
//					if (value != null)
//					{
//						setter.invoke(target, Integer.parseInt(value));
//					}
//				}
//			}
//			catch (Exception ex)
//			{
//				ex.printStackTrace();
//			}
//		}
//
//		// Repaints the graph component
//		if (e.getSource() instanceof mxGraphComponent)
//		{
//			mxGraphComponent graphComponent = (mxGraphComponent) e
//					.getSource();
//			graphComponent.repaint();
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class TogglePropertyItem extends JCheckBoxMenuItem
//{
//	/**
//	 * 
//	 */
//	public TogglePropertyItem(Object target, String name, String fieldname)
//	{
//		this(target, name, fieldname, false);
//	}
//
//	/**
//	 * 
//	 */
//	public TogglePropertyItem(Object target, String name, String fieldname,
//			boolean refresh)
//	{
//		this(target, name, fieldname, refresh, null);
//	}
//
//	/**
//	 * 
//	 */
//	public TogglePropertyItem(final Object target, String name,
//			final String fieldname, final boolean refresh,
//			ActionListener listener)
//	{
//		super(name);
//
//		// Since action listeners are processed last to first we add the given
//		// listener here which means it will be processed after the one below
//		if (listener != null)
//		{
//			addActionListener(listener);
//		}
//
//		addActionListener(new ActionListener()
//		{
//			/**
//			 * 
//			 */
//			public void actionPerformed(ActionEvent e)
//			{
//				execute(target, fieldname, refresh);
//			}
//		});
//
//		PropertyChangeListener propertyChangeListener = new PropertyChangeListener()
//		{
//
//			/*
//			 * (non-Javadoc)
//			 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
//			 */
//			public void propertyChange(PropertyChangeEvent evt)
//			{
//				if (evt.getPropertyName().equalsIgnoreCase(fieldname))
//				{
//					update(target, fieldname);
//				}
//			}
//		};
//
//		if (target instanceof mxGraphComponent)
//		{
//			((mxGraphComponent) target)
//			.addPropertyChangeListener(propertyChangeListener);
//		}
//		else if (target instanceof mxGraph)
//		{
//			((mxGraph) target)
//			.addPropertyChangeListener(propertyChangeListener);
//		}
//
//		update(target, fieldname);
//	}
//
//	/**
//	 * 
//	 */
//	public void update(Object target, String fieldname)
//	{
//		if (target != null && fieldname != null)
//		{
//			try
//			{
//				Method getter = target.getClass().getMethod(
//						"is" + fieldname);
//
//				if (getter != null)
//				{
//					Object current = getter.invoke(target);
//
//					if (current instanceof Boolean)
//					{
//						setSelected(((Boolean) current).booleanValue());
//					}
//				}
//			}
//			catch (Exception e)
//			{
//				// ignore
//			}
//		}
//	}
//
//	/**
//	 * 
//	 */
//	public void execute(Object target, String fieldname, boolean refresh)
//	{
//		if (target != null && fieldname != null)
//		{
//			try
//			{
//				Method getter = target.getClass().getMethod(
//						"is" + fieldname);
//				Method setter = target.getClass().getMethod(
//						"set" + fieldname, new Class[] { boolean.class });
//
//				Object current = getter.invoke(target);
//
//				if (current instanceof Boolean)
//				{
//					boolean value = !((Boolean) current).booleanValue();
//					setter.invoke(target, value);
//					setSelected(value);
//				}
//
//				if (refresh)
//				{
//					mxGraph graph = null;
//
//					if (target instanceof mxGraph)
//					{
//						graph = (mxGraph) target;
//					}
//					else if (target instanceof mxGraphComponent)
//					{
//						graph = ((mxGraphComponent) target).getGraph();
//					}
//
//					graph.refresh();
//				}
//			}
//			catch (Exception e)
//			{
//				// ignore
//			}
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class FontStyleAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	protected boolean bold;
//
//	/**
//	 * 
//	 */
//	public FontStyleAction(boolean bold)
//	{
//		this.bold = bold;
//	}
//
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		if (e.getSource() instanceof mxGraphComponent)
//		{
//			mxGraphComponent graphComponent = (mxGraphComponent) e
//					.getSource();
//			Component editorComponent = null;
//
//			if (graphComponent.getCellEditor() instanceof mxCellEditor)
//			{
//				editorComponent = ((mxCellEditor) graphComponent
//						.getCellEditor()).getEditor();
//			}
//
//			if (editorComponent instanceof JEditorPane)
//			{
//				JEditorPane editorPane = (JEditorPane) editorComponent;
//				int start = editorPane.getSelectionStart();
//				int ende = editorPane.getSelectionEnd();
//				String text = editorPane.getSelectedText();
//
//				if (text == null)
//				{
//					text = "";
//				}
//
//				try
//				{
//					HTMLEditorKit editorKit = new HTMLEditorKit();
//					HTMLDocument document = (HTMLDocument) editorPane
//							.getDocument();
//					document.remove(start, (ende - start));
//					editorKit.insertHTML(document, start, ((bold) ? "<b>"
//							: "<i>") + text + ((bold) ? "</b>" : "</i>"),
//							0, 0, (bold) ? HTML.Tag.B : HTML.Tag.I);
//				}
//				catch (Exception ex)
//				{
//					ex.printStackTrace();
//				}
//
//				editorPane.requestFocus();
//				editorPane.select(start, ende);
//			}
//			else
//			{
//				mxIGraphModel model = graphComponent.getGraph().getModel();
//				model.beginUpdate();
//				try
//				{
//					graphComponent.stopEditing(false);
//					graphComponent.getGraph().toggleCellStyleFlags(
//							mxConstants.STYLE_FONTSTYLE,
//							(bold) ? mxConstants.FONT_BOLD
//									: mxConstants.FONT_ITALIC);
//				}
//				finally
//				{
//					model.endUpdate();
//				}
//			}
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class WarningAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		if (e.getSource() instanceof mxGraphComponent)
//		{
//			mxGraphComponent graphComponent = (mxGraphComponent) e
//					.getSource();
//			Object[] cells = graphComponent.getGraph().getSelectionCells();
//
//			if (cells != null && cells.length > 0)
//			{
//				String warning = JOptionPane.showInputDialog(mxResources
//						.get("enterWarningMessage"));
//
//				for (int i = 0; i < cells.length; i++)
//				{
//					graphComponent.setCellWarning(cells[i], warning);
//				}
//			}
//			else
//			{
//				JOptionPane.showMessageDialog(graphComponent,
//						mxResources.get("noCellSelected"));
//			}
//		}
//	}
//}
//

//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class ToggleAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	protected String key;
//
//	/**
//	 * 
//	 */
//	protected boolean defaultValue;
//
//	/**
//	 * 
//	 * @param key
//	 */
//	public ToggleAction(String key)
//	{
//		this(key, false);
//	}
//
//	/**
//	 * 
//	 * @param key
//	 */
//	public ToggleAction(String key, boolean defaultValue)
//	{
//		this.key = key;
//		this.defaultValue = defaultValue;
//	}
//
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		mxGraph graph = mxGraphActions.getGraph(e);
//
//		if (graph != null)
//		{
//			graph.toggleCellStyles(key, defaultValue);
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class SetLabelPositionAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	protected String labelPosition, alignment;
//
//	/**
//	 * 
//	 * @param key
//	 */
//	public SetLabelPositionAction(String labelPosition, String alignment)
//	{
//		this.labelPosition = labelPosition;
//		this.alignment = alignment;
//	}
//
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		mxGraph graph = mxGraphActions.getGraph(e);
//
//		if (graph != null && !graph.isSelectionEmpty())
//		{
//			graph.getModel().beginUpdate();
//			try
//			{
//				// Checks the orientation of the alignment to use the correct constants
//				if (labelPosition.equals(mxConstants.ALIGN_LEFT)
//						|| labelPosition.equals(mxConstants.ALIGN_CENTER)
//						|| labelPosition.equals(mxConstants.ALIGN_RIGHT))
//				{
//					graph.setCellStyles(mxConstants.STYLE_LABEL_POSITION,
//							labelPosition);
//					graph.setCellStyles(mxConstants.STYLE_ALIGN, alignment);
//				}
//				else
//				{
//					graph.setCellStyles(
//							mxConstants.STYLE_VERTICAL_LABEL_POSITION,
//							labelPosition);
//					graph.setCellStyles(mxConstants.STYLE_VERTICAL_ALIGN,
//							alignment);
//				}
//			}
//			finally
//			{
//				graph.getModel().endUpdate();
//			}
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class SetStyleAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	protected String value;
//
//	/**
//	 * 
//	 * @param key
//	 */
//	public SetStyleAction(String value)
//	{
//		this.value = value;
//	}
//
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		mxGraph graph = mxGraphActions.getGraph(e);
//
//		if (graph != null && !graph.isSelectionEmpty())
//		{
//			graph.setCellStyle(value);
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class KeyValueAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	protected String key, value;
//
//	/**
//	 * 
//	 * @param key
//	 */
//	public KeyValueAction(String key)
//	{
//		this(key, null);
//	}
//
//	/**
//	 * 
//	 * @param key
//	 */
//	public KeyValueAction(String key, String value)
//	{
//		this.key = key;
//		this.value = value;
//	}
//
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		mxGraph graph = mxGraphActions.getGraph(e);
//
//		if (graph != null && !graph.isSelectionEmpty())
//		{
//			graph.setCellStyles(key, value);
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class PromptValueAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	protected String key, message;
//
//	/**
//	 * 
//	 * @param key
//	 */
//	public PromptValueAction(String key, String message)
//	{
//		this.key = key;
//		this.message = message;
//	}
//
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		if (e.getSource() instanceof Component)
//		{
//			mxGraph graph = mxGraphActions.getGraph(e);
//
//			if (graph != null && !graph.isSelectionEmpty())
//			{
//				String value = (String) JOptionPane.showInputDialog(
//						(Component) e.getSource(),
//						mxResources.get("value"), message,
//						JOptionPane.PLAIN_MESSAGE, null, null, "");
//
//				if (value != null)
//				{
//					if (value.equals(mxConstants.NONE))
//					{
//						value = null;
//					}
//
//					graph.setCellStyles(key, value);
//				}
//			}
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class AlignCellsAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	protected String align;
//
//	/**
//	 * 
//	 * @param key
//	 */
//	public AlignCellsAction(String align)
//	{
//		this.align = align;
//	}
//
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		mxGraph graph = mxGraphActions.getGraph(e);
//
//		if (graph != null && !graph.isSelectionEmpty())
//		{
//			graph.alignCells(align);
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class AutosizeAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		mxGraph graph = mxGraphActions.getGraph(e);
//
//		if (graph != null && !graph.isSelectionEmpty())
//		{
//			Object[] cells = graph.getSelectionCells();
//			mxIGraphModel model = graph.getModel();
//
//			model.beginUpdate();
//			try
//			{
//				for (int i = 0; i < cells.length; i++)
//				{
//					graph.updateCellSize(cells[i]);
//				}
//			}
//			finally
//			{
//				model.endUpdate();
//			}
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class ColorAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	protected String name, key;
//
//	/**
//	 * 
//	 * @param key
//	 */
//	public ColorAction(String name, String key)
//	{
//		this.name = name;
//		this.key = key;
//	}
//
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		if (e.getSource() instanceof mxGraphComponent)
//		{
//			mxGraphComponent graphComponent = (mxGraphComponent) e
//					.getSource();
//			mxGraph graph = graphComponent.getGraph();
//
//			if (!graph.isSelectionEmpty())
//			{
//				Color newColor = JColorChooser.showDialog(graphComponent,
//						name, null);
//
//				if (newColor != null)
//				{
//					graph.setCellStyles(key, mxUtils.hexString(newColor));
//				}
//			}
//		}
//	}
//
//
//}
//
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class BackgroundImageAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		if (e.getSource() instanceof mxGraphComponent)
//		{
//			mxGraphComponent graphComponent = (mxGraphComponent) e
//					.getSource();
//			String value = (String) JOptionPane.showInputDialog(
//					graphComponent, mxResources.get("backgroundImage"),
//					"URL", JOptionPane.PLAIN_MESSAGE, null, null,
//					"http://www.callatecs.com/images/background2.JPG");
//
//			if (value != null)
//			{
//				if (value.length() == 0)
//				{
//					graphComponent.setBackgroundImage(null);
//				}
//				else
//				{
//					Image background = mxUtils.loadImage(value);
//					// Incorrect URLs will result in no image.
//					// todo provide feedback that the URL is not correct
//					if (background != null)
//					{
//						graphComponent.setBackgroundImage(new ImageIcon(
//								background));
//					}
//				}
//
//				// Forces a repaint of the outline
//				graphComponent.getGraph().repaint();
//			}
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class BackgroundAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		if (e.getSource() instanceof mxGraphComponent)
//		{
//			mxGraphComponent graphComponent = (mxGraphComponent) e
//					.getSource();
//			Color newColor = JColorChooser.showDialog(graphComponent,
//					mxResources.get("background"), null);
//
//			if (newColor != null)
//			{
//				graphComponent.getViewport().setOpaque(true);
//				graphComponent.getViewport().setBackground(newColor);
//			}
//
//			// Forces a repaint of the outline
//			graphComponent.getGraph().repaint();
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class PageBackgroundAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		if (e.getSource() instanceof mxGraphComponent)
//		{
//			mxGraphComponent graphComponent = (mxGraphComponent) e
//					.getSource();
//			Color newColor = JColorChooser.showDialog(graphComponent,
//					mxResources.get("pageBackground"), null);
//
//			if (newColor != null)
//			{
//				graphComponent.setPageBackgroundColor(newColor);
//			}
//
//			// Forces a repaint of the component
//			graphComponent.repaint();
//		}
//	}
//}
//
///**
// *
// */
//@SuppressWarnings("serial")
//public static class StyleAction extends AbstractAction
//{
//	/**
//	 * 
//	 */
//	public void actionPerformed(ActionEvent e)
//	{
//		if (e.getSource() instanceof mxGraphComponent)
//		{
//			mxGraphComponent graphComponent = (mxGraphComponent) e
//					.getSource();
//			mxGraph graph = graphComponent.getGraph();
//			String initial = graph.getModel().getStyle(
//					graph.getSelectionCell());
//			String value = (String) JOptionPane.showInputDialog(
//					graphComponent, mxResources.get("style"),
//					mxResources.get("style"), JOptionPane.PLAIN_MESSAGE,
//					null, null, initial);
//
//			if (value != null)
//			{
//				graph.setCellStyle(value);
//			}
//		}
//	}
//}
