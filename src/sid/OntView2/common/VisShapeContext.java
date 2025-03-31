package sid.OntView2.common;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Screen;
import javafx.stage.Stage;

import javafx.stage.StageStyle;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.reasoner.NodeSet;

import java.util.ArrayList;
import java.util.Objects;

public class VisShapeContext extends ContextMenu {
	MenuItem hideItem, hideProperties, showInstances, showSliderPercentage;
    HBox titleBar;
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
		boolean visC = shape instanceof VisClass;
		expression = shape.asVisClass().getLinkedClassExpression();
       
		hideProperties = getMenuHideProperties();
		
		if (!visC || shape.asVisClass().getPropertyBox() == null)
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
		hideItem.setOnAction(event -> {
			shape.hide();
			shape.updateParents();
			Platform.runLater(parent.redrawRunnable);
		});

		this.getItems().add(getShowInstancesItem());
        this.getItems().add(getSliderPercentage());
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

    private MenuItem getSliderPercentage(){
        if (showSliderPercentage == null) {
            showSliderPercentage = new MenuItem("Show Percentage Visibility");
            if (shape.asVisClass().descendants.isEmpty()) {
                showSliderPercentage.setDisable(true);
            }
            showSliderPercentage.setOnAction(event -> showSliderAction());
        }
        return showSliderPercentage;
    }


	private MenuItem getMenuHideProperties(){
		if (hideProperties==null) {
			hideProperties = new MenuItem();
			if (shape.asVisClass().getPropertyBox() != null) {
				boolean b = shape.asVisClass().getPropertyBox().visible;
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

    /*
     * SLIDER PERCENTAGE METHODS
     */
    private HBox sliderHeader(Stage sliderStage) {
        if (titleBar == null) {
            Label titleLabel = new Label("Slider Percentage");
            titleLabel.getStyleClass().add("title-label");
            Button closeButton = new Button("X");
            closeButton.getStyleClass().add("round-button");
            closeButton.setOnAction(event -> sliderStage.close());

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            titleBar = new HBox(titleLabel, spacer, closeButton);
            titleBar.getStyleClass().add("custom-title-bar");
            titleBar.setAlignment(Pos.CENTER_RIGHT);
            titleBar.setPadding(new Insets(5));
        }
        return titleBar;
    }

    private Slider getSlider(){
        Slider slider = new Slider(0,100, getPercentage());
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(10);
        slider.setMinorTickCount(5);
        slider.setBlockIncrement(1);
        slider.setSnapToTicks(true);
        slider.getStyleClass().add("custom-slider");

        slider.setOnMouseReleased(event -> {
            changeValue((int) slider.getValue());
        });
        return slider;
    }

    private int getPercentage(){
        System.out.println(shape.getLabel() + " percentage: " + (100*(shape.asVisClass().descendants.size() - shape.asVisClass().getHiddenDescendantsSet())
                / shape.asVisClass().descendants.size()));
        return (100 * (shape.asVisClass().descendants.size() - shape.asVisClass().getHiddenDescendantsSet()))
            / shape.asVisClass().descendants.size();
    }

    private void showSliderAction() {
        Stage sliderStage = new Stage();
        sliderStage.setTitle("Slider Percentage");
        sliderStage.setMinHeight(110);
        sliderStage.setMaxHeight(110);
        sliderStage.setMinWidth(470);
        sliderStage.initStyle(StageStyle.UNDECORATED);
        sliderStage.setAlwaysOnTop(true);

        VBox vbox = new VBox(sliderHeader(sliderStage), getSlider());
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.CENTER);
        vbox.getStyleClass().add("custom-vbox-slider");
        handleDragged(vbox, sliderStage); // to be able to move the stage

        ClassLoader c = Thread.currentThread().getContextClassLoader();
        Scene scene = new Scene(vbox, 450, 70);
        scene.getStylesheets().add(Objects.requireNonNull(c.getResource("styles.css")).toExternalForm());
        sliderStage.setScene(scene);

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        sliderStage.setX(screenBounds.getMaxX() - scene.getWidth() - 100);
        sliderStage.setY(300);

        sliderStage.show();
    }

    private void changeValue(double valor) {
        System.out.println("Valor del slider: " + valor);
    }

    /**
     * Method to handle the dragging of the slider stage.
     * Since the stage is set to UNDECORATED, we need to add a listener for dragging.
     */
    private void handleDragged(VBox vbox, Stage sliderStage) {
        final Delta dragDelta = new Delta(); // helper class to store the offset during dragging
        vbox.setOnMousePressed(event -> {
            dragDelta.x = sliderStage.getX() - event.getScreenX();
            dragDelta.y = sliderStage.getY() - event.getScreenY();
        });
        vbox.setOnMouseDragged(event -> {
            sliderStage.setX(event.getScreenX() + dragDelta.x);
            sliderStage.setY(event.getScreenY() + dragDelta.y);
        });
    }

    private static class Delta {
        double x, y;
    }
	
}