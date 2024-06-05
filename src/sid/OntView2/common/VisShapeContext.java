package sid.OntView2.common;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.reasoner.NodeSet;

import java.util.ArrayList;

public class VisShapeContext extends ContextMenu {
	MenuItem hideItem, hideProperties, showInstances;
	Shape shape;
	PaintFrame parent;
	OWLClassExpression expression;
	
	double posx,posy;
	public VisShapeContext (Shape s, PaintFrame parentFrame, MouseEvent e){
		
		super();
		shape =s;
		posx =e.getScreenX();
		posy =e.getScreenY();
		parent= parentFrame;
		boolean visc = shape instanceof VisClass;
		expression = shape.asVisClass().getLinkedClassExpression();
       
		hideProperties = new MenuItem("Hide/Show Properties");
		
		
		if ((!visc) || (visc && shape.asVisClass().getPropertyBox() ==null))
			hideProperties.setDisable(true);

		hideProperties.setOnAction(event -> {
			boolean b =shape.asVisClass().getPropertyBox().visible;
			shape.asVisClass().getPropertyBox().setVisible(!b);
		});
		
		hideItem = new MenuItem("Hide");
		
		
		if (!shape.asVisClass().allSubHidden()) {
			hideItem.setDisable(true);
		}
		hideItem.setOnAction(event -> {
			shape.hide();
		});
		
		
		this.getItems().add(getShowInstancesItem());
		this.getItems().add(hideProperties);
		this.getItems().add(hideItem);
		
	}
	
	private MenuItem getShowInstancesItem(){
		if (showInstances == null) {
			showInstances = new MenuItem("show instances");
			showInstances.setOnAction(event -> {
				showInstancesAction();
			});
		}
		return showInstances;
	}
	
	private void showInstancesAction(){
		ArrayList<String> instanceArray = new ArrayList<String>();
		if ((shape instanceof VisClass)){
			NodeSet<OWLNamedIndividual> instanceSet = ((VisClass) shape).getInstances();
			for (org.semanticweb.owlapi.reasoner.Node<OWLNamedIndividual>  instanceNode : instanceSet.getNodes() ){
				for (OWLNamedIndividual instance : instanceNode.getEntities()){
					instanceArray.add(instance.getIRI().getFragment());
				}
				
			}
			VisInstance c = new VisInstance();
			c.setTitle("instances of " + shape.asVisClass().label);
			c.setModel(instanceArray);
			c.showAndWait();

			Stage stage = (Stage) c.getDialogPane().getScene().getWindow();
			stage.setX(posx);
			stage.setY(posy);
		
		}
	}
	
}