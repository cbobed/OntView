package sid.OntView2.main;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.*;
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
import javafx.stage.*;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;
import sid.OntView2.common.*;
import sid.OntView2.utils.ErrorHandler;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

//VS4E -- DO NOT REMOVE THIS LINE!
public class TopPanel extends Canvas implements ControlPanelInterface {
    private static final Logger logger = LogManager.getLogger(TopPanel.class);

	//private static final long serialVersionUID = 1L;
	private Button loadOntologyButton, loadReasonerButton, saveViewButton, restoreViewButton,
        saveImageButton, saveImagePartialButton, cleanConnectorsButton, fileSystemButton, helpButton,
        expressionButton, changeSelectionStrategy, moreOptions;
	private ComboBox<Object> loadOntologyCombo;
	private ComboBox<String> loadReasonerCombo, comboBox, kceComboBox;
	private VBox mainPane, panelLoad, panelCheckBox, viewPanel, panel0, connectorPanel, helpPanel,
        classExpressionPanel, percentagePanel;
	private Label label0;
	private ToggleButton toggleSwitch;
	private CheckBox Properties, reduceCheckBox;
	private Slider zoomSlider;
    private Spinner<Integer> percentageSpinner;
	private Popup helpPopup;
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
			Image image = new Image(Objects.requireNonNull(c.getResourceAsStream("assets/saveImagePartial.JPG")));
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

	public CheckBox getQualifiedNames() {
		if (qualifiedNames == null) {
			qualifiedNames = new CheckBox("qualified names");
			qualifiedNames.setCursor(Cursor.HAND);
			qualifiedNames.setOnAction(this::qualifiedNamesActionActionPerformed);
		}
		return qualifiedNames;
	}

	public void qualifiedNamesActionActionPerformed(ActionEvent event) {
		if (parent.artPanel != null) {
			parent.artPanel.qualifiedNames = getQualifiedNames().isSelected();
			parent.artPanel.getVisGraph().changeRenderMethod(parent.artPanel.renderLabel, parent.artPanel.qualifiedNames,
                parent.artPanel.selectedLanguage);
			Platform.runLater(parent.artPanel.getRedrawRunnable());
		}
	}

	public CheckBox getRenderLabel() {
		if (renderLabel == null) {
			renderLabel = new CheckBox("label");
			renderLabel.setCursor(Cursor.HAND);
            renderLabel.setOnAction(this::selectLanguageActionPerformed);
		}
		return renderLabel;
	}

	public void renderLabelActionActionPerformed(ActionEvent event, String lang) {
		if (parent.artPanel != null) {
			parent.artPanel.renderLabel = getRenderLabel().isSelected();
			parent.artPanel.getVisGraph().changeRenderMethod(parent.artPanel.renderLabel, parent.artPanel.qualifiedNames, lang);
			Platform.runLater(parent.artPanel.getRedrawRunnable());
		}
	}

    private Button moreOptions() {
        if (moreOptions == null) {
            moreOptions = new Button(OntViewConstants.THREE_DOTS);
            moreOptions.setStyle("-fx-font-size: 20px; -fx-padding: -2 5 0 5;");

            ContextMenu contextMenu = new ContextMenu();
            MenuItem item1 = createMenuItemWithTooltip(VisConstants.COMPACT_GRAPH,
                "Compacts the graph to reduce visual clutter and enhance clarity");
            MenuItem item2 = createMenuItemWithTooltip("Reset zoom value",
                "Reset the zoom value to 1.0");
            contextMenu.getStyleClass().add("context-menu-custom");

            item1.setOnAction(e -> {
                parent.artPanel.compactGraph();
                contextMenu.hide();
            });

            item2.setOnAction(e -> {
                zoomSlider.setValue(1.0);
                zoomSliderChangeStateChanged(1.0);
                contextMenu.hide();
            });

            contextMenu.getItems().addAll(item1, item2);

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

    public boolean ignoreSliderListener = false;
	Slider getZoomSlider() {
		if (zoomSlider == null) {
			zoomSlider = new Slider(0.25, 2.0, 1.0);
			zoomSlider.setOrientation(Orientation.VERTICAL);
            zoomSlider.setShowTickLabels(true);
            zoomSlider.setMinWidth(30);

			zoomSlider.getStyleClass().add("zoom-slider");
			zoomSlider.setPrefHeight(VisConstants.CONTAINER_SIZE);
			zoomSlider.setMinHeight(VisConstants.CONTAINER_SIZE);
			zoomSlider.setMaxHeight(VisConstants.CONTAINER_SIZE);

            zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (ignoreSliderListener) {
                    ignoreSliderListener = false;
                    return;
                }
                zoomSliderChangeStateChanged(newVal.doubleValue());
            });
            tooltipInfo(zoomSlider, "Zoom in/out the graph");
        }
		return zoomSlider;
	}

