package io.github.contractautomata.catapp.actions;

import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.function.Consumer;

import javax.swing.AbstractAction;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.shape.mxStencilShape;
import com.mxgraph.swing.mxGraphComponent;

import io.github.contractautomata.catapp.castate.MxState;

public class ToggleInitialStateAction extends AbstractAction
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final mxCell node;

	/**
	 *
	 */
	public ToggleInitialStateAction(mxCell node)
	{
		this.node=node;
	}

	public void actionPerformed(ActionEvent e)
	{
		mxGraphComponent graphComponent = (mxGraphComponent) e.getSource();
		
		Consumer<mxCell> reset = n->{
			if (MxState.isFinal.test(n))
				n.setStyle(MxState.finalnodestylevalue);
			else
				n.setStyle(MxState.nodestylevalue);
			
			double x=n.getGeometry().getX();
			double y=n.getGeometry().getY();
			n.setGeometry(new mxGeometry(x, y, 40, 40));
		};

		if (MxState.isInitial.test(node))
			reset.accept(node);//reset
		else {
			graphComponent.getGraph().selectAll();
			Arrays.stream(graphComponent.getGraph().getSelectionCells())
			.map(x->(mxCell)x)
			.filter(x->x!=null && MxState.isInitial.test(x))
			.forEach(reset);

			if (MxState.isFinal.test(node))
				node.setStyle(MxState.initialfinalnodestylevalue); 
			else
				node.setStyle(MxState.initialnodestylevalue);
	
			double x=node.getGeometry().getX();
			double y=node.getGeometry().getY();
			node.setGeometry(new mxGeometry(x, y, 40+MxState.initialStateWidthIncrement, 40));

			graphComponent.getGraph().clearSelection();
			graphComponent.getGraph().addSelectionCell(node);
		}
		graphComponent.refresh();
	}


	public static String addStencilShape(String nodeXml)
	{
		// Some editors place a 3 byte BOM at the start of files
		// Ensure the first char is a "<"
		int lessthanIndex = nodeXml.indexOf("<");
		nodeXml = nodeXml.substring(lessthanIndex);
		mxStencilShape newShape = new mxStencilShape(nodeXml);
		String name = newShape.getName();

		mxGraphics2DCanvas.putShape(name, newShape);
		return name;
	}

}
