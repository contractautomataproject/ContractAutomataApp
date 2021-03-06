//package io.github.contractautomataproject.catapp.actions.deprecated;
//
//import java.awt.event.ActionEvent;
//import java.io.File;
//
//import javax.swing.AbstractAction;
//import javax.swing.JFileChooser;
//import javax.swing.JOptionPane;
//
//import com.mxgraph.util.mxResources;
//import com.mxgraph.view.mxGraph;
//
//import io.github.contractautomataproject.catapp.App;
//import io.github.contractautomataproject.catapp.EditorActions;
//import io.github.contractautomataproject.catapp.EditorMenuBar;
//import io.github.contractautomataproject.catapp.converters.MxeConverter;
//import io.github.contractautomataproject.catlib.automaton.Automaton;
//import io.github.contractautomataproject.catlib.automaton.label.CALabel;
//import io.github.contractautomataproject.catlib.automaton.state.State;
//import io.github.contractautomataproject.catlib.converters.AutDataConverter;
//import io.github.contractautomataproject.catlib.transition.ModalTransition;
//
//@SuppressWarnings("serial")
//public class ImportData extends AbstractAction {
//
//	@Override
//	public void actionPerformed(ActionEvent e) {
//		App editor = (App) EditorActions.getEditor(e);
//		EditorMenuBar menuBar = (EditorMenuBar) editor.getMenuFrame().getJMenuBar();
//
//		if (!menuBar.loseChanges.test(editor)) return;
//
//		mxGraph graph = editor.getGraphComponent().getGraph();
//		if (graph == null) return;
//
//		JFileChooser fc = new JFileChooser(
//				(editor.getCurrentFile()!=null)?editor.getCurrentFile().getParent(): System.getProperty("user.dir"));
//
//		// Adds file filter for supported file format
//		menuBar.setDefaultFilter(fc,".data","FMCA description",null);
//
//		int rc = fc.showDialog(null,
//				mxResources.get("openFile"));
//		if (rc == JFileChooser.APPROVE_OPTION)
//		{
//			menuBar.lastDir = fc.getSelectedFile().getParent();
//			Automaton<String,String,State<String>,ModalTransition<String,String,State<String>,CALabel>> aut;
//			try {
//				String filename = fc.getSelectedFile().toString();
//				aut = new AutDataConverter<CALabel>(CALabel::new).importMSCA(filename);
//				filename = filename.substring(0,filename.lastIndexOf("."));
//				new MxeConverter().exportMSCA(filename,aut);
//
//				filename=filename.endsWith(".mxe")?filename:(filename+".mxe"); //filename = filename+".mxe";
//				File file = new File(filename);
//				editor.lastaut=aut;
//				menuBar.loadMorphStore(file.getName(), editor, file);
//			} catch (Exception e1) {
//				JOptionPane.showMessageDialog(editor.getGraphComponent(),e1.toString(),mxResources.get("error"),JOptionPane.ERROR_MESSAGE);
//			}
//		}
//
//
//	}
//
//}