	private void tooltipInfo(Node node, String text) {
		Tooltip tooltip = new Tooltip(text);
		tooltip.setFont(new Font("DejaVu Sans", 12));
        Tooltip.install(node, tooltip);
    }

	private Button getfileSystemButton() {
		if (fileSystemButton == null) {
			fileSystemButton = new Button();
			fileSystemButton.getStyleClass().add("button");
			fileSystemButton.setCursor(Cursor.HAND);

			ClassLoader c = Thread.currentThread().getContextClassLoader();
			Image icon = new Image(Objects.requireNonNull(c.getResourceAsStream("assets/folder.png")));
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

	public CheckBox getPropertiesCheckBox() {
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
			HBox row = createRow(getLabel0(), getComboSearchBox());
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
			toggleSwitch = new ToggleButton("Hide");
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
            tooltipInfo(toggleSwitch, "Change connectors visibility");
		}
		return toggleSwitch;
	}

	private Button createCleanConnectorsButton() {
		if (cleanConnectorsButton == null) {
			cleanConnectorsButton = new Button("Clean");
			cleanConnectorsButton.getStyleClass().add("button");
			cleanConnectorsButton.setMinWidth(60);
			cleanConnectorsButton.setOnAction(this::cleanConnectorActionPerformed);

            tooltipInfo(cleanConnectorsButton, "Clear the selection of visible connectors in the graph");
        }

		return cleanConnectorsButton;
	}

	private ComboBox<String> getComboSearchBox() {
		if (comboBox == null) {
			comboBox = new ComboBox<>();
			comboBox.setEditable(true);
			comboBox.getStyleClass().add("custom-combo-box");
            comboBox.setMaxWidth(180);
            comboBox.setPrefWidth(180);

			ObservableList<String> items = FXCollections.observableArrayList();
			comboBox.setItems(items);
            comboBox.setVisibleRowCount(5);
			HBox.setHgrow(comboBox, Priority.ALWAYS);
            comboBox.setOnAction(event -> comboBox0ItemItemStateChanged(comboBox.getSelectionModel().getSelectedItem()));

			TextField editor = comboBox.getEditor();
			editor.addEventHandler(KeyEvent.KEY_RELEASED, event -> handleAutoComplete(editor.getText(), items));

			tooltipInfo(comboBox, "Search for a class once the ontology is loaded");
		}
		return comboBox;
	}

	public ComboBox<String> getKceComboBox() {
		if (kceComboBox == null) {
			kceComboBox = new ComboBox<>();
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

            tooltipInfo(kceComboBox, "Select summarization technique");
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
			Image image = new Image(Objects.requireNonNull(c.getResourceAsStream("assets/search.JPG")));
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

            tooltipInfo(loadReasonerButton, "Run reasoning over the ontology");
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
			Image image = new Image(Objects.requireNonNull(c.getResourceAsStream("assets/saveImage.JPG")));
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

            tooltipInfo(loadOntologyButton, "Load ontology from URL or file system");
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

            tooltipInfo(loadReasonerCombo, "Select reasoner");
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

            tooltipInfo(loadOntologyCombo, "Select a recently used ontology");
        }
		return loadOntologyCombo;
	}

	private VBox createShowClassExpressionButton() {
		if (classExpressionPanel == null) {
			classExpressionPanel = new VBox();
			classExpressionPanel.setPadding(new Insets(5));
			classExpressionPanel.setSpacing(5);


			StackPane titlePane = createTitlePane("Class Expression");
			classExpressionPanel = createContainer(titlePane, classExpressionButton());
		}
		return classExpressionPanel;
	}

    private Button classExpressionButton() {
        if (expressionButton == null) {
            expressionButton = new Button("Select");
            expressionButton.getStyleClass().add("button");
            expressionButton.setOnAction(e -> {
                if (parent.artPanel != null) {
                    new ClassExpression(parent);
                }
            });

            tooltipInfo(expressionButton, "View class expressions between the selected top and bottom classes");
        }
        return expressionButton;
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
            changeSelectionStrategy = new Button(OntViewConstants.THREE_DOTS);
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

            tooltipInfo(percentageSpinner, "Set visibility percentage for the expand/collapse functionality");
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

            HelpUser helpUser = new HelpUser();

            helpButton.setOnAction(e -> helpUser.showHelpStage());
            helpPanel = createContainer(titlePane, helpButton);

            tooltipInfo(helpButton, "Open user guide and instructions");

        }
        return helpPanel;
    }

