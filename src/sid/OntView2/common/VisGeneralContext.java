package sid.OntView2.common;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map.Entry;
import java.util.Set;


public class VisGeneralContext extends JPopupMenu {

	
	JMenuItem item1;
	JMenuItem item2;
	JMenuItem item3;
	private JMenuItem getJMenuItem1(){
		if (item1==null)
			item1 = new JMenuItem("Show/Hide Properties");
		return item1;
	}
	
	private JMenuItem getJMenuItem2(){
		if (item2==null)
			item2 = new JMenuItem("Show/hide Disjoint");
		return item2;
	}
	
	private JMenuItem getJMenuItem3(){
		if (item3==null)
			item3 = new JMenuItem("Show/Hide Ranges");
		return item3;
	}
	PaintFrame parent;
	
	public VisGeneralContext (PaintFrame pparent){
		super();	
		parent = pparent;
		add(getJMenuItem1());
		item1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				propertiesItemClicked();
			}});
		add(getJMenuItem2());
		item2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				disjointItemClicked();
			}
		});
		add(getJMenuItem3());
		item3.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				rangeItemClicked();
			}
		});
	}
	
	
	
	
	private void propertiesItemClicked() {
	    Set<Entry<String, Shape>> classesInGraph = parent.getVisGraph().getClassesInGraph();
	    if (existsVisiblePropertyBox()){
	        for (Entry<String, Shape> entry : classesInGraph) {
		    	if ((entry.getValue() instanceof VisClass) && (entry.getValue().asVisClass().getPropertyBox()!=null)){
		    		VisPropertyBox box = entry.getValue().asVisClass().getPropertyBox();
		    		box.setVisible(false);
		    	}
		    }
	    }
	    else {
	    	for (Entry<String, Shape> entry : classesInGraph) {
		    	if ((entry.getValue() instanceof VisClass) && (entry.getValue().asVisClass().getPropertyBox()!=null)){
		    		VisPropertyBox box = entry.getValue().asVisClass().getPropertyBox();
		    		box.setVisible(true);
		    	}
		    }
	    }
		
	}
	
	private boolean existsVisiblePropertyBox(){
		
	    Set<Entry<String, Shape>> classesInGraph = parent.getVisGraph().getClassesInGraph();
	    for (Entry<String, Shape> entry : classesInGraph) {
	    	if ((entry.getValue() instanceof VisClass) && (entry.getValue().asVisClass().getPropertyBox()!=null)){
	    		if (entry.getValue().asVisClass().getPropertyBox().visible)
	    			return true;
	    	}
	    }
	    return false;
	}
	
	private void disjointItemClicked() {
		parent.getVisGraph().disjoint = ! parent.getVisGraph().disjoint;
		
	}
	

	private void rangeItemClicked() {
		parent.hideRange = !parent.hideRange;
	}
}
