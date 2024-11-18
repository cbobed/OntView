package sid.OntView2.common;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.scene.layout.VBox;

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

	public VisShapeContext(Shape s, PaintFrame parentFrame, MouseEvent e){
		
		super();
		shape = s;
		posx = e.getScreenX();
		posy = e.getScreenY();
		parent = parentFrame;
		boolean visc = shape instanceof VisClass;
		expression = shape.asVisClass().getLinkedClassExpression();
       
		hideProperties = getMenuHideProperties();
		
		if ((!visc) || (visc && shape.asVisClass().getPropertyBox() ==null))
			hideProperties.setDisable(true);

		hideProperties.setOnAction(event -> {
			boolean b =shape.asVisClass().getPropertyBox().visible;
			shape.asVisClass().getPropertyBox().setVisible(!b);
			hideProperties.setText(b ? "Hide Properties" : "Show Properties");
			parent.setStateChanged(true);
			Platform.runLater(parent.relaxerRunnable);
		});
		
		hideItem = new MenuItem("Hide");
	
		if (!shape.asVisClass().allSubHidden()) {
			hideItem.setDisable(true);
		}
		hideItem.setOnAction(event -> shape.hide());
		
		
		this.getItems().add(getShowInstancesItem());
		this.getItems().add(hideProperties);
		this.getItems().add(hideItem);
	}
	
	private MenuItem getShowInstancesItem(){
		if (showInstances == null) {
			showInstances = new MenuItem("Show Instances");
			showInstances.setOnAction(event -> showInstancesAction());
		}
		return showInstances;
	}

	private MenuItem getMenuHideProperties(){
		if (hideProperties==null) {
			hideProperties = new MenuItem();
			if (shape.asVisClass().getPropertyBox() != null) {
				boolean b =shape.asVisClass().getPropertyBox().visible;
				hideProperties.setText(b ? "Hide Properties" : "Show Properties");
			}
			else {
				hideProperties.setText("No Properties"); 
			}
		}
		return hideProperties;
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
			Stage stage = new Stage();
			stage.setTitle("Instances of " + shape.asVisClass().label);

			ObservableList<String> items = FXCollections.observableArrayList(instanceArray);
			ListView<String> listView = new ListView<>(items);

			Text regularText = new Text("Instances of ");
			Text boldBlueText = new Text(shape.asVisClass().label);
			boldBlueText.setFill(Color.BLUE);
			boldBlueText.setFont(Font.font("DejaVu Sans", FontWeight.BOLD, FontPosture.ITALIC, 14));

			TextFlow textFlow = new TextFlow(regularText, boldBlueText);

			VBox vbox = new VBox(textFlow, listView);
			VBox.setMargin(listView, new Insets(10, 0, 0, 0));
			vbox.setPadding(new Insets(10));

			Scene scene = new Scene(vbox, 300, 400);
			stage.setScene(scene);
			stage.setX(posx);
			stage.setY(posy);
			stage.show();
		
		}
	}
	
}