    private void loadRecent(){
        File recentFile = new File("recent.txt");
        try {
            if (!recentFile.exists()){
                if (!recentFile.createNewFile()){
                    logger.error("Could not create recent.txt file.");
                }
            }

            try (BufferedReader in = new BufferedReader(new FileReader(recentFile))) {
                String line;
                int count = 0;
                ObservableList<Object> items = FXCollections.observableArrayList();
                while (count < 15 && (line = in.readLine()) != null) {
                    if (!line.isBlank()) {
                        items.add(line);
                        count++;
                    }
                }
                loadOntologyCombo.setItems(items);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	void OntologyButtonActionActionPerformed(ActionEvent event) {
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

	void applyCheckBoxFunctions(ActionEvent e) {
		if (getPropertiesCheckBox().isSelected()) {
			propertiesActionActionPerformed(e);
		}

		if (getRenderLabel().isSelected()) {
			renderLabelActionActionPerformed(e, "en");
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
        if(parent.artPanel.diagramOverview != null && parent.artPanel.diagramOverview.overviewStage.isShowing()) { // close diagram overview
            parent.artPanel.diagramOverview.closeDiagramOverview();
        }
        parent.artPanel.cleanConnectors();
        parent.artPanel.clearCanvas();
        parent.artPanel.languagesLabels.clear();
        parent.artPanel.selectedShape = null;
        getComboSearchBox().setValue("");
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

		parent.artPanel.loadingStage = parent.artPanel.showLoadingStage(task);

        task.setOnCancelled(e -> {
            if (!parent.cancelledFlag) VisGraph.voluntaryCancel(true);
            parent.cancelledFlag = false;
        });
		task.setOnSucceeded(e -> parent.artPanel.loadingStage.close());
		task.setOnFailed(e -> {
			task.getException().printStackTrace();
			parent.artPanel.showAlertDialog("Error", "Reasoner could not be loaded.",
                ErrorHandler.getLoadAndReasonError(task.getException()), Alert.AlertType.ERROR);
            parent.artPanel.loadingStage.close();
		});

		new Thread(task).start();
	}

	 void loadReasonerButtonActionActionPerformed(ActionEvent event) {
		String x = getReasonerCombo().getValue();
		if(toggleSwitch != null)
            parent.artPanel.setShowConnectors(true);

         //toggleSwitch.setSelected();

		if ((x != null) && (!x.isEmpty())) {
			try {
				boolean loaded = parent.loadReasoner(x);
				if(!loaded) {
                    logger.error("Reasoner could not be loaded.");
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
				parent.artPanel.showAlertDialog("Error", "Reasoner could not be loaded.",
                    ErrorHandler.getLoadAndReasonError(e), Alert.AlertType.ERROR);
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
        try {
            parent.restoreViewButtonAction(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

	private void comboBox0ItemItemStateChanged(String selectedItem) {
		if (parent.artPanel.getVisGraph() == null || selectedItem == null || selectedItem.isBlank()) return;
        String key = parent.artPanel.getVisGraph().labelMap.get(selectedItem);
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
            return;
		}

        for (String item : dataList) {
            if (item.toLowerCase().contains(input.toLowerCase())) {
                filteredList.add(item);
            }
        }
        if (!filteredList.isEmpty()) {
            comboBox.setItems(filteredList);
            comboBox.show();
        } else {
            comboBox.hide();
        }

	}

	private void saveViewButtonActionActionPerformed(ActionEvent event) {
		parent.saveViewButtonAction(event);
	}

    public void loadSearchCombo() {
		getComboSearchBox().getItems().clear();
        Set<String> labelSet = new HashSet<>();

		for (Entry<String, Shape> s : parent.artPanel.getVisGraph().shapeMap.entrySet()) {
			if (s.getValue().isVisible())
            	labelSet.add(s.getValue().getLabel());
		}

        List<String> sortedLabels = new ArrayList<>(labelSet);
        Collections.sort(sortedLabels);
        comboBox.getItems().addAll(sortedLabels);
	}

	private void propertiesActionActionPerformed(ActionEvent event) {
		Set<Entry<String, Shape>> classesInGraph = parent.artPanel.getVisGraph().getClassesInGraph();

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
                parent.artPanel.setStateChanged(true);
                Platform.runLater(parent.artPanel.getRelaxerRunnable());
                Platform.runLater(parent.artPanel.getCanvasAdjusterRunnable());
                return null;
			}
		};

		Stage loadingStage = parent.artPanel.showLoadingStage(task);

        task.setOnCancelled(e -> loadingStage.close());
		task.setOnSucceeded(e -> {
			loadingStage.close();
			parent.artPanel.setStateChanged(true);
			Platform.runLater(parent.artPanel.getRelaxerRunnable());
		});

		task.setOnFailed(e -> {
			loadingStage.close();
			getPropertiesCheckBox().setSelected(false);
			parent.artPanel.showAlertDialog("Error", "Properties could not be loaded.", "Try again.",
                Alert.AlertType.ERROR);
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

	public void restoreSliderValue() {
		if(size != null) {
			parent.artPanel.setFactor(1.0);
		}
		getZoomSlider().setValue(1);
	}

    private void resetConnectorsSwitch() {
        if (toggleSwitch != null) {
            toggleSwitch.setSelected(true);
            toggleSwitch.setText("Hide");
            parent.artPanel.setShowConnectors(true);
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

    /** Label language */
    private void selectLanguageActionPerformed(ActionEvent event) {
        if (!getRenderLabel().isSelected()) {
            parent.artPanel.selectedLanguage = "";
            renderLabelActionActionPerformed(event,"");
        } else {

            if (parent.artPanel.getVisGraph() == null) {
                return;
            }

            Stage stage = new Stage();
            stage.setTitle("Select language");
            stage.initModality(Modality.APPLICATION_MODAL);

            VBox root = createLanguageModalContent(stage);
            root.setPrefWidth(400);

            Scene scene = new Scene(root);
            ClassLoader c = Thread.currentThread().getContextClassLoader();
            scene.getStylesheets().add(Objects.requireNonNull(c.getResource("styles.css")).toExternalForm());
            stage.setScene(scene);
            stage.setResizable(false);
            stage.setAlwaysOnTop(true);

            stage.setOnCloseRequest(e -> getRenderLabel().setSelected(false));
            stage.show();
        }
    }

    private VBox createLanguageModalContent(Stage stage) {
        VBox container = new VBox(10);
        container.setPadding(new Insets(10));

        GridPane languagesGrid = new GridPane();
        languagesGrid.setHgap(15);
        languagesGrid.setVgap(10);
        languagesGrid.setAlignment(Pos.CENTER);

        VBox togglesContainer;
        ToggleGroup languageGroup;
        Button submitButton = new Button("Submit");

        if (parent.artPanel.languagesLabels == null || parent.artPanel.languagesLabels.isEmpty()) {
            Label noLabels = new Label("No language labels available in this ontology.");
            togglesContainer = createContainerNoSize(noLabels);
            languageGroup = new ToggleGroup();
            submitButton.setDisable(true);
        } else {
            languageGroup = createLanguageToggleGroup(languagesGrid);
            togglesContainer = createContainerNoSize(languagesGrid);
        }

        submitButton.setOnAction(e -> {
            RadioButton selected = (RadioButton) languageGroup.getSelectedToggle();
            parent.artPanel.selectedLanguage = selected.getText();
            getRenderLabel().setSelected(true);
            renderLabelActionActionPerformed(e,  parent.artPanel.selectedLanguage);
            stage.close();
        });

        HBox buttonContainer = new HBox();
        buttonContainer.setAlignment(Pos.CENTER_RIGHT);
        buttonContainer.getChildren().add(submitButton);

        container.getChildren().addAll(togglesContainer, buttonContainer);
        return container;
    }

    private ToggleGroup createLanguageToggleGroup(GridPane grid) {
        int columnsPerRow = 6;
        for (int i = 0; i < columnsPerRow; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / columnsPerRow);
            grid.getColumnConstraints().add(cc);
        }

        ToggleGroup languageGroup = new ToggleGroup();
        List<String> labels = new ArrayList<>(parent.artPanel.languagesLabels);
        int total = labels.size();
        int fullRows = total / columnsPerRow;
        int remainder = total % columnsPerRow;

        for (int i = 0; i < total; i++) {
            int row = i / columnsPerRow;
            int colInRow = i % columnsPerRow;
            int offset = (row == fullRows && remainder > 0) ? (columnsPerRow - remainder) / 2 : 0;

            RadioButton rb = new RadioButton(labels.get(i));
            rb.setToggleGroup(languageGroup);
            if (labels.get(i).equalsIgnoreCase("en")) rb.setSelected(true);
            rb.getStyleClass().add("language-radio");

            grid.add(rb, offset + colInRow, row);
            GridPane.setHalignment(rb, HPos.CENTER);
        }

        return languageGroup;
    }
}
