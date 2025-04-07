package sid.OntView2.main;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.semanticweb.owlapi.model.IRI;
import sid.OntView2.common.*;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

//VS4E -- DO NOT REMOVE THIS LINE!
public class TopPanel extends Canvas implements ControlPanelInterface {

	//private static final long serialVersionUID = 1L;
	private Button loadOntologyButton, loadReasonerButton, saveViewButton, restoreViewButton,
        saveImageButton, saveImagePartialButton, cleanConnectorsButton, fileSystemButton, helpButton,
        helpButtonCE, submitButtonCE, expressionButton, changeSelectionStrategy, moreOptions;
	private ComboBox<Object> loadOntologyCombo;
	private ComboBox<String> loadReasonerCombo, comboBox, kceComboBox;
	private VBox mainPane, panelLoad, panelCheckBox, viewPanel, panel0, connectorPanel, helpPanel,
        classExpressionPanel, percentagePanel, parentListBox, childListBox;
	private Label label0;
	private ToggleButton toggleSwitch;
	private CheckBox Properties, reduceCheckBox;
	private Slider zoomSlider;
    private Spinner<Integer> percentageSpinner;
	private Popup helpPopup;
	private TextField parentField, childField, parentSearchField, childSearchField;
	private ListView<CheckBox> parentCheckBoxList , childCheckBoxList;
	private VisClass selectedParent = null, selectedChild = null;
    private final Mine parent;


	public TopPanel(Mine pParent) {
		parent = pParent;
		initComponents();
	}

	public Node getMainPane() {
		return mainPane;
	}

	private void initComponents() {
		mainPane = new VBox(10);
		mainPane.setPadding(new Insets(10));
		mainPane.setStyle("-fx-border-color: #404472; -fx-background-color: #f9f9f9; -fx-border-width: 1; -fx-border-style: solid;");
		mainPane.setStyle("-fx-border-style: solid;");

		mainPane.getChildren().addAll(createLoadOntologyRow(), createOtherComponentsRow());
		mainPane.setAlignment(Pos.CENTER);
		mainPane.setSpacing(10);

		setWidth(1300);
		setHeight(100);
	}

	private VBox createLoadOntologyRow() {
		StackPane titlePane = createTitlePane("Load Ontology");

		HBox row = createRow(getOntologyCombo(), getfileSystemButton(), getLoadOntologyButton());
		row.setAlignment(Pos.CENTER);

		return createContainer(titlePane, row);

	}

	private HBox createOtherComponentsRow() {
		HBox row = new HBox(10,
				createLoadOntologyOptions(),
				createShowClassExpressionButton(),
                createVisibilityPercentageSelector(),
				getPanelCheckBox(),
				getZoomSlider(),
				getViewPanel(),
				getSnapshotPanel(),
				getPanel0(),
				getConnectorsActions(),
				createHelpButton());
		row.setAlignment(Pos.CENTER);

		return row;
	}

	private VBox getSnapshotPanel() {
		if (snapshotPanel == null) {
			snapshotPanel = new VBox();
			snapshotPanel.setPadding(new Insets(5));

			StackPane titlePane = createTitlePane("Snapshot");

			HBox row = createRow(getSaveImageButton(), getSaveImagePartialButton());
			row.setAlignment(Pos.CENTER);

			snapshotPanel = createContainer(titlePane, row);
		}
		return snapshotPanel;
	}

	private Button getSaveImagePartialButton() {
		if (saveImagePartialButton == null) {
			saveImagePartialButton = new Button();
			saveImagePartialButton.getStyleClass().add("button");
			saveImagePartialButton.setCursor(Cursor.HAND);
			saveImagePartialButton.setMinWidth(40);

			ClassLoader c = Thread.currentThread().getContextClassLoader();
			Image image = new Image(Objects.requireNonNull(c.getResourceAsStream("saveImagePartial.JPG")));
			ImageView imageView = new ImageView(image);
			imageView.setFitWidth(20);
			imageView.setFitHeight(20);
			imageView.setPreserveRatio(true);
			saveImagePartialButton.setGraphic(imageView);

			saveImagePartialButton.setOnAction(this::saveImageButtonPartialActionActionPerformed);

			tooltipInfo(saveImagePartialButton, "Save visible graph as image");
		}
		return saveImagePartialButton;
	}

	private CheckBox getQualifiedNames() {
		if (qualifiedNames == null) {
			qualifiedNames = new CheckBox("qualified names");
			qualifiedNames.setCursor(Cursor.HAND);
			// TODO Auto-generated method stub
			qualifiedNames.setOnAction(this::qualifiedNamesActionActionPerformed);
		}
		return qualifiedNames;
	}

	public void qualifiedNamesActionActionPerformed(ActionEvent event) {
		if (parent.artPanel != null) {
			parent.artPanel.qualifiedNames = getQualifiedNames().isSelected();
			parent.artPanel.getVisGraph().changeRenderMethod(parent.artPanel.renderLabel, parent.artPanel.qualifiedNames);
			Platform.runLater(parent.artPanel.getRedrawRunnable());
		}
	}

	private CheckBox getRenderLabel() {
		if (renderLabel == null) {
			renderLabel = new CheckBox("label");
			renderLabel.setCursor(Cursor.HAND);
			renderLabel.setOnAction(this::renderLabelActionActionPerformed);
		}
		return renderLabel;
	}

	public void renderLabelActionActionPerformed(ActionEvent event) {
		if (parent.artPanel != null) {
			parent.artPanel.renderLabel = getRenderLabel().isSelected();
			parent.artPanel.getVisGraph().changeRenderMethod(parent.artPanel.renderLabel, parent.artPanel.qualifiedNames);
			Platform.runLater(parent.artPanel.getRedrawRunnable());
		}
	}

