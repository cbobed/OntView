package sid.OntView2.main;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.util.Duration;
import org.semanticweb.owlapi.model.IRI;
import sid.OntView2.common.*;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

//VS4E -- DO NOT REMOVE THIS LINE!
public class TopPanel extends Canvas implements ControlPanelInterface {

	private static final long serialVersionUID = 1L;
	private Button loadOntologyButton;
	private ComboBox<Object> loadOntologyCombo;
	private ComboBox<String> loadReasonerCombo;
	private Button loadReasonerButton;
	private VBox panelLoad;
	private VBox panelCheckBox;
	private Button saveViewButton;
	private Button restoreViewButton;
	private VBox viewPanel;
	private Button saveImageButton;
	private Button saveImagePartialButton;
	private Label label0;
	private ComboBox<String> comboBox;
	private ComboBox<String> kceComboBox;
	private VBox panel0;
	private VBox connectorPanel;
	private ToggleButton toggleSwitch;
	private Button cleanConnectorsButton;
	private Mine parent;
	private CheckBox Properties;
	private Button fileSystemButton;
	private Slider zoomSlider;
	private CheckBox reduceCheckBox;
	private VBox mainPane;
	private VBox helpPanel;
	private Button helpButton;
	private Popup helpPopup;

	public TopPanel(Mine pparent) {
		parent = pparent;
		initComponents();
	}

	public TopPanel() {
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

		VBox loadOntologyRow = createLoadOntologyRow();
		HBox otherComponentsRow = createOtherComponentsRow();

		mainPane.getChildren().addAll(loadOntologyRow, otherComponentsRow);
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
		HBox row = new HBox(20,
				createLoadOntologyOptions(),
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
			Image image = new Image(Objects.requireNonNull(c.getResourceAsStream("saveImageParcial.JPG")));
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
			Platform.runLater(parent.artPanel.getDrawerRunnable());
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
			Platform.runLater(parent.artPanel.getDrawerRunnable());
		}
	}

	private Slider getZoomSlider() {
		if (zoomSlider == null) {
			zoomSlider = new Slider();
			zoomSlider.setMin(0);
			zoomSlider.setMax(20);
			zoomSlider.setOrientation(Orientation.VERTICAL);
			zoomSlider.setValue(5);

			zoomSlider.getStyleClass().add("zoom-slider");
			zoomSlider.setPrefHeight(VisConstants.CONTAINER_SIZE);
			zoomSlider.setMinHeight(VisConstants.CONTAINER_SIZE);
			zoomSlider.setMaxHeight(VisConstants.CONTAINER_SIZE);
			zoomSlider.onMouseReleasedProperty().set((MouseEvent event) -> zoomSliderChangeStateChanged(zoomSlider.getValue()));

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
			toggleSwitch.setMinWidth(70);

			// Add a listener to handle the switch state
			toggleSwitch.selectedProperty().addListener((observable, oldValue, newValue) -> {
				if (newValue) {
					toggleSwitch.setText("Hide");
					parent.artPanel.setShowConnectors(true);
					Platform.runLater(parent.artPanel.getDrawerRunnable());
				} else {
					toggleSwitch.setText("Show");
					parent.artPanel.setShowConnectors(false);
					Platform.runLater(parent.artPanel.getDrawerRunnable());

				}
			});

		}
		return toggleSwitch;
	}

	private Button createCleanConnectorsButton() {
		if (cleanConnectorsButton == null) {
			cleanConnectorsButton = new Button("Clean");
			cleanConnectorsButton.getStyleClass().add("button");
			cleanConnectorsButton.setMinWidth(70);
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

			ObservableList<String> items = FXCollections.observableArrayList();
			comboBox.setItems(items);
			HBox.setHgrow(comboBox, Priority.ALWAYS);
			comboBox.setMaxWidth(Double.MAX_VALUE);
			comboBox.valueProperty().addListener((options, oldValue, newValue) -> comboBox0ItemItemStateChanged(newValue));

			TextField editor = comboBox.getEditor();
			editor.addEventHandler(KeyEvent.KEY_RELEASED, event -> handleAutoComplete(editor.getText(), items));

			tooltipInfo(comboBox, "Search for a class onces the ontology is loaded");
		}
		return comboBox;
	}

	private ComboBox<String> getKceComboBox() {
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
					VisConstants.RDFRANKCOMBOOPTION2);
			kceComboBox.setItems(items);

			HBox.setHgrow(kceComboBox, Priority.ALWAYS);
			kceComboBox.setMaxWidth(Double.MAX_VALUE);

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
		// TODO Auto-generated method stub
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
			loadReasonerButton.setMinWidth(80);
			loadReasonerButton.setCursor(Cursor.HAND);
			loadReasonerButton.setFont(Font.font("DejaVu Sans", FontWeight.NORMAL, 10));
			loadReasonerButton.getStyleClass().add("button");
			loadReasonerButton.setOnAction(this::loadReasonerButtonActionActionPerformed);
		}
		return loadReasonerButton;
	}

	private VBox createLoadOntologyOptions() {
		if (panelLoad == null) {
			ComboBox<String> reasonerComboBox = getReasonerCombo();
			ComboBox<String> kceComboBox = getKceComboBox();
			Button syncButton = getLoadReasonerButton();

			HBox row1 = createRow(reasonerComboBox, kceComboBox, syncButton);

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
        container.getStyleClass().add("container");
        container.setPrefHeight(VisConstants.CONTAINER_SIZE);
		container.setMinHeight(VisConstants.CONTAINER_SIZE);
		container.setMaxHeight(VisConstants.CONTAINER_SIZE);
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
			restoreViewButton.setMinWidth(70);
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
			saveViewButton.setMinWidth(70);
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
			HBox row = createRow(getPropertiesCheckBox(), getRenderLabel(), getQualifiedNames());
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
			boolean success = parent.loadActiveOntology(IRI.create(x));
			if (!success) return;
			loadReasonerButton.setDisable(false);
			getReasonerCombo().setDisable(false);
		} else {
			parent.showErrorDialog("Error", "No ontology selected.", "Please select an ontology first.");
		}
	}


	private void saveImageButtonActionActionPerformed(ActionEvent event) {
		parent.createImage(parent.artPanel);
	}

	private void saveImageButtonPartialActionActionPerformed(ActionEvent event) {
		parent.createImageFromVisibleRect(parent.artPanel);
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

	 void loadReasonerButtonActionActionPerformed(ActionEvent event) {
		String x = getReasonerCombo().getValue();
		//parent.artPanel.setShowConnectors(true);
		if(toggleSwitch != null)
			toggleSwitch.setSelected(false);

		if ((x != null) && (!x.isEmpty())) {
			try {
				boolean loaded = parent.loadReasoner(x);
				if(!loaded) return;
				createButtonActionActionPerformed(event);
				ArrayList<String> recent = new ArrayList<>();
				String selected = (String) getOntologyCombo().getValue();
				recent.add(selected);

				for (Object item : getOntologyCombo().getItems()) {
					recent.add(item.toString());
				}

				FileOutputStream fout = new FileOutputStream("recent.txt");
				PrintStream pstream = new PrintStream(fout);
				pstream.println(selected);
				for (String str : recent) {
					if (!str.equals(selected)) {
						pstream.println(str);
					}
				}
				pstream.flush();
				pstream.close();

				// Needed to solve concurrency issue
				PauseTransition pause = new PauseTransition(Duration.millis(100));
				pause.setOnFinished(e -> Platform.runLater(() -> applyCheckBoxFunctions(event)));
				pause.play();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void cleanConnectorActionPerformed(ActionEvent event) {
		parent.artPanel.cleanConnectors();
		Platform.runLater(parent.artPanel.getDrawerRunnable());
	}

	private void createButtonActionActionPerformed(ActionEvent event) {
		parent.createButtonAction();
	}

	private void restoreViewButtonActionActionPerformed(ActionEvent event) {
		parent.restoreViewButtonAction(event);
		Platform.runLater(parent.artPanel.getDrawerRunnable());
	}

	private void comboBox0ItemItemStateChanged(String selectedItem) {
		String key = parent.artPanel.getVisGraph().getQualifiedLabelMap().get(selectedItem);
		if (key != null)
			parent.artPanel.focusOnShape(key, null);
	}

	private void handleAutoComplete(String input, ObservableList<String> dataList) {
		ObservableList<String> filteredList = FXCollections.observableArrayList();

		if (input.isEmpty()) {
			comboBox.getSelectionModel().clearSelection();
			comboBox.setItems(dataList);
			comboBox.hide();
		} else {
			for (String item : dataList) {
				if (item.toLowerCase().startsWith(input.toLowerCase())) {
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
		/*
		 * Fills up the search combo with all the non-anonymous entities
		 */
		ArrayList<String> temp;
		getComboBox0().getItems().clear();
		temp = new ArrayList<>();

		for (Entry<String, String> s : parent.artPanel.getVisGraph().getQualifiedLabelMap().entrySet()) {
			if (!s.getValue().contains("SIDClass_"))
				temp.add(s.getKey());
		}

		Collections.sort(temp);
		getComboBox0().getItems().addAll(temp);
	}

	private void propertiesActionActionPerformed(ActionEvent event) {
		Set<Entry<String, Shape>> classesInGraph = parent.artPanel.getVisGraph().getClassesInGraph();
		if (!getPropertiesCheckBox().isSelected()) {
			for (Entry<String, Shape> entry : classesInGraph) {
				if ((entry.getValue() instanceof VisClass) && (entry.getValue().asVisClass().getPropertyBox() != null)) {
					entry.getValue().asVisClass().getPropertyBox().setVisible(false);
				}
			}
			parent.artPanel.compactGraph();
		} else {
			for (Entry<String, Shape> entry : classesInGraph) {
				if ((entry.getValue() instanceof VisClass) && (entry.getValue().asVisClass().getPropertyBox() != null)) {
					entry.getValue().asVisClass().getPropertyBox().setVisible(true);
				}
			}
		}
		parent.artPanel.checkAndResizeCanvas();
		parent.artPanel.setStateChanged(true);
		Platform.runLater(parent.artPanel.getRelaxerRunnable());
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
				Alert alert = new Alert(Alert.AlertType.INFORMATION);
				alert.setTitle("Information Dialog");
				alert.setHeaderText(null);
				alert.setContentText("Load ontology first");
				alert.showAndWait();
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
			parent.artPanel.scale(1.0, size);
		}
		getZoomSlider().setValue(5);
	}

	Dimension2D size = null;
	private CheckBox renderLabel;
	private CheckBox qualifiedNames;
	private VBox snapshotPanel;

	private void zoomSliderChangeStateChanged(Number newValue) {
		if (parent.artPanel != null) {
			double factor = (0.5 + (double) newValue / 10.0);
			factor = Math.round(factor * 100.0) / 100.0;
			if (size == null) {
				size = new Dimension2D(parent.artPanel.getWidth(), parent.artPanel.getHeight());
				parent.artPanel.setOriginalSize(size);
			}
			parent.artPanel.getVisGraph().setZoomLevel((int) getZoomSlider().getValue());
			parent.artPanel.setFactor(factor);
			parent.artPanel.scale(factor, size);
		}
	}
}
