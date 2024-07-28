package sid.OntView2.common;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import org.apache.jena.base.Sys;

import java.util.Map.Entry;
import java.util.Set;


public class VisGeneralContext extends ContextMenu {


	MenuItem item1;
	MenuItem item2;
	MenuItem item3;
	private MenuItem getMenuItem1(){
		if (item1==null)
			item1 = new MenuItem();
		return item1;
	}

	private MenuItem getMenuItem2(){
		if (item2==null)
			item2 = new MenuItem();
		return item2;
	}

	private MenuItem getMenuItem3(){
		if (item3==null)
			item3 = new MenuItem();
		return item3;
	}
	PaintFrame parent;

	public VisGeneralContext (PaintFrame pparent){
		super();
		parent = pparent;
		getItems().add(getMenuItem1());
		getMenuItem1().setOnAction(e -> propertiesItemClicked());

		getItems().add(getMenuItem2());
		getMenuItem2().setOnAction(e -> disjointItemClicked());

		getItems().add(getMenuItem3());
		getMenuItem3().setOnAction(actionEvent -> rangeItemClicked());

		updateMenuItemTexts();
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
		parent.setStateChanged(true);
		parent.relax();
		updateMenuItemTexts();

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
		updateMenuItemTexts();
	}


	private void rangeItemClicked() {
		parent.hideRange = !parent.hideRange;
		updateMenuItemTexts();
	}

	private void updateMenuItemTexts() {
		if (existsVisiblePropertyBox()) {
			getMenuItem1().setText("Hide Properties");
		} else {
			getMenuItem1().setText("Show Properties");
		}

		if (!parent.getVisGraph().disjoint) {
			getMenuItem2().setText("Hide Disjoint");
		} else {
			getMenuItem2().setText("Show Disjoint");
		}

		if (!parent.hideRange) {
			getMenuItem3().setText("Show Ranges");
		} else {
			getMenuItem3().setText("Hide Ranges");
		}
	}
}
