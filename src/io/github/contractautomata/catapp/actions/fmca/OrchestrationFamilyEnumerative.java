package io.github.contractautomata.catapp.actions.fmca;

import com.mxgraph.util.mxResources;
import io.github.contractautomata.catapp.App;
import io.github.contractautomata.catapp.EditorActions;
import io.github.contractautomata.catapp.EditorMenuBar;
import io.github.contractautomata.catapp.ProductFrame;
import io.github.contractautomata.catapp.converters.MxeConverter;
import io.github.contractautomata.catlib.automaton.Automaton;
import io.github.contractautomata.catlib.automaton.label.CALabel;
import io.github.contractautomata.catlib.automaton.label.action.Action;
import io.github.contractautomata.catlib.automaton.state.State;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition;
import io.github.contractautomata.catlib.family.FMCA;
import io.github.contractautomata.catlib.family.Family;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@SuppressWarnings("serial")
public class OrchestrationFamilyEnumerative extends AbstractAction {

	@Override
	public void actionPerformed(ActionEvent e) {
		App editor = (App) EditorActions.getEditor(e);
		EditorMenuBar menuBar = (EditorMenuBar) Objects.requireNonNull(editor).getMenuFrame().getJMenuBar();
		if (menuBar.checkAut(editor)) return;
		String filename=editor.getCurrentFile().getName();

		ProductFrame pf=editor.getProductFrame();
		if (pf==null)
		{
			JOptionPane.showMessageDialog(editor.getGraphComponent(),"No Family loaded!",mxResources.get("error"), JOptionPane.ERROR_MESSAGE);
			return;
		}

		menuBar.lastDir=editor.getCurrentFile().getParent();
		Automaton<String,Action,State<String>, ModalTransition<String, Action,State<String>,CALabel>> aut=editor.lastaut;
		Family f=pf.getFamily();

		JOptionPane.showMessageDialog(editor.getGraphComponent(),"Warning : the enumerative computation may require several minutes!","Warning",JOptionPane.WARNING_MESSAGE);


		Instant start = Instant.now();
		Automaton<String,Action,State<String>,ModalTransition<String,Action,State<String>,CALabel>> controller = new FMCA(aut,f).getOrchestrationOfFamilyEnumerative();
		Instant stop = Instant.now();
		long elapsedTime = Duration.between(start, stop).toMillis();
	

		if (controller==null)
		{
			JOptionPane.showMessageDialog(editor.getGraphComponent(),"The orchestration is empty"+System.lineSeparator()+" Elapsed time : "+elapsedTime + " milliseconds","Empty",JOptionPane.WARNING_MESSAGE);
			return;
		}

		String K="Orc_familyWithoutPO_"+filename;
		File file;
		try {
			new MxeConverter().exportMSCA(menuBar.lastDir+File.separator+K,controller);
			file = new File(menuBar.lastDir+File.separator+K);
		} catch (Exception e1) {
			JOptionPane.showMessageDialog(editor.getGraphComponent(),
					"Error in saving the file "+e1.getMessage(),
					"Error",JOptionPane.ERROR_MESSAGE);

			return;			
		}

		String message = "The orchestration has been stored with filename "+menuBar.lastDir+File.separator+K;


		JOptionPane.showMessageDialog(editor.getGraphComponent(),message,"Success!",JOptionPane.WARNING_MESSAGE);
		editor.lastaut=controller;
		menuBar.loadMorphStore(K,editor,file);
		
	}

}
