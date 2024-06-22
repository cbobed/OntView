package sid.OntView2.main;

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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import org.semanticweb.owlapi.model.IRI;
import sid.OntView2.common.*;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

//VS4E -- DO NOT REMOVE THIS LINE!
public class TopPanelBackup extends Canvas implements ControlPanelInterface {

	private static final long serialVersionUID = 1L;
	private Button loadOntologyButton;
	private ComboBox<Object> loadOntologyCombo;
	private ComboBox<String> loadReasonerCombo;
	private Button loadReasonerButton;
	private VBox panelLoad;
	private CheckBox expandCheckBox;
	private VBox panelCheckBox;
	private Button saveViewButton;
	private Button restoreViewButton;
	private VBox viewPanel;
	private Button saveImageButton;
	private Button saveImagePartialButton;
	private Label label0;
	private ComboBox<String> comboBox;
	private ComboBox<String> kceComboBox;
	private HBox panel0;
	private Mine parent;
    static String  RESOURCE_BASE ;
	private CheckBox Properties;
	private Button fileSystemButton;
	private Slider zoomSlider;
	private CheckBox reduceCheckBox;
	private Label reduceLabel;
	private HBox mainPane;

	public TopPanelBackup(Mine pparent){
		parent = pparent;
		initComponents();
	}
	public TopPanelBackup() {
		initComponents();

	}

	public Node getMainPane() {
		return mainPane;
	}

	// REVISAR
	private void initComponents() {
		mainPane = new HBox(10);
		mainPane.setPadding(new Insets(10));
		//mainPane.maxWidth(Double.MAX_VALUE);
		//mainPane.maxHeight(90);
		mainPane.setStyle("-fx-border-color: black; -fx-background-color: #e5e4e0; -fx-border-width: 1; -fx-border-style: solid;");

		VBox loadOntologyOptions = createLoadOntologyOptions();
		VBox checkBoxPanel = getPanelCheckBox();
		Slider zoomSlider = getZoomSlider();
		VBox viewSnapshotPanel = getViewSnapshotPanel();

		mainPane.getChildren().addAll(loadOntologyOptions, checkBoxPanel, zoomSlider, viewSnapshotPanel);
		mainPane.setAlignment(Pos.CENTER);
		mainPane.setSpacing(20);

		setWidth(1300);
		setHeight(100);

	}

	private VBox getSnapshotPanel() {
		if (snapshotPanel == null) {
			snapshotPanel = new VBox();
			snapshotPanel.setPadding(new Insets(5));

			Text titleText = new Text("Snapshot");
			titleText.setFill(Color.BLUE);
			titleText.setFont(Font.font("Dialog", FontWeight.BOLD, FontPosture.ITALIC, 12));

			StackPane titlePane = new StackPane();
			titlePane.getChildren().add(titleText);
			StackPane.setAlignment(titleText, Pos.TOP_LEFT);
			titlePane.setPadding(new Insets(0, 0, 2, 0));

			HBox row = createRow(getSaveImageButton(), getSaveImagePartialButton());
			row.setAlignment(Pos.CENTER);

			snapshotPanel.getChildren().addAll(titlePane, row);
			snapshotPanel.getStyleClass().add("container");

		}
		return snapshotPanel;
	}

	private Button getSaveImagePartialButton() {
		if (saveImagePartialButton == null) {
			saveImagePartialButton = new Button();
			saveImagePartialButton.getStyleClass().add("button");
			saveImagePartialButton.setCursor(Cursor.HAND);
			saveImagePartialButton.setMinWidth(60);

			ClassLoader c = Thread.currentThread().getContextClassLoader();
			Image image = new Image(Objects.requireNonNull(c.getResourceAsStream("saveImageParcial.JPG")));
			saveImagePartialButton.setGraphic(new ImageView(image));

			saveImagePartialButton.setOnAction(this::saveImageButtonPartialActionActionPerformed);
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
		if (parent.artPanel!= null){
			parent.artPanel.qualifiedNames = !parent.artPanel.qualifiedNames;
			parent.artPanel.getVisGraph().changeRenderMethod(parent.artPanel.renderLabel, parent.artPanel.qualifiedNames);
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
		if (parent.artPanel!= null){
			parent.artPanel.renderLabel = !parent.artPanel.renderLabel;
			parent.artPanel.getVisGraph().changeRenderMethod(parent.artPanel.renderLabel, parent.artPanel.qualifiedNames);
		}
	}

	private Slider getZoomSlider() {
		if (zoomSlider == null) {
			zoomSlider = new Slider(0, 25, 5);
			zoomSlider.setBlockIncrement(3);
			zoomSlider.setOrientation(Orientation.VERTICAL);
			zoomSlider.setFocusTraversable(true);
			zoomSlider.getStyleClass().add("zoom-slider");
			zoomSlider.setPrefHeight(VisConstants.CONTAINER_SIZE);
			zoomSlider.setMinHeight(VisConstants.CONTAINER_SIZE);
			zoomSlider.setMaxHeight(VisConstants.CONTAINER_SIZE);
			zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> zoomSliderChangeStateChanged(newVal));
		}
		return zoomSlider;
	}
	private Button getfileSystemButton() {
		if (fileSystemButton == null) {
			fileSystemButton = new Button();
			fileSystemButton.getStyleClass().add("button");
			fileSystemButton.setCursor(Cursor.HAND);

			ClassLoader c = Thread.currentThread().getContextClassLoader();
			Image icon = new Image(Objects.requireNonNull(c.getResourceAsStream("folder.png")));
			ImageView imageView = new ImageView(icon);
			imageView.setFitWidth(15);
			imageView.setFitHeight(15);
			imageView.setPreserveRatio(true);
			fileSystemButton.setGraphic(imageView);
			fileSystemButton.setOnAction(this::fileSystemButtonActionActionPerformed);
		}
		return fileSystemButton;
	}
	private CheckBox getPropertiesCheckBox() {
		if (Properties == null) {
			Properties = new CheckBox("properties");
			Properties.setText("properties");
			Properties.setCursor(Cursor.HAND);
			Properties.setOnAction(this::PropertiesActionActionPerformed);
		}
		return Properties;
	}
	private HBox getPanel0() {
		if (panel0 == null) {
			panel0 = new HBox();
			panel0.setPadding(new Insets(5));
			panel0.setSpacing(5);
			panel0.getStyleClass().add("container");

			panel0.getChildren().addAll(getLabel0(), getComboBox0());
		}
		return panel0;
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

			comboBox.valueProperty().addListener((options, oldValue, newValue) -> {
				comboBox0ItemItemStateChanged(newValue);
			});

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
					VisConstants.KCECOMBOOPTION1,
					VisConstants.KCECOMBOOPTION2,
					VisConstants.KCECOMBOOPTION3);
			kceComboBox.setItems(items);

			HBox.setHgrow(kceComboBox, Priority.ALWAYS);
			kceComboBox.setMaxWidth(Double.MAX_VALUE);

			if (!items.isEmpty()) {
				kceComboBox.setValue(items.get(0));
			}

			if (parent.artPanel!=null) {
				parent.artPanel.setKceOption((String)getKceComboBox().getItems().get(0));
			}
			kceComboBox.setOnAction(this::kceItemItemStateChanged);
		}
		return kceComboBox;
	}

	private VBox getViewSnapshotPanel() {
		VBox viewPanel = getViewPanel();
		VBox snapshotPanel = getSnapshotPanel();

		HBox firstRow = new HBox(10, viewPanel, snapshotPanel);
		VBox combinedPanel = createContainer(false, firstRow, getPanel0());
		combinedPanel.setPrefHeight(VisConstants.CONTAINER_SIZE);
		combinedPanel.setMinHeight(VisConstants.CONTAINER_SIZE);
		combinedPanel.setMaxHeight(VisConstants.CONTAINER_SIZE);

		return combinedPanel;
	}

	private void fillKceComboBox(ObservableList<String> items){
		items.addAll(VisConstants.KCECOMBOOPTION1, VisConstants.KCECOMBOOPTION2, VisConstants.KCECOMBOOPTION3);

	}

	protected void kceItemItemStateChanged(ActionEvent event) {
		// TODO Auto-generated method stub
		if (parent.artPanel!=null) {
			parent.artPanel.setKceOption(kceComboBox.getSelectionModel().getSelectedItem());
			parent.artPanel.doKceOptionAction();
		}
	}

	private Label getLabel0(){
		if(label0==null){
			label0 = new Label();
			ClassLoader c = Thread.currentThread().getContextClassLoader();
			Image image = new Image(Objects.requireNonNull(c.getResourceAsStream("search.JPG")));
			ImageView imageView = new ImageView(image);
			label0.setGraphic(imageView);
		}
		return label0;
	}

	private CheckBox getExpandCheckBox() {
		if (expandCheckBox == null) {
			expandCheckBox = new CheckBox("expand");
			expandCheckBox.setCursor(Cursor.HAND);
			expandCheckBox.setOnAction(this::expandCheckBoxActionActionPerformed);
		}
		return expandCheckBox;
	}

	public Button getLoadReasonerButton() {
		if (loadReasonerButton == null) {
			loadReasonerButton = new Button("Sync");
			loadReasonerButton.setMinWidth(80);
			loadReasonerButton.setCursor(Cursor.HAND);
			loadReasonerButton.setFont(Font.font("Dialog", FontWeight.NORMAL, 10));
			loadReasonerButton.getStyleClass().add("button");
			loadReasonerButton.setOnAction(this::loadReasonerButtonActionActionPerformed);
		}
		return loadReasonerButton;
	}


	public Label getReduceLabel() {
		if (reduceLabel == null) {
			reduceLabel = new Label("reduce");
			reduceLabel.setText("reduce");
			reduceLabel.setFont(Font.font("Dialog", FontWeight.NORMAL, 9));
		}
		return reduceLabel;
	}

	private VBox createLoadOntologyOptions() {
		if (panelLoad == null ) {
			ComboBox<Object> urlComboBox = getOntologyCombo();
			Button loadOntologyButton = getLoadOntologyButton();
			Button folderOntologyButton = getfileSystemButton();

			ComboBox<String> reasonerComboBox = getReasonerCombo();
			ComboBox<String> kceComboBox = getKceComboBox();
			Button syncButton = getLoadReasonerButton();

			HBox row1 = createRow(urlComboBox, folderOntologyButton, loadOntologyButton);
			HBox row2 = createRow(reasonerComboBox, kceComboBox, syncButton);

			panelLoad = createContainer(true, row1, row2);
		}
		return panelLoad;
	}

	private HBox createRow(Node... children) {
		HBox row = new HBox(5);
		row.getChildren().addAll(children);
		return row;
	}

	private VBox createContainer(boolean applyStyle, Node... children) {
		VBox container = new VBox(7);
		container.getChildren().addAll(children);
		container.setPadding(new Insets(5));
		if (applyStyle) {
			container.getStyleClass().add("container");
		}
		container.setPrefHeight(VisConstants.CONTAINER_SIZE);
		container.setMinHeight(VisConstants.CONTAINER_SIZE);
		container.setMaxHeight(VisConstants.CONTAINER_SIZE);
		container.setAlignment(Pos.CENTER);
		return container;
	}

	private CheckBox getReduceCheckBox() {
		if (reduceCheckBox == null) {
			reduceCheckBox = new CheckBox();
			reduceCheckBox.setFont(Font.font("Dialog", FontWeight.NORMAL, 10));
			reduceCheckBox.setOnAction(this::reduceActionActionPerformed);
		}
		return reduceCheckBox;
	}

	private Button getSaveImageButton() {
		if (saveImageButton == null) {
			saveImageButton = new Button();
			saveImageButton.getStyleClass().add("button");
			saveImageButton.setCursor(Cursor.HAND);
			saveViewButton.setMinWidth(60);

			ClassLoader c = Thread.currentThread().getContextClassLoader();
			Image image = new Image(Objects.requireNonNull(c.getResourceAsStream("saveImage.JPG")));
			saveImageButton.setGraphic(new ImageView(image));
			saveImageButton.setOnAction(this::saveImageButtonActionActionPerformed);
		}
		return saveImageButton;
	}

	private VBox getViewPanel() {
		if (viewPanel == null) {
			viewPanel = new VBox();
			viewPanel.setPadding(new Insets(5));

			Text titledPane = new Text("View");
			titledPane.setFill(Color.BLUE);
			titledPane.setFont(Font.font("Dialog", FontWeight.BOLD, FontPosture.ITALIC, 12));

			StackPane titlePane = new StackPane();
			titlePane.getChildren().add(titledPane);
			StackPane.setAlignment(titledPane, Pos.TOP_LEFT);
			titlePane.setPadding(new Insets(0, 0, 4, 0));

			HBox row = createRow(getSaveViewButton(), getRestoreViewButton());
			row.setAlignment(Pos.CENTER);

			viewPanel.getChildren().addAll(titlePane, row);
			viewPanel.getStyleClass().add("container");

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
		}
		return saveViewButton;
	}
	private VBox getPanelCheckBox() {
		if (panelCheckBox == null) {
			panelCheckBox = new VBox();
			//panelCheckBox.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-border-style: solid;");

			HBox row1 = createRow(getPropertiesCheckBox(), getExpandCheckBox());
			HBox row2 = createRow(getRenderLabel(), getQualifiedNames());

			panelCheckBox = createContainer(true, row1, row2);
			panelCheckBox.setSpacing(20);
			panelCheckBox.setMinWidth(200);
		}
		return panelCheckBox;
	}

	private Button getLoadOntologyButton() {
		if (loadOntologyButton == null) {
			loadOntologyButton = new Button("Load Ont");
			loadOntologyButton.setCursor(Cursor.HAND);
			loadOntologyButton.setFont(Font.font("Dialog", FontWeight.NORMAL, 10));
			loadOntologyButton.getStyleClass().add("button");
			loadOntologyButton.setMinWidth(100);
			loadOntologyButton.setOnAction(this::OntologyButtonActionActionPerformed);
		}
		return loadOntologyButton;
	}

	public ComboBox<String> getReasonerCombo() {
		if (loadReasonerCombo == null) {
			loadReasonerCombo = new ComboBox<>(FXCollections.observableArrayList("Pellet", "JFact", "Elk", "Jcel"));
			loadReasonerCombo.setEditable(true);
			loadReasonerCombo.setPromptText("Select reasoner");
			loadReasonerCombo.getStyleClass().add("custom-combo-box");
			//HBox.setHgrow(loadReasonerCombo, Priority.ALWAYS);
			loadReasonerCombo.setMaxWidth(Double.MAX_VALUE);
			//loadReasonerCombo.setBorder(null);
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
			//HBox.setHgrow(loadOntologyCombo, Priority.ALWAYS);
			loadOntologyCombo.setMaxWidth(Double.MAX_VALUE);
			//loadOntologyCombo.setBorder(null);
			loadOntologyCombo.setDisable(false);
		}
		return loadOntologyCombo;
	}
	
	private void loadRecent(){
		try {
			ClassLoader c = Thread.currentThread().getContextClassLoader();
			BufferedReader in;
			if (new File("recent.txt").exists()){
				in = new BufferedReader(new FileReader("recent.txt"));
			}
			else {
				in = new BufferedReader(new InputStreamReader(Objects.requireNonNull(c.getResourceAsStream("recent.txt"))));
			}
			String line = "";
			int no = 0;
			ObservableList<Object> items = FXCollections.observableArrayList();
			while ((line!=null) && (no<15)){
				line = in.readLine();	
				if ((line!=null) && (!line.equals(""))){
					items.add(line);
					no++;
				}
			}
			in.close();
			loadOntologyCombo.setItems(items);
		}
		catch(IOException e ){
			e.printStackTrace();
		}
	}
	
	private void OntologyButtonActionActionPerformed(ActionEvent event) {
		String x = (String)getOntologyCombo().getValue();
		if ((x!= null) && (!x.equals(""))){
			parent.loadActiveOntology(IRI.create(x));
			loadReasonerButton.setDisable(false);
			getReasonerCombo().setDisable(false);
		}	
	}

	private void saveImageButtonActionActionPerformed(ActionEvent event) {
		parent.createImage(parent.artPanel);
	}
	
	private void saveImageButtonPartialActionActionPerformed(ActionEvent event) {
		parent.createImageFromVisibleRect(parent.artPanel);
	}

	private void loadReasonerButtonActionActionPerformed(ActionEvent event) {
		String x = (String)getReasonerCombo().getValue();
		if ((x!= null) && (!x.equals(""))) {
			try{
				parent.loadReasoner(x);
				createButtonActionActionPerformed(event);
				ArrayList<String> recent = new ArrayList<String>();
				String selected = (String)getOntologyCombo().getValue();
			  	recent.add(selected);

				for (Object item : getOntologyCombo().getItems()) {
					recent.add(item.toString());
				}

				FileOutputStream fout = new FileOutputStream ("recent.txt");
				PrintStream pstream = new PrintStream(fout);
				pstream.println(selected);
				for (String str : recent){
					if (!str.equals(selected)){
						pstream.println(str);
					}
				}
				pstream.flush();
				pstream.close();
			}
			catch(Exception e ){
				e.printStackTrace();
			}
		
		}	
	}

	private void expandCheckBoxActionActionPerformed(ActionEvent event) {
		if (!getExpandCheckBox().isSelected()) {
			parent.check=true;
			parent.createButtonAction();
		}
		else {
			parent.check = false;
			parent.createButtonAction();
		}
	}

	private void createButtonActionActionPerformed(ActionEvent event) {
		parent.createButtonAction();
	}
	private void restoreViewButtonActionActionPerformed(ActionEvent event) {
		parent.restoreViewButtonAction(event);
	}
	
	private void comboBox0ItemItemStateChanged(String selectedItem) {
		if (parent.firstItemStateChanged) {
			String key = parent.artPanel.getVisGraph().getQualifiedLabelMap().get(selectedItem);
			parent.artPanel.focusOnShape(key,null); }
		else {
			parent.firstItemStateChanged = true; }
	}

	private void saveViewButtonActionActionPerformed(ActionEvent event) {
		parent.saveViewButtonAction(event);
	}
	
	
	 public void loadSearchCombo(){
		/*
		 * Fills up the search combo with all the non-anonymous entities
		 */
		ArrayList<String> temp;
		getComboBox0().getItems().clear();
		temp = new ArrayList<>();

		for ( Entry<String,String> s : parent.artPanel.getVisGraph().getQualifiedLabelMap().entrySet()){
			temp.add(s.getKey());
		}

		Collections.sort(temp);
		getComboBox0().getItems().addAll(temp);
 	}
	 

	private void PropertiesActionActionPerformed(ActionEvent event) {
	    Set<Entry<String,Shape>> classesInGraph = parent.artPanel.getVisGraph().getClassesInGraph();	
		if (!getPropertiesCheckBox().isSelected()) {
		    for (Entry<String,Shape> entry : classesInGraph) {
		    	if ((entry.getValue() instanceof VisClass) && (entry.getValue().asVisClass().getPropertyBox()!=null)){
		    		entry.getValue().asVisClass().getPropertyBox().setVisible(false);
		    	}
		    }
		}
		else {
			for (Entry<String,Shape> entry : classesInGraph) {
		    	if ((entry.getValue() instanceof VisClass) && (entry.getValue().asVisClass().getPropertyBox()!=null)){
		    		entry.getValue().asVisClass().getPropertyBox().setVisible(true);
		    	}
		    }
		}
	}
	
	private void reduceActionActionPerformed(ActionEvent event) {	
		int answer;
		if (getReduceCheckBox().isSelected()) {
			if (parent.artPanel.getOntology() != null){
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
			}
			else {
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
		selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("OWL Files", "*.owl"));
		File selectedFile = selector.showOpenDialog(parent.getPrimaryStage());
		String x = null;
		try{
			x = selectedFile.toURI().toURL().toString();
		}catch(Exception e){}
		if ((x!= null) && (!x.equals(""))){
			getOntologyCombo().setValue(x);
			parent.loadActiveOntology(x);
			loadReasonerButton.setDisable(false);
			getReasonerCombo().setDisable(false);
		}	
	}
	
	public void restoreSliderValue(){
		getZoomSlider().setValue(5);
	}

	Dimension2D size = null;
	private CheckBox renderLabel;
	private CheckBox qualifiedNames;
	private VBox snapshotPanel;

	private void zoomSliderChangeStateChanged(Number newValue) {
		if (parent.artPanel!=null) {
			double factor = (0.5+getZoomSlider().getValue()/10.0);
			if (size == null) {
				size = new Dimension2D(parent.artPanel.getWidth(), parent.artPanel.getHeight());
				parent.artPanel.setOriginalSize(size);
			}
			parent.artPanel.getVisGraph().setZoomLevel(newValue.intValue());
			parent.artPanel.setFactor(factor);	
			parent.artPanel.scale(factor, size);
		}
	}

	private void addComponent(Pane pane, Node node, double x, double y, double width, double height) {
		node.relocate(x, y);
		pane.getChildren().add(node);
		pane.setMinSize(width, height);
	}

	private void addBilateralComponent(Pane pane, Region node, double leading, double trailing, double y, double height) {
		node.relocate(leading, y);
		node.setPrefWidth(pane.getWidth() - leading - trailing);
		node.setPrefHeight(height);
		pane.getChildren().add(node);
	}
}
	
	 
