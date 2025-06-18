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
    Slider slider;
    TextField manualValueSlider;
    private Button changeSelectionStrategy;
    private Stage sliderStage = null;
	Shape shape;
	PaintFrame parent;
	OWLClassExpression expression;
	double posx,posy;
    int oldValue;
    boolean ignoreSliderListener = false;

    public Stage getSliderStage(){
        return sliderStage;
    }

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
            ArrayList<String> instanceArray = getInstances();
            if (instanceArray.isEmpty()) {
                showInstances.setDisable(true);
                showInstances.setText("No Instances");
            }
			showInstances.setOnAction(event -> showInstancesAction(instanceArray));
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

    private ArrayList<String> getInstances(){
        ArrayList<String> instanceArray = new ArrayList<>();
        if ((shape instanceof VisClass)) {
            NodeSet<OWLNamedIndividual> instanceSet = ((VisClass) shape).getInstances();
            for (org.semanticweb.owlapi.reasoner.Node<OWLNamedIndividual> instanceNode : instanceSet.getNodes()) {
                for (OWLNamedIndividual instance : instanceNode.getEntities()) {
                    instanceArray.add(instance.getIRI().getFragment());
                }
            }
        }
        return instanceArray;
    }
	
	private void showInstancesAction(ArrayList<String> instanceArray){
		if ((shape instanceof VisClass)){
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
    private HBox sliderHeader() {
        if (titleBar == null) {
            String firstLine = shape.getLabel().split("\n").length > 1 ?
                shape.getLabel().split("\n")[0] + "..." : shape.getLabel();
            Label titleLabel = new Label(" Slider Percentage: " + firstLine);
            titleLabel.getStyleClass().add("title-label");
            titleLabel.setWrapText(false);
            titleLabel.setEllipsisString("...");
            titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

            Button closeButton = new Button("X");
            closeButton.getStyleClass().add("round-button");
            closeButton.setOnAction(event -> {
                sliderStage.close();
                shape.graph.paintframe.selectedShape = null;
                parent.setSliderStage(null);
                Platform.runLater(shape.graph.paintframe.redrawRunnable);
            });

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            titleBar = new HBox(changeSelectionStrategy(), titleLabel, spacer, closeButton);
            titleBar.getStyleClass().add("custom-title-bar");
            titleBar.setAlignment(Pos.CENTER_RIGHT);
            titleBar.setPadding(new Insets(5));
        }
        return titleBar;
    }

    private Slider getSlider() {
        if (slider == null) {
            slider = new Slider(0, 100, getPercentage());
            slider.setShowTickLabels(true);
            slider.setShowTickMarks(true);
            slider.setMajorTickUnit(10);
            slider.setMinorTickCount(5);
            slider.setBlockIncrement(1);
            slider.setSnapToTicks(true);
            slider.getStyleClass().add("custom-slider");

            oldValue = (int) slider.getValue();

            slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (ignoreSliderListener) {
                    ignoreSliderListener = false;
                    return;
                }
                int intValue = newVal.intValue();
                if (intValue != oldValue) {
                    showShapesAccordingToSliderValue(oldValue, intValue);
                    setManualValueSlider().setText(String.valueOf(intValue));
                    oldValue = intValue;
                }
            });
        }

        return slider;
    }

    private TextField setManualValueSlider(){
        if (manualValueSlider == null) {
            manualValueSlider = new TextField(String.valueOf((int) getSlider().getValue()));
            manualValueSlider.setPrefWidth(40);
            manualValueSlider.setMaxWidth(40);
            manualValueSlider.alignmentProperty().set(Pos.CENTER);
            manualValueSlider.getStyleClass().add("text-field-custom");

            manualValueSlider.textProperty().addListener((obs, oldValue, newValue) -> {
                if (!newValue.matches("\\d*")) {
                    manualValueSlider.setText(newValue.replaceAll("[^\\d]", ""));
                } else if (!newValue.isEmpty()) {
                    try {
                        int value = Integer.parseInt(newValue);
                        if (value < 0 || value > 100) {
                            manualValueSlider.setText(oldValue);
                        }
                    } catch (NumberFormatException e) {
                        manualValueSlider.setText(oldValue);
                    }
                }
            });

            manualValueSlider.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused && !manualValueSlider.getText().isEmpty()) {
                    getSlider().setValue(Integer.parseInt(manualValueSlider.getText()));
                }
            });

            manualValueSlider.setOnAction(e -> {
                if (!manualValueSlider.getText().isEmpty()) {
                    getSlider().setValue(Integer.parseInt(manualValueSlider.getText()));
                }
            });
        }
        return manualValueSlider;
    }

    public void updateSliderView(){
        ignoreSliderListener = true;
        if (sliderStage != null && sliderStage.isShowing()) {
            getSlider().setValue(getPercentage());
        }
    }

    private int getPercentage(){
        return (100 * (shape.asVisClass().descendants.size() - shape.getHiddenDescendantsSet())
            / shape.asVisClass().descendants.size());
    }

    private void showSliderAction() {
        if (parent.getSliderStage() != null && parent.getSliderStage().isShowing()) {
            shape.graph.paintframe.selectedShape = null;
            parent.getSliderStage().close();
        }

        sliderStage = new Stage();
        sliderStage.setMinHeight(140);
        sliderStage.setMaxHeight(140);
        sliderStage.setMinWidth(470);
        sliderStage.initStyle(StageStyle.UNDECORATED);
        sliderStage.setAlwaysOnTop(true);

        VBox vbox = new VBox(sliderHeader(), getSlider(), setManualValueSlider());
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.CENTER);
        vbox.getStyleClass().add("custom-vbox-slider");

        handleDragged(vbox, sliderStage); // to be able to move the stage
        shape.graph.paintframe.selectedShape = shape; // mark as selected
        Platform.runLater(shape.graph.paintframe.redrawRunnable);

        vbox.setOnMouseClicked(e -> {
            if (manualValueSlider.isFocused()) {
                vbox.requestFocus();
            }
        });

        ClassLoader c = Thread.currentThread().getContextClassLoader();
        Scene scene = new Scene(vbox, 450, 70);
        scene.getStylesheets().add(Objects.requireNonNull(c.getResource("styles.css")).toExternalForm());
        sliderStage.setScene(scene);

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        sliderStage.setX(screenBounds.getMaxX() - scene.getWidth() - 100);
        sliderStage.setY(300);

        parent.setSliderStage(sliderStage);
        sliderStage.show();
    }

    private void showShapesAccordingToSliderValue(int oldValue, int newValue) {
        if (newValue < oldValue) {
            shape.hideSubLevels(shape.getShapesFromStrategyGlobal(true, newValue));
        } else {
            shape.showSubLevels(shape.getShapesFromStrategyGlobal(false, newValue));
        }

        parent.refreshDashedConnectors();
        VisLevel.adjustWidthAndPos(parent.visGraph.getLevelSet());
        parent.setStateChanged(true);
        Platform.runLater(parent.relaxerRunnable);
    }

    private Button changeSelectionStrategy() {
        if (changeSelectionStrategy == null) {
            changeSelectionStrategy = new Button(OntViewConstants.QUESTION_MARK);
            changeSelectionStrategy.setStyle("-fx-font-size: 20px; -fx-padding: -2 5 0 5;");

            ContextMenu contextMenu = new ContextMenu();
            MenuItem item1 = new MenuItem(VisConstants.GLOBALSTRATEGY_RDFRANK);
            MenuItem item2 = new MenuItem(VisConstants.GLOBALSTRATEGY_RDF_LEVEL_LR);
            MenuItem item3 = new MenuItem(VisConstants.GLOBALSTRATEGY_RDF_LEVEL_RL);
            contextMenu.getStyleClass().add("context-menu-custom");

            item1.setOnAction(e -> {
                parent.setStrategyOptionGlobal(VisConstants.GLOBALSTRATEGY_RDFRANK);
                contextMenu.hide();
            });
            item2.setOnAction(e -> {
                parent.setStrategyOptionGlobal(VisConstants.GLOBALSTRATEGY_RDF_LEVEL_LR);
                contextMenu.hide();
            });
            item3.setOnAction(e -> {
                parent.setStrategyOptionGlobal(VisConstants.GLOBALSTRATEGY_RDF_LEVEL_RL);
                contextMenu.hide();
            });

            contextMenu.getItems().addAll(item1, item2, item3);

            changeSelectionStrategy.setOnAction(e -> {
                if (contextMenu.isShowing()) {
                    contextMenu.hide();
                } else {
                    contextMenu.show(changeSelectionStrategy, changeSelectionStrategy.localToScreen(0, changeSelectionStrategy.getHeight()).getX(),
                        changeSelectionStrategy.localToScreen(0, changeSelectionStrategy.getHeight()).getY());
                }
            });

        }
        return changeSelectionStrategy;
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