    private Button moreOptions() {
        if (moreOptions == null) {
            moreOptions = new Button("\u22EE");
            moreOptions.setStyle("-fx-font-size: 20px; -fx-padding: -2 5 0 5;");

            ContextMenu contextMenu = new ContextMenu();
            MenuItem item1 = createMenuItemWithTooltip(VisConstants.COMPACT_GRAPH,
                "Compacts the graph to reduce visual clutter and enhance clarity");
            contextMenu.getStyleClass().add("context-menu-custom");

            item1.setOnAction(e -> {
                parent.artPanel.compactGraph();
                contextMenu.hide();
            });

            contextMenu.getItems().addAll(item1);

            moreOptions.setOnAction(e -> {
                if (contextMenu.isShowing()) {
                    contextMenu.hide();
                } else {
                    contextMenu.show(moreOptions, moreOptions.localToScreen(0, moreOptions.getHeight()).getX(),
                        moreOptions.localToScreen(0, moreOptions.getHeight()).getY());
                }
            });

            tooltipInfo(moreOptions, "Additional options for ontology processing");
        }
        return moreOptions;
    }

	private Slider getZoomSlider() {
		if (zoomSlider == null) {
			zoomSlider = new Slider(0.5, 2.0, 1.0);
			zoomSlider.setOrientation(Orientation.VERTICAL);
            zoomSlider.setShowTickLabels(true);
            zoomSlider.setMinWidth(30);

			zoomSlider.getStyleClass().add("zoom-slider");
			zoomSlider.setPrefHeight(VisConstants.CONTAINER_SIZE);
			zoomSlider.setMinHeight(VisConstants.CONTAINER_SIZE);
			zoomSlider.setMaxHeight(VisConstants.CONTAINER_SIZE);

            zoomSlider.valueProperty().addListener((obs, oldVal, newVal) ->
                zoomSliderChangeStateChanged(newVal.doubleValue())
            );
		}
		return zoomSlider;
	}

	private void tooltipInfo(Node node, String text) {
		Tooltip tooltip = new Tooltip(text);
		Tooltip.install(node, tooltip);
		tooltip.setFont(new Font("DejaVu Sans", 12));
	}

	private Button getfileSystemButton() {
		if (fileSystemButton == null) {
			fileSystemButton = new Button();
			fileSystemButton.getStyleClass().add("button");
			fileSystemButton.setCursor(Cursor.HAND);

			ClassLoader c = Thread.currentThread().getContextClassLoader();
			Image icon = new Image(Objects.requireNonNull(c.getResourceAsStream("folder.png")));
			ImageView imageView = new ImageView(icon);
			imageView.setFitWidth(17);
			imageView.setFitHeight(17);
			imageView.setPreserveRatio(true);
			fileSystemButton.setGraphic(imageView);
			fileSystemButton.setOnAction(this::fileSystemButtonActionActionPerformed);

			tooltipInfo(fileSystemButton, "Select ontology file from file system");
		}
		return fileSystemButton;
	}

	private CheckBox getPropertiesCheckBox() {
		if (Properties == null) {
			Properties = new CheckBox("properties");
			Properties.setCursor(Cursor.HAND);
			Properties.setOnAction(this::propertiesActionActionPerformed);
		}
		return Properties;
	}

	private VBox getPanel0() {
		if (panel0 == null) {
			panel0 = new VBox();
			panel0.setPadding(new Insets(5));
			panel0.setSpacing(5);


			StackPane titlePane = createTitlePane("Search");
			HBox row = createRow(getLabel0(), getComboBox0());

			panel0 = createContainer(titlePane, row);

		}
		return panel0;
	}

	private VBox getConnectorsActions() {
		if (connectorPanel == null) {
			connectorPanel = new VBox();
			connectorPanel.setPadding(new Insets(5));
			connectorPanel.setSpacing(5);

			StackPane titlePane = createTitlePane("Connectors");
			HBox row = createRow(getConnectorsSwitch(), createCleanConnectorsButton());
			row.setAlignment(Pos.CENTER);

			connectorPanel = createContainer(titlePane, row);

		}

		return connectorPanel;
	}

