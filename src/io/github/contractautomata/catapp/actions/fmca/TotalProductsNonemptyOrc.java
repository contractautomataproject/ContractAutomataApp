package io.github.contractautomata.catapp.actions.fmca;

import com.mxgraph.util.mxResources;
import io.github.contractautomata.catapp.App;
import io.github.contractautomata.catapp.EditorActions;
import io.github.contractautomata.catapp.EditorMenuBar;
import io.github.contractautomata.catapp.ProductFrame;
import io.github.contractautomata.catlib.automaton.Automaton;
import io.github.contractautomata.catlib.automaton.label.CALabel;
import io.github.contractautomata.catlib.automaton.label.action.Action;
import io.github.contractautomata.catlib.automaton.state.State;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition;
import io.github.contractautomata.catlib.family.FMCA;
import io.github.contractautomata.catlib.family.Product;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("serial")
public class TotalProductsNonemptyOrc extends AbstractAction {

	@Override
	public void actionPerformed(ActionEvent e) {
		App editor = (App) EditorActions.getEditor(e);
		EditorMenuBar menuBar = (EditorMenuBar) Objects.requireNonNull(editor).getMenuFrame().getJMenuBar();
		
		if (menuBar.checkAut(editor)) return;

		menuBar.lastDir=editor.getCurrentFile().getParent();
		Automaton<String,Action,State<String>, ModalTransition<String, Action,State<String>,CALabel>> aut=editor.lastaut;

		ProductFrame pf=editor.getProductFrame();
		if (pf==null)
		{
			JOptionPane.showMessageDialog(editor.getGraphComponent(),"No Family loaded!",mxResources.get("error"),JOptionPane.ERROR_MESSAGE);
			return;
		}

		
		Instant start;

		Map<Product,Automaton<String,Action,State<String>,ModalTransition<String,Action,State<String>,CALabel>>> vpp;
		if (!aut.getForwardStar(aut.getInitial()).stream()
				.map(ModalTransition<String,Action,State<String>,CALabel>::getLabel)
				.allMatch(l->l.getAction().getLabel().equals("dummy")))
		{
			start = Instant.now();
			vpp=new FMCA(aut,pf.getFamily()).getTotalProductsWithNonemptyOrchestration();
		}
		else
		{
			JOptionPane.showMessageDialog(editor.getGraphComponent(),"Operation not supported for an orchestration of a family","",JOptionPane.WARNING_MESSAGE);
			return;
		}
		Instant stop = Instant.now();
		long elapsedTime = Duration.between(start, stop).toMillis();
	

		if (vpp==null)
		{			
			JOptionPane.showMessageDialog(editor.getGraphComponent(),"No Total Products With non-empty orchestration"+ System.lineSeparator()+"Elapsed time : "+elapsedTime+ " milliseconds","",JOptionPane.WARNING_MESSAGE);
			return;
		}

		pf.setColorButtonProducts(vpp.keySet(), Color.BLUE);
		StringBuilder message= new StringBuilder(vpp.size() + " Total Products With non-empty orchestration Found:" + System.lineSeparator());
		for (Product p : vpp.keySet())
			message.append(pf.indexOf(p)).append(" : ").append(System.lineSeparator()).append(p.toString()).append(System.lineSeparator());

		message.append("Elapsed time : ").append(elapsedTime).append(" milliseconds");
		JTextArea textArea = new JTextArea(200,200);
		textArea.setText(message.toString());
		textArea.setEditable(true);

		JScrollPane scrollPane = new JScrollPane(textArea);
		JDialog jd = new JDialog(pf);
		jd.add(scrollPane);
		jd.setTitle("Products With non-empty orchestration");
		jd.setResizable(true);
		jd.setVisible(true);

		jd.setSize(500,500);
		jd.setLocationRelativeTo(null);
		// JOptionPane.showMessageDialog(editor.getGraphComponent(), jd);
		//JOptionPane.showMessageDialog(editor.getGraphComponent(),message,"Valid Products",JOptionPane.PLAIN_MESSAGE);

		
	}

}