	private ToggleButton getConnectorsSwitch() {
		if (toggleSwitch == null) {
			toggleSwitch = new ToggleButton("Show");
			toggleSwitch.getStyleClass().add("button");
			toggleSwitch.setMinWidth(60);

			// Add a listener to handle the switch state
			toggleSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
				if (newValue) {
					toggleSwitch.setText("Hide");
					parent.artPanel.setShowConnectors(true);
					Platform.runLater(parent.artPanel.getRedrawRunnable());
				} else {
					toggleSwitch.setText("Show");
					parent.artPanel.setShowConnectors(false);
					Platform.runLater(parent.artPanel.getRedrawRunnable());

				}
			});

		}
		return toggleSwitch;
	}

	private Button createCleanConnectorsButton() {
		if (cleanConnectorsButton == null) {
			cleanConnectorsButton = new Button("Clean");
			cleanConnectorsButton.getStyleClass().add("button");
			cleanConnectorsButton.setMinWidth(60);
			cleanConnectorsButton.setOnAction(this::cleanConnectorActionPerformed);
		}

		return cleanConnectorsButton;
	}

	private ComboBox<String> getComboBox0() {
		if (comboBox == null) {
			comboBox = new ComboBox<>();
			AutoCompletion.enable(comboBox);
			comboBox.setEditable(true);
			comboBox.getStyleClass().add("custom-combo-box");
            comboBox.setMaxWidth(100);

			ObservableList<String> items = FXCollections.observableArrayList();
			comboBox.setItems(items);
			HBox.setHgrow(comboBox, Priority.ALWAYS);
			comboBox.setMaxWidth(Double.MAX_VALUE);
			comboBox.valueProperty().addListener((options, oldValue, newValue) -> comboBox0ItemItemStateChanged(newValue));

			TextField editor = comboBox.getEditor();
			editor.addEventHandler(KeyEvent.KEY_RELEASED, event -> handleAutoComplete(editor.getText(), items));

			tooltipInfo(comboBox, "Search for a class once the ontology is loaded");
		}
		return comboBox;
	}

	public ComboBox<String> getKceComboBox() {
		if (kceComboBox == null) {
			kceComboBox = new ComboBox<>();
			AutoCompletion.enable(kceComboBox);
			kceComboBox.setEditable(true);
			kceComboBox.setPromptText("Select KCE");
			kceComboBox.getStyleClass().add("custom-combo-box");

			ObservableList<String> items = FXCollections.observableArrayList(
					VisConstants.NONECOMBOOPTION,
					VisConstants.KCECOMBOOPTION1,
					VisConstants.KCECOMBOOPTION2,
					VisConstants.PAGERANKCOMBOOPTION1,
					VisConstants.PAGERANKCOMBOOPTION2,
					VisConstants.RDFRANKCOMBOOPTION1,
					VisConstants.RDFRANKCOMBOOPTION2,
					VisConstants.CUSTOMCOMBOOPTION3);
			kceComboBox.setItems(items);

			HBox.setHgrow(kceComboBox, Priority.ALWAYS);
			kceComboBox.setMaxWidth(Double.MAX_VALUE);
			kceComboBox.setPrefWidth(130);
            kceComboBox.setDisable(true);

			if (!items.isEmpty()) {
				kceComboBox.setValue(items.get(0));
			}

			if (parent.artPanel != null) {
				parent.artPanel.setKceOption(kceComboBox.getItems().get(0));
			}
			kceComboBox.setOnAction(this::kceItemItemStateChanged);
		}
		return kceComboBox;
	}

	protected void kceItemItemStateChanged(ActionEvent event) {
		if (parent.artPanel != null) {
			parent.artPanel.setKceOption(kceComboBox.getSelectionModel().getSelectedItem());
			parent.artPanel.doKceOptionAction();
		}
	}

	private Label getLabel0() {
		if (label0 == null) {
			label0 = new Label();
			ClassLoader c = Thread.currentThread().getContextClassLoader();
			Image image = new Image(Objects.requireNonNull(c.getResourceAsStream("search.JPG")));
			ImageView imageView = new ImageView(image);
			imageView.setFitWidth(18);
			imageView.setFitHeight(18);
			label0.setGraphic(imageView);
		}
		return label0;
	}

	public Button getLoadReasonerButton() {
		if (loadReasonerButton == null) {
			loadReasonerButton = new Button("Sync");
			loadReasonerButton.setMinWidth(60);
			loadReasonerButton.setCursor(Cursor.HAND);
			loadReasonerButton.setFont(Font.font("DejaVu Sans", FontWeight.NORMAL, 10));
			loadReasonerButton.getStyleClass().add("button");
			loadReasonerButton.setOnAction(this::loadReasonerButtonActionTask);
		}
		return loadReasonerButton;
	}

	private VBox createLoadOntologyOptions() {
		if (panelLoad == null) {
			HBox row1 = createRow(getReasonerCombo(), getKceComboBox(), getLoadReasonerButton());

			StackPane titlePane = createTitlePane("Reasoner & KConcept Extraction");

			panelLoad = createContainer(titlePane, row1);
			panelLoad.setMinWidth(250);
		}
		return panelLoad;
	}

	private HBox createRow(Node... children) {
		HBox row = new HBox(5);
		row.setAlignment(Pos.CENTER);
		row.getChildren().addAll(children);
		return row;
	}

	private VBox createContainer(Node... children) {
		VBox container = new VBox(7);
		container.getChildren().addAll(children);
		container.setPadding(new Insets(10, 10, 10, 10));
        container.getStyleClass().add("custom-container");
        container.setPrefHeight(VisConstants.CONTAINER_SIZE);
		container.setMinHeight(VisConstants.CONTAINER_SIZE);
		container.setMaxHeight(VisConstants.CONTAINER_SIZE);
		container.setAlignment(Pos.CENTER);
		return container;
	}

	private VBox createContainerNoSize(Node... children) {
		VBox container = new VBox(7);
		container.getChildren().addAll(children);
		container.setPadding(new Insets(10, 10, 10, 10));
		container.getStyleClass().add("custom-container");
		container.setAlignment(Pos.CENTER);
		return container;
	}

	private StackPane createTitlePane(String title) {
		Text titledPane = new Text(title);
		titledPane.setFill(Color.BLUE);
		titledPane.setFont(Font.font("DejaVu Sans", FontWeight.BOLD, FontPosture.ITALIC, 12));

		StackPane titlePane = new StackPane();
		titlePane.getChildren().add(titledPane);

		return titlePane;
	}

	private CheckBox getReduceCheckBox() {
		if (reduceCheckBox == null) {
			reduceCheckBox = new CheckBox();
			reduceCheckBox.setFont(Font.font("DejaVu Sans", FontWeight.NORMAL, 10));
			reduceCheckBox.setOnAction(this::reduceActionActionPerformed);
		}
		return reduceCheckBox;
	}

	private Button getSaveImageButton() {
		if (saveImageButton == null) {
			saveImageButton = new Button();
			saveImageButton.getStyleClass().add("button");
			saveImageButton.setCursor(Cursor.HAND);
			saveImageButton.setMinWidth(40);

			ClassLoader c = Thread.currentThread().getContextClassLoader();
			Image image = new Image(Objects.requireNonNull(c.getResourceAsStream("saveImage.JPG")));
			ImageView imageView = new ImageView(image);
			imageView.setFitWidth(20);
			imageView.setFitHeight(20);
			imageView.setPreserveRatio(true);
			saveImageButton.setGraphic(imageView);
			saveImageButton.setOnAction(this::saveImageButtonActionActionPerformed);

			tooltipInfo(saveImageButton, "Save graph as image");

		}
		return saveImageButton;
	}

	private VBox getViewPanel() {
		if (viewPanel == null) {
			viewPanel = new VBox();
			viewPanel.setPadding(new Insets(5));

			StackPane titlePane = createTitlePane("View");
			HBox row = createRow(getSaveViewButton(), getRestoreViewButton());
			row.setAlignment(Pos.CENTER);

			viewPanel = createContainer(titlePane, row);
		}
		return viewPanel;
	}

	private Button getRestoreViewButton() {
		if (restoreViewButton == null) {
			restoreViewButton = new Button("Restore");
			restoreViewButton.getStyleClass().add("button");
			restoreViewButton.setMinWidth(60);
			restoreViewButton.setCursor(Cursor.HAND);

			restoreViewButton.setOnAction(this::restoreViewButtonActionActionPerformed);

			tooltipInfo(restoreViewButton, "Restore view from xml file");
		}
		return restoreViewButton;
	}

	private Button getSaveViewButton() {
		if (saveViewButton == null) {
			saveViewButton = new Button("Save");
			saveViewButton.getStyleClass().add("button");
			saveViewButton.setMinWidth(60);
			saveViewButton.setCursor(Cursor.HAND);

			saveViewButton.setOnAction(this::saveViewButtonActionActionPerformed);

			tooltipInfo(saveViewButton, "Save current view as xml file");
		}
		return saveViewButton;
	}

	private VBox getPanelCheckBox() {
		if (panelCheckBox == null) {
			panelCheckBox = new VBox();

			StackPane titlePane = createTitlePane("Options");
			HBox row = createRow(getPropertiesCheckBox(), getRenderLabel(), getQualifiedNames(), moreOptions());
			row.setSpacing(10);
			row.setAlignment(Pos.CENTER);

			panelCheckBox = createContainer(titlePane, row);
			panelCheckBox.setMinWidth(325);
		}
		return panelCheckBox;
	}

	private Button getLoadOntologyButton() {
		if (loadOntologyButton == null) {
			loadOntologyButton = new Button("Load Ont");
			loadOntologyButton.setCursor(Cursor.HAND);
			loadOntologyButton.setFont(Font.font("DejaVu Sans", FontWeight.NORMAL, 10));
			loadOntologyButton.getStyleClass().add("button");
			loadOntologyButton.setMinWidth(100);
			loadOntologyButton.setOnAction(this::OntologyButtonActionActionPerformed);
		}
		return loadOntologyButton;
	}

	public ComboBox<String> getReasonerCombo() {
		if (loadReasonerCombo == null) {
			loadReasonerCombo = new ComboBox<>();
			loadReasonerCombo.setEditable(true);
			loadReasonerCombo.setPromptText("Select reasoner");
			ObservableList <String> items = FXCollections.observableArrayList("Openllet", "HermiT");
			loadReasonerCombo.setItems(items);

			if (!items.isEmpty()) {
				loadReasonerCombo.setValue(items.get(0));
			}

			loadReasonerCombo.getStyleClass().add("custom-combo-box");
			loadReasonerCombo.setMaxWidth(Double.MAX_VALUE);
			loadReasonerCombo.setPrefWidth(100);
			loadReasonerCombo.setDisable(true);
		}
		return loadReasonerCombo;
	}

	public ComboBox<Object> getOntologyCombo() {
		if (loadOntologyCombo == null) {
			loadOntologyCombo = new ComboBox<>();
			loadOntologyCombo.setEditable(true);
			loadOntologyCombo.setPromptText("Enter URL or select from file system");
			loadOntologyCombo.getStyleClass().add("custom-combo-box");
			loadRecent();
			loadOntologyCombo.setMaxWidth(Double.MAX_VALUE);
			HBox.setHgrow(loadOntologyCombo, Priority.ALWAYS);
			loadOntologyCombo.setDisable(false);
		}
		return loadOntologyCombo;
	}

	private VBox createShowClassExpressionButton() {
		if (classExpressionPanel == null) {
			classExpressionPanel = new VBox();
			classExpressionPanel.setPadding(new Insets(5));
			classExpressionPanel.setSpacing(5);


			StackPane titlePane = createTitlePane("Class Expression");
			expressionButton = new Button("Select");
			expressionButton.getStyleClass().add("button");
			expressionButton.setOnAction(this::selectNodesClassExpressionActionPerformed);

			classExpressionPanel = createContainer(titlePane, expressionButton);
		}
		return classExpressionPanel;
	}

    private VBox createVisibilityPercentageSelector() {
        if (percentagePanel == null) {
            percentagePanel = new VBox();
            percentagePanel.setPadding(new Insets(5));
            percentagePanel.setSpacing(5);

            StackPane titlePane = createTitlePane("Visibility Step");
            HBox row = createRow(createTitlePane("%"), getVisibilitySpinner(), changeSelectionStrategy());

            percentagePanel = createContainer(titlePane, row);
        }
        return percentagePanel;
    }

    private Button changeSelectionStrategy() {
        if (changeSelectionStrategy == null) {
            changeSelectionStrategy = new Button("\u22EE");
            changeSelectionStrategy.setStyle("-fx-font-size: 20px; -fx-padding: -2 5 0 5;");

            ContextMenu contextMenu = new ContextMenu();
            MenuItem item1 = createMenuItemWithTooltip(VisConstants.STEPSTRATEGY_RDFRANK, "Apply PageRank strategy to all descendants of the node");
            MenuItem item2 = createMenuItemWithTooltip(VisConstants.STEPSTRATEGY_RDF_LEVEL_LR, "Apply PageRank strategy by progressively hiding nodes level by level from left to right");
            MenuItem item3 = createMenuItemWithTooltip(VisConstants.STEPSTRATEGY_RDF_LEVEL_RL, "Apply PageRank strategy by progressively hiding nodes level by level from right to left");
            contextMenu.getStyleClass().add("context-menu-custom");

            item1.setOnAction(e -> {
                parent.artPanel.setStrategyOptionStep(VisConstants.STEPSTRATEGY_RDFRANK);
                contextMenu.hide();
            });
            item2.setOnAction(e -> {
                parent.artPanel.setStrategyOptionStep(VisConstants.STEPSTRATEGY_RDF_LEVEL_LR);
                contextMenu.hide();
            });
            item3.setOnAction(e -> {
                parent.artPanel.setStrategyOptionStep(VisConstants.STEPSTRATEGY_RDF_LEVEL_RL);
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

            tooltipInfo(changeSelectionStrategy, "Select strategy to determine how nodes are displayed or hidden");
        }
        return changeSelectionStrategy;
    }

    private MenuItem createMenuItemWithTooltip(String text, String tooltipText) {
        Label label = new Label(text);
        Tooltip tooltip = new Tooltip(tooltipText);
        Tooltip.install(label, tooltip);
        tooltip.setFont(new Font("DejaVu Sans", 12));

        return new CustomMenuItem(label, false);
    }

    public void changeLimitValue(int newLimit) {
        if (percentageSpinner != null) {
            getVisibilitySpinner().getValueFactory().setValue(newLimit);
            parent.artPanel.setPercentageShown(newLimit);
        }
    }

    private Spinner<Integer> getVisibilitySpinner() {
        if (percentageSpinner == null) {
            SpinnerValueFactory<Integer> values =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0, 1);
            percentageSpinner = new Spinner<>(values);
            percentageSpinner.setEditable(true);
            percentageSpinner.getStyleClass().add("spinner");
            percentageSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
                savePercentageValue(newValue);
            });

            // Only numbers allow [0-100]
            TextField editor = percentageSpinner.getEditor();
            editor.textProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue.isEmpty()) {
                    return;
                }
                if (!newValue.matches("\\d*")) {
                    editor.setText(oldValue);
                } else {
                    try {
                        int value = Integer.parseInt(newValue);
                        if (value < 0 || value > 100) {
                            editor.setText(oldValue);
                        }
                    } catch (NumberFormatException e) {
                        editor.setText(oldValue);
                    }
                }
            });
            editor.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused && editor.getText().isEmpty()) {
                    editor.setText("0");
                    percentageSpinner.getValueFactory().setValue(0);
                }
            });

            percentageSpinner.setPrefWidth(60);
        }
        return percentageSpinner;
    }

	private VBox createHelpButton() {
		if (helpPanel == null) {
			helpPanel = new VBox();
			helpPanel.setPadding(new Insets(5));
			helpPanel.setSpacing(5);


			StackPane titlePane = createTitlePane("Help");
			helpButton = new Button("?");
			helpButton.getStyleClass().add("button");
			helpButton.setStyle("-fx-shape:'M 0 50 a 50 50 0 1 1 100 0 a 50 50 0 1 1 -100 0'; -fx-background-radius: 50%;");

			TabPane tabPane = createHelpTabPane();

			helpPopup = new Popup();
			helpPopup.getContent().add(tabPane);
			helpPopup.setAutoHide(true);

			helpButton.setOnMouseClicked(event -> {
				if (helpPopup.isShowing()) {
					helpPopup.hide();
				} else {
					helpPopup.show(helpButton, event.getScreenX(), event.getScreenY() + 15);
				}
			});

			helpPanel = createContainer(titlePane, helpButton);
		}
		return helpPanel;
	}

	private TabPane createHelpTabPane() {
		TabPane tabPane = new TabPane();

		Tab helpTab = new Tab("Help");
		helpTab.setClosable(false);
		TextFlow helpContent = createHelpContent();
		helpTab.setContent(helpContent);

		Tab elementsInfoTab = new Tab("Legend");
		elementsInfoTab.setClosable(false);
		TextFlow elementsInfoContent = createElementsInfoContent();
		elementsInfoTab.setContent(elementsInfoContent);

		tabPane.getTabs().addAll(helpTab, elementsInfoTab);

		return tabPane;
	}

	private TextFlow createHelpContent() {
		Text title = new Text("How to use the application:\n");
		title.setFill(Color.BLUE);
		title.setStyle("-fx-font-weight: bold;");

		Text step1 = new Text("1. Load an ontology and click the 'Load Ont' button.\n");
		Text step2 = new Text("2. Select a reasoner, then choose the 'KConceptExtractor', and finally click the 'Sync' button.\n");
		Text step3 = new Text("3. You are now ready to use the rest of the functionalities.");

		TextFlow helpContent = new TextFlow(title, step1, step2, step3);
		helpContent.setPadding(new Insets(10));

		helpContent.setLineSpacing(5);
		return helpContent;
	}
	private TextFlow createElementsInfoContent() {
		Text elementsInfoTitle = new Text("Legend:\n");
		elementsInfoTitle.setFill(Color.BLUE);
		elementsInfoTitle.setStyle("-fx-font-weight: bold;");

		Text elementsInfoText = new Text(
                """
				P: Indicates that this specific node has properties associated with it.
				D: Indicates that the class is disjoint with other classes.
				Named Classes: Gray
				Defined Classes: Light Green
				Anonymous Classes: White
				"""
		);

		TextFlow elementsInfoContent = new TextFlow(elementsInfoTitle, elementsInfoText);
		elementsInfoContent.setPadding(new Insets(10));

		elementsInfoContent.setLineSpacing(5);

		return elementsInfoContent;
	}


	private void loadRecent() {
		try {
			ClassLoader c = Thread.currentThread().getContextClassLoader();
			BufferedReader in;
			if (new File("recent.txt").exists()) {
				in = new BufferedReader(new FileReader("recent.txt"));
			} else {
				in = new BufferedReader(new InputStreamReader(Objects.requireNonNull(c.getResourceAsStream("recent.txt"))));
			}
			String line = "";
			int no = 0;
			ObservableList<Object> items = FXCollections.observableArrayList();
			while ((line != null) && (no < 15)) {
				line = in.readLine();
				if ((line != null) && (!line.isEmpty())) {
					items.add(line);
					no++;
				}
			}
			in.close();
			loadOntologyCombo.setItems(items);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void OntologyButtonActionActionPerformed(ActionEvent event) {
		parent.artPanel.stop();
		String x = (String) getOntologyCombo().getValue();
		if ((x != null) && (!x.isEmpty())) {
			parent.loadActiveOntologyTask(IRI.create(x));
		} else {
			parent.artPanel.showAlertDialog("Error", "No ontology selected.",
					"Please select an ontology first.", Alert.AlertType.ERROR);
		}
	}

    void savePercentageValue(Integer value) {
        parent.artPanel.setPercentageShown(value);
    }

	private void saveImageButtonActionActionPerformed(ActionEvent event) {
		parent.createImage();
	}

	private void saveImageButtonPartialActionActionPerformed(ActionEvent event) {
		parent.createImageFromVisibleRect();
	}

	private void applyCheckBoxFunctions(ActionEvent e) {
		if (getPropertiesCheckBox().isSelected()) {
			propertiesActionActionPerformed(e);
		}

		if (getRenderLabel().isSelected()) {
			renderLabelActionActionPerformed(e);
		}

		if (getQualifiedNames().isSelected()) {
			qualifiedNamesActionActionPerformed(e);
		}
	}

    private void resetParameters(){
        restoreSliderValue();
        resetConnectorsSwitch();
        if(parent.artPanel.menuVisShapeContext != null && parent.artPanel.menuVisShapeContext.getSliderStage() != null) { // close slider
            parent.artPanel.menuVisShapeContext.getSliderStage().close();
        }
        parent.artPanel.cleanConnectors();
        parent.artPanel.clearCanvas();
        parent.artPanel.selectedShape = null;
    }

	void loadReasonerButtonActionTask(ActionEvent event) {
		// Only use Custom mode once the graph is loaded
        resetParameters();
		if (Objects.equals(getKceComboBox().getValue(), VisConstants.CUSTOMCOMBOOPTION3)){
			getKceComboBox().setValue(VisConstants.NONECOMBOOPTION);
		}

		Task<Void> task = new Task<>() {
			@Override
			protected Void call() {
				loadReasonerButtonActionActionPerformed(event);
				return null;
			}
		};

		Stage loadingStage = parent.artPanel.showLoadingStage(task);

		task.setOnSucceeded(e -> loadingStage.close());
		task.setOnFailed(e -> {
			task.getException().printStackTrace();
			parent.artPanel.showAlertDialog("Error", "Reasoner could not be loaded.",  "Try another reasoner.",
					Alert.AlertType.ERROR);
			loadingStage.close();
		});

		new Thread(task).start();
	}

	 void loadReasonerButtonActionActionPerformed(ActionEvent event) {
		String x = getReasonerCombo().getValue();
		//parent.artPanel.setShowConnectors(true);
		if(toggleSwitch != null)
			toggleSwitch.setSelected(false);

		if ((x != null) && (!x.isEmpty())) {
			try {
				boolean loaded = parent.loadReasoner(x);
				if(!loaded) {
					System.out.println("Reasoner could not be loaded.");
					return;
				}
				createButtonActionActionPerformed(event);
				ArrayList<String> recent = new ArrayList<>();
				String selected = (String) getOntologyCombo().getValue();
				recent.add(selected);

				for (Object item : getOntologyCombo().getItems()) {
					recent.add(item.toString());
				}

				FileOutputStream fOut = new FileOutputStream("recent.txt");
				PrintStream pStream = new PrintStream(fOut);
				pStream.println(selected);
				for (String str : recent) {
					if (!str.equals(selected)) {
						pStream.println(str);
					}
				}
				pStream.flush();
				pStream.close();

				// Needed to solve concurrency issue
				PauseTransition pause = new PauseTransition(Duration.millis(100));
				pause.setOnFinished(e -> Platform.runLater(() -> applyCheckBoxFunctions(event)));
				pause.play();

			} catch (Exception e) {
				e.printStackTrace();
				parent.artPanel.showAlertDialog("Error", "Reasoner could not be loaded.", e.getMessage(),
						Alert.AlertType.ERROR);
				throw new RuntimeException(e);
			}
		}
	}

	private void cleanConnectorActionPerformed(ActionEvent event) {
		parent.artPanel.cleanConnectors();
		Platform.runLater(parent.artPanel.getRedrawRunnable());
	}

	private void createButtonActionActionPerformed(ActionEvent event) {
		parent.createButtonAction();
	}

	private void restoreViewButtonActionActionPerformed(ActionEvent event) {
		parent.restoreViewButtonAction(event);
	}

	private void comboBox0ItemItemStateChanged(String selectedItem) {
		if (parent.artPanel.getVisGraph() == null) return;
		String key = parent.artPanel.getVisGraph().getQualifiedLabelMap().get(selectedItem);
		if (key != null) {
            parent.artPanel.focusOnShape(key, null);

        }
	}

	private void handleAutoComplete(String input, ObservableList<String> dataList) {
		ObservableList<String> filteredList = FXCollections.observableArrayList();

		if (input.isEmpty()) {
			comboBox.getSelectionModel().clearSelection();
			comboBox.setItems(dataList);
			comboBox.hide();
		} else {
			for (String item : dataList) {
				if (item.toLowerCase().contains(input.toLowerCase())) {
					filteredList.add(item);
				}
			}
			comboBox.setItems(filteredList);

			if (!filteredList.isEmpty()) {
				comboBox.show();
			}
		}
	}

	private void saveViewButtonActionActionPerformed(ActionEvent event) {
		parent.saveViewButtonAction(event);
	}

	public void loadSearchCombo() {
		ArrayList<String> temp;
		getComboBox0().getItems().clear();
		temp = new ArrayList<>();

		for (Entry<String, String> s : parent.artPanel.getVisGraph().getQualifiedLabelMap().entrySet()) {
			temp.add(s.getKey());
		}

		Collections.sort(temp);
		getComboBox0().getItems().addAll(temp);
	}

	private void propertiesActionActionPerformed(ActionEvent event) {
		Set<Entry<String, Shape>> classesInGraph = parent.artPanel.getVisGraph().getClassesInGraph();
		AtomicBoolean isGraphTooLarge = new AtomicBoolean(false);

		Task<Void> task = new Task<>() {
			@Override
			protected Void call() {
                for (Entry<String, Shape> entry : classesInGraph) {
                    if ((entry.getValue() instanceof VisClass) && (entry.getValue().asVisClass().getPropertyBox() != null)) {
                        entry.getValue().asVisClass().getPropertyBox().setVisible(getPropertiesCheckBox().isSelected());
                        if (!getPropertiesCheckBox().isSelected()) {
                            if(entry.getValue().asVisClass().getPropertyBox() != null)
                                parent.artPanel.compactNodes(entry.getValue().asVisClass());
                        }
                    }
                }
                Platform.runLater(parent.artPanel.getCanvasAdjusterRunnable());
                parent.artPanel.setStateChanged(true);
                Platform.runLater(parent.artPanel.getRelaxerRunnable());
				return null;
			}
		};

		Stage loadingStage = parent.artPanel.showLoadingStage(task);

		task.setOnSucceeded(e -> {
			loadingStage.close();
			parent.artPanel.setStateChanged(true);
			Platform.runLater(() -> parent.artPanel.getRelaxerRunnable());
		});

		task.setOnFailed(e -> {
			loadingStage.close();
			getPropertiesCheckBox().setSelected(false);
			if (isGraphTooLarge.get()) {
				parent.artPanel.showAlertDialog("Information Dialog", "Graph is too large to show properties.",
						"We recommend displaying the node properties one at a time.", Alert.AlertType.INFORMATION);
			} else {
				parent.artPanel.showAlertDialog("Error", "Properties could not be loaded.", "Try again.",
						Alert.AlertType.ERROR);
			}
		});

		task.setOnCancelled(e -> getPropertiesCheckBox().setSelected(false));

		new Thread(task).start();
	}

	private void reduceActionActionPerformed(ActionEvent event) {
		if (getReduceCheckBox().isSelected()) {
			if (parent.artPanel.getOntology() != null) {
				Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
				alert.setTitle("Confirmation Dialog");
				alert.setHeaderText("Warning! this could modify source ontology");
				alert.setContentText("Proceed?");
				Optional<ButtonType> result = alert.showAndWait();
				if (result.isPresent() && result.get() == ButtonType.OK) {
					// parent.artPanel.applyStructuralReduction();
				} else {
					getReduceCheckBox().setSelected(false);
				}
			} else {
				parent.artPanel.showAlertDialog("Information Dialog", null,
						"Please load an ontology first.", Alert.AlertType.INFORMATION);
			}
		}
	}

	private void fileSystemButtonActionActionPerformed(ActionEvent event) {
		FileChooser selector = new FileChooser();
		selector.setInitialDirectory(new File(System.getProperty("user.dir")));
		selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("OWL Files", "*.owl"));
		File selectedFile = selector.showOpenDialog(parent.getPrimaryStage());

		// Cancel option
		if (selectedFile == null) {
			return;
		}

		String x = null;
		try {
			x = selectedFile.toURI().toURL().toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if ((x != null) && (!x.isEmpty())) {
			getOntologyCombo().setValue(x);
		}
	}

	public void restorePropertyCheckboxes() {
		getPropertiesCheckBox().setSelected(false);
		getPropertiesCheckBox().setDisable(false);
	}

	public void restoreSliderValue() {
		if(size != null) {
			parent.artPanel.setFactor(1.0);
		}
		getZoomSlider().setValue(1);
	}

    private void resetConnectorsSwitch() {
        if (toggleSwitch != null) {
            toggleSwitch.setSelected(false);
            toggleSwitch.setText("Show");
            parent.artPanel.setShowConnectors(false);
        }
    }


    Dimension2D size = null;
	private CheckBox renderLabel;
	private CheckBox qualifiedNames;
	private VBox snapshotPanel;

	private void zoomSliderChangeStateChanged(Number newValue) {
		if (parent.artPanel != null) {
			if (size == null) {
				size = new Dimension2D(parent.artPanel.getWidth(), parent.artPanel.getHeight());
				parent.artPanel.setOriginalSize(size);
			}
			parent.artPanel.setFactor(newValue.doubleValue());
			Platform.runLater(parent.artPanel.getCanvasAdjusterRunnable());
		}
	}


	/** class expression (query) */
	private void selectNodesClassExpressionActionPerformed(ActionEvent event) {
		Stage stage = new Stage();
		stage.setMinWidth(800);
		stage.setMinHeight(500);
		stage.setMaxHeight(700);
		stage.setTitle("Query (Class Expression)");
		stage.initModality(Modality.APPLICATION_MODAL);

		VBox classExpressionBox = new VBox(10);
		classExpressionBox.setPadding(new Insets(10));

		StackPane parentTitle = createTitlePane("Parent");
		StackPane childTitle = createTitlePane("Child");
		VBox parentBox = createContainerNoSize(parentTitle, getParentClassExpression(), childTitle, getChildClassExpression());

		HBox bottomSection = new HBox(10);
		bottomSection.setPadding(new Insets(10, 0, 0, 0));

		VBox parentBoxContainer = selectParentCheckBox();
		VBox childBoxContainer = selectChildCheckBox();
		bottomSection.getChildren().addAll(parentBoxContainer, childBoxContainer);
		HBox.setHgrow(parentBoxContainer, Priority.ALWAYS);
		HBox.setHgrow(childBoxContainer, Priority.ALWAYS);

		HBox submitHelp = new HBox(5);
		Button helpButton = getHelpButtonCE();
		Button submitButton = submitCE();

		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);

		submitHelp.getChildren().addAll(helpButton, spacer, submitButton);
		submitHelp.setStyle("-fx-alignment: center-left;");

		classExpressionBox.getChildren().addAll(parentBox, bottomSection, submitHelp);

		Scene scene = new Scene(classExpressionBox, 800, 500);
		ClassLoader c = Thread.currentThread().getContextClassLoader();
		scene.getStylesheets().add(Objects.requireNonNull(c.getResource("styles.css")).toExternalForm());
		stage.setScene(scene);
		stage.show();
	}


	private Button getHelpButtonCE(){
		if (helpButtonCE == null) {
			helpButtonCE = new Button("?");
			helpButtonCE.getStyleClass().add("button");
			helpButtonCE.setStyle(
					"-fx-font-size: 14px; -fx-shape: 'M 0 50 a 50 50 0 1 1 100 0 a 50 50 0 1 1 -100 0'; " +
							"-fx-background-radius: 50%; -fx-min-width: 35px; -fx-min-height: 35px; " +
							"-fx-pref-width: 35px; -fx-pref-height: 35px;"
			);
			//helpButtonCE.setOnAction(this::helpButtonCEActionActionPerformed);
		}
		return helpButtonCE;
	}

	private Button submitCE(){
		if (submitButtonCE == null) {
			submitButtonCE = new Button("Submit");
			submitButtonCE.getStyleClass().add("button");
			submitButtonCE.setCursor(Cursor.HAND);
			submitButtonCE.setMinWidth(40);

			submitButtonCE.setOnAction(this::submitButtonCEActionActionPerformed);
		}
		return submitButtonCE;
	}

	private TextField getParentClassExpression() {
		if (parentField == null) {
			parentField = new TextField();
			parentField.setPromptText("Type here...");
		}
		return parentField;
	}

	private TextField getChildClassExpression() {
		if (childField == null) {
			childField = new TextField();
			childField.setPromptText("Type here...");
		}
		return childField;
	}


	private VBox selectParentCheckBox() {
		parentListBox = new VBox(5);
		StackPane parentTitle = createTitlePane("Parent");
		parentSearchField = new TextField();
		parentSearchField.setPromptText("Search node...");

		parentCheckBoxList = new ListView<>();

		ObservableList<CheckBox> checkBoxList = toCheckBoxList(getAllShapeMap(), true);
		parentCheckBoxList.setItems(checkBoxList);

		parentSearchField.textProperty().addListener((observable, oldValue, newValue) -> filterCheckBoxes(checkBoxList, newValue, parentCheckBoxList));

		parentListBox.getChildren().addAll(parentTitle, parentSearchField, parentCheckBoxList);
		parentListBox.setMaxWidth(Double.MAX_VALUE);

		return parentListBox;
	}

	private VBox selectChildCheckBox() {
		childListBox = new VBox(5);
		StackPane childTitle = createTitlePane("Child");
		childSearchField = new TextField();
		childSearchField.setPromptText("Search node...");

		childCheckBoxList = new ListView<>();

		ObservableList<CheckBox> checkBoxList = toCheckBoxList(getAllShapeMap(), false);
		childCheckBoxList.setItems(checkBoxList);

		childSearchField.textProperty().addListener((observable, oldValue, newValue) -> filterCheckBoxes(checkBoxList, newValue, childCheckBoxList));

		childListBox.getChildren().addAll(childTitle, childSearchField, childCheckBoxList);
		childListBox.setMaxWidth(Double.MAX_VALUE);

		return childListBox;
	}


	private void handleParentCheckBoxSelection(CheckBox selectedCheckBox, ObservableList<CheckBox> checkBoxList) {
		boolean isSelected = selectedCheckBox.isSelected();
		for (CheckBox checkBox : checkBoxList) {
			if (checkBox != selectedCheckBox) {
				checkBox.setSelected(false);
			}
		}

		if (isSelected) {
			parentSearchField.setPromptText(selectedCheckBox.getText());
			selectedParent = getShapeByLabel(selectedCheckBox.getText());
			if (selectedChild != null) return;
			if (selectedParent != null) {
				Set<Shape> descendants = selectedParent.getDescendants();
				childCheckBoxList.setItems(toCheckBoxList(descendants, false));
			}
		} else {
			parentSearchField.setPromptText("Search node...");
			selectedParent = null;
			if (selectedChild == null) {
				childCheckBoxList.setItems(toCheckBoxList(getAllShapeMap(), false));
				parentCheckBoxList.setItems(toCheckBoxList(getAllShapeMap(), true));
			}
		}
	}

	private void handleChildCheckBoxSelection(CheckBox selectedCheckBox, ObservableList<CheckBox> checkBoxList) {
		boolean isSelected = selectedCheckBox.isSelected();
		for (CheckBox checkBox : checkBoxList) {
			if (checkBox != selectedCheckBox) {
				checkBox.setSelected(false);
			}
		}

		if (isSelected) {
			childSearchField.setPromptText(selectedCheckBox.getText());
			selectedChild = getShapeByLabel(selectedCheckBox.getText());
			if (selectedParent != null) return;
			if (selectedChild != null) {
				Set<Shape> ancestors = selectedChild.getAncestors();
				parentCheckBoxList.setItems(toCheckBoxList(ancestors, true));
			}
		} else {
			childSearchField.setPromptText("Search node...");
			selectedChild = null;
			if (selectedParent == null) {
				parentCheckBoxList.setItems(toCheckBoxList(getAllShapeMap(), true));
				childCheckBoxList.setItems(toCheckBoxList(getAllShapeMap(), false));
			}
		}
	}

	private Set<Shape> getAllShapeMap() {
		Set<Shape> allNodes = new HashSet<>();
		if (parent.artPanel.getVisGraph() != null) {
			Map<String, Shape> shapeMap = parent.artPanel.getVisGraph().shapeMap;
			for (Shape shape : shapeMap.values()) {
				if (shape instanceof VisClass) {
					allNodes.add(shape);
				}
			}
		}
		return allNodes;
	}

	private ObservableList<CheckBox> toCheckBoxList(Set<Shape> nodeList, Boolean isParent) {
		ObservableList<CheckBox> checkBoxList = FXCollections.observableArrayList();
		for (Shape node : nodeList) {
			CheckBox checkBox = new CheckBox(node.getLabel());
			checkBoxList.add(checkBox);
			if (isParent){
				checkBox.setOnAction(event -> handleParentCheckBoxSelection(checkBox, checkBoxList));
			} else {
				checkBox.setOnAction(event -> handleChildCheckBoxSelection(checkBox, checkBoxList));
			}
		}
		return checkBoxList;
	}

	private VisClass getShapeByLabel(String label) {
		if (parent.artPanel.getVisGraph() != null) {
			Map<String, Shape> shapeMap = parent.artPanel.getVisGraph().shapeMap;
			for (Shape shape : shapeMap.values()) {
				if (shape instanceof VisClass && shape.getLabel().equals(label)) {
					return (VisClass) shape;
				}
			}
		}
		return null;
	}

	private void filterCheckBoxes(ObservableList<CheckBox> checkBoxList, String searchText, ListView<CheckBox> childCheckBoxList) {
		ObservableList<CheckBox> filteredList = FXCollections.observableArrayList();
		for (CheckBox checkBox : checkBoxList) {
			if (checkBox.getText().toLowerCase().contains(searchText.toLowerCase())) {
				filteredList.add(checkBox);
			}
		}
		childCheckBoxList.setItems(filteredList);
	}

	private void submitButtonCEActionActionPerformed(ActionEvent event) {
		if ( selectedChild == null) {
			System.out.println("Please select a child node");
		}
		if ( selectedParent == null) {
			System.out.println("Please select a parent node");
		}
		if (selectedChild != null && selectedParent != null)
			System.out.println("selectedChild " + selectedChild.getLabel() + " selectedParent " + selectedParent.getLabel());
	}
}
