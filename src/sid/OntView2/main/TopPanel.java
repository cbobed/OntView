package sid.OntView2.main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Dimension2D;
import javafx.geometry.Orientation;
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
import javafx.stage.FileChooser;
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
	private Pane panelLoad;
	private CheckBox expandCheckBox;
	private Pane panel1;
	private Button saveViewButton;
	private Button restoreViewButton;
	private Pane viewPanel;
	private Button saveImageButton;
	private Button saveImagePartialButton;
	private Label label0;
	private ComboBox<String> comboBox;
	private ComboBox<String> kceComboBox;
	private Pane panel0;
	private Mine parent;
    static String  RESOURCE_BASE ;
	private CheckBox Properties;
	private Button fileSystemButton;
	private Slider zoomSlider;
	private CheckBox reduceCheckBox;
	private Label reduceLabel;
	private Pane mainPane;

	public TopPanel(Mine pparent){
		parent = pparent;
		initComponents();
	}
	public TopPanel() {
		initComponents();

	}

	public Node getMainPane() {
		return mainPane;
	}

	// REVISAR
	private void initComponents() {
		mainPane = new Pane();
		mainPane.maxWidth(Double.MAX_VALUE);
		mainPane.maxHeight(90);
		mainPane.setStyle("-fx-border-color: black; -fx-background-color: #e5e4e0; -fx-border-width: 1; -fx-border-style: solid;");

		addComponent(mainPane, getPanelLoad(), 12, 10, 523, 78);
		addComponent(mainPane, getZoomSlider(), 750, 2, 24, 94);
		addComponent(mainPane, getPanel1(), 541, 10, 197, 78);
		addComponent(mainPane, getSnapshotPanel(), 968, 2, 120, 51);
		addComponent(mainPane, getPanel0(), 786, 54, 300, 34);
		addComponent(mainPane, getViewPanel(), 786, 2, 179, 51);

		setWidth(1040);
		setHeight(108);

		/*
			// Define column constraints for responsive design
            ColumnConstraints col1 = new ColumnConstraints();
            col1.setPercentWidth(20); // Set percentage width for first column
            ColumnConstraints col2 = new ColumnConstraints();
            col2.setPercentWidth(80); // Set percentage width for second column
            panel1.getColumnConstraints().addAll(col1, col2);

            // Define row constraints for responsive design
            RowConstraints row1 = new RowConstraints();
            row1.setVgrow(Priority.ALWAYS); // Allow row to grow vertically
            RowConstraints row2 = new RowConstraints();
            row2.setVgrow(Priority.ALWAYS);
            RowConstraints row3 = new RowConstraints();
            row3.setVgrow(Priority.ALWAYS);
            panel1.getRowConstraints().addAll(row1, row2, row3);

            // Add components to GridPane with specified positions
            panel1.add(getPropertiesCheckBox(), 0, 0); // Columna 0, Fila 0
            panel1.add(getExpandCheckBox(), 0, 1); // Columna 0, Fila 1
            panel1.add(getRenderLabel(), 0, 2); // Columna 0, Fila 2
            panel1.add(getQualifiedNames(), 1, 2); // Columna 1, Fila 2
		 */
	}

	private Pane getSnapshotPanel() {
		if (snapshotPanel == null) {
			snapshotPanel = new Pane();
			snapshotPanel.setStyle("-fx-border-color: blue; -fx-border-width: 1; -fx-border-radius: 5; -fx-border-style: solid;");
			addComponent(snapshotPanel, getSaveImageButton(), 8, 0, 30, 26);
			addComponent(snapshotPanel, getSaveImagePartialButton(), 45, 0, 30, 26);

		}
		return snapshotPanel;
	}

	private Button getSaveImagePartialButton() {
		System.out.println("getSaveImagePartialButton");
		if (saveImagePartialButton == null) {
			saveImagePartialButton = new Button();
			ClassLoader c = Thread.currentThread().getContextClassLoader();
			Image image = new Image(Objects.requireNonNull(c.getResourceAsStream("saveImageParcial.JPG")));
			saveImagePartialButton.setGraphic(new ImageView(image));

			saveImagePartialButton.setOnAction(this::saveImageButtonPartialActionActionPerformed);
		}
		return saveImagePartialButton;
	}

	private CheckBox getQualifiedNames() {
		if (qualifiedNames == null) {
			qualifiedNames = new CheckBox();
			qualifiedNames.setFont(Font.font("Dialog", FontWeight.NORMAL, 10));
			qualifiedNames.setText("qualified names");
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
			renderLabel = new CheckBox();
			renderLabel.setFont(Font.font("Dialog", FontWeight.NORMAL, 10));
			renderLabel.setText("label");
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
			zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> zoomSliderChangeStateChanged(newVal));
		}
		return zoomSlider;
	}
	private Button getfileSystemButton() {
		if (fileSystemButton == null) {
			fileSystemButton = new Button();

			ClassLoader c = Thread.currentThread().getContextClassLoader();
			Image icon = new Image(Objects.requireNonNull(c.getResourceAsStream("folder.png")));
			ImageView imageView = new ImageView(icon);
			imageView.setFitWidth(17);
			imageView.setFitHeight(17);
			imageView.setPreserveRatio(true);
			fileSystemButton.setGraphic(imageView);
			fileSystemButton.setOnAction(this::fileSystemButtonActionActionPerformed);
		}
		return fileSystemButton;
	}
	private CheckBox getPropertiesCheckBox() {
		if (Properties == null) {
			Properties = new CheckBox();
			Properties.setFont(Font.font("Dialog", FontWeight.NORMAL, 10));
			Properties.setText("properties");
			Properties.setOnAction(this::PropertiesActionActionPerformed);
		}
		return Properties;
	}
	private Pane getPanel0() {
		if (panel0 == null) {
			panel0 = new Pane();
			panel0.setStyle("-fx-border-color: #a9a9a9; -fx-border-width: 1; -fx-border-style: solid;");
			addComponent(panel0, getLabel0(), 8, 1, 22, 24);
			addBilateralComponent(panel0, getComboBox0(), 39, 12, 4, 21);
		}
		return panel0;
	}
	private ComboBox<String> getComboBox0() {
		if (comboBox == null) {
			comboBox = new ComboBox<>();
			AutoCompletion.enable(comboBox);
			comboBox.setEditable(true);
			comboBox.setStyle("-fx-font-family: 'Dialog';" + "-fx-font-size: 10px;" +
							"-fx-font-weight: bold;" + "-fx-text-fill: blue;");

			ObservableList<String> items = FXCollections.observableArrayList();
			comboBox.setItems(items);
			comboBox.setBorder(null);

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
			kceComboBox.setStyle("-fx-font-family: 'Dialog';" + "-fx-font-size: 10px;" +
					"-fx-font-weight: bold;" + "-fx-text-fill: blue;");

			ObservableList<String> items = FXCollections.observableArrayList();
			kceComboBox.setItems(items);
			kceComboBox.setBorder(null);

			fillKceComboBox(items);
			if (parent.artPanel!=null) {
				parent.artPanel.setKceOption((String)getKceComboBox().getItems().get(0));
			}
			kceComboBox.setOnAction(this::kceItemItemStateChanged);
		}
		return kceComboBox;
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
			expandCheckBox = new CheckBox();
			expandCheckBox.setFont(Font.font("Dialog", FontWeight.NORMAL, 10));
			expandCheckBox.setText("expand");
			expandCheckBox.setOnAction(this::expandCheckBoxActionActionPerformed);
		}
		return expandCheckBox;
	}

	public Button getLoadReasonerButton() {
		if (loadReasonerButton == null) {
			loadReasonerButton = new Button();
			loadReasonerButton.setFont(Font.font("Dialog", FontWeight.NORMAL, 10));
			loadReasonerButton.setStyle("-fx-text-fill: blue;");
			loadReasonerButton.setText("Sync");
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

	private Pane getPanelLoad() {
		if (panelLoad == null) {
			panelLoad = new Pane();
			panelLoad.setStyle("-fx-border-color: #a9a9a9; -fx-border-width: 1; -fx-border-style: solid;");

			addComponent(panelLoad, getfileSystemButton(), 404, 12, 24, 23);
			addComponent(panelLoad, getOntologyCombo(), 10, 12, 388, 23);
			addComponent(panelLoad, getLoadOntologyButton(), 434, 12, 24, 23);
			addComponent(panelLoad, getLoadReasonerButton(), 440, 45, 71, 23);
			addComponent(panelLoad, getReasonerCombo(), 10, 45, 358, 23);
			// addComponent(pane, getReduceCheckBox(), 407, 44, 24, 15);
			// addComponent(pane, getReduceLabel(), 403, 52, 40, 23);
			addComponent(panelLoad, getKceComboBox(), 373, 45, 60, 23);

		}
		return panelLoad;
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
			ClassLoader c = Thread.currentThread().getContextClassLoader();
			Image image = new Image(Objects.requireNonNull(c.getResourceAsStream("saveImage.JPG")));
			saveImageButton.setGraphic(new ImageView(image));
			saveImageButton.setOnAction(this::saveImageButtonActionActionPerformed);
		}
		return saveImageButton;
	}

	private Pane getViewPanel() {
		if (viewPanel == null) {
			viewPanel = new HBox(10);

			TitledPane titledPane = new TitledPane();
			titledPane.setText("View");
			titledPane.setFont(Font.font("Dialog", FontWeight.BOLD, FontPosture.ITALIC, 10));
			titledPane.setCollapsible(false);

			BorderStroke borderStroke = new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT);
			titledPane.setBorder(new Border(borderStroke));

			// Crear un nuevo HBox que contenga los botones
			Pane contentBox = new Pane();
			addComponent(contentBox, getSaveViewButton(), 6, -4, 76, 28);
			addComponent(contentBox, getRestoreViewButton(), 88, -4, 76, 28);

			//contentBox.getChildren().addAll(getSaveViewButton(), getRestoreViewButton());

			// Establecer el nuevo HBox como el contenido del TitledPane
			titledPane.setContent(contentBox);

			// Agregar el TitledPane al viewPanel
			viewPanel.getChildren().add(titledPane);



		}
		return viewPanel;
	}
	private Button getRestoreViewButton() {
		if (restoreViewButton == null) {
			restoreViewButton = new Button();
			restoreViewButton.setFont(Font.font("Dialog", FontWeight.NORMAL, 10));
			restoreViewButton.setStyle("-fx-text-fill: blue;");
			restoreViewButton.setText("Restore");
			restoreViewButton.setOnAction(this::restoreViewButtonActionActionPerformed);
		}
		return restoreViewButton;
	}
	private Button getSaveViewButton() {
		if (saveViewButton == null) {
			saveViewButton = new Button();
			saveViewButton.setFont(Font.font("Dialog", FontWeight.NORMAL, 10));
			saveViewButton.setStyle("-fx-text-fill: blue;");
			saveViewButton.setText("Save");
			saveViewButton.setOnAction(this::saveViewButtonActionActionPerformed);
		}
		return saveViewButton;
	}
	private Pane getPanel1() {
		if (panel1 == null) {
			panel1 = new Pane();
			panel1.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-border-style: solid;");

			addComponent(panel1, getPropertiesCheckBox(), 8.0, 6.0, 8, 10);
			addComponent(panel1, getExpandCheckBox(), 8.0, 27.0, 8, 8);
			addComponent(panel1, getRenderLabel(), 8.0, 45.0, 8, 8);
			addComponent(panel1, getQualifiedNames(), 86.0, 45.0, 10, 8);
		}
		return panel1;
	}

	private Button getLoadOntologyButton() {
		if (loadOntologyButton == null) {
			loadOntologyButton = new Button();
			loadOntologyButton.setFont(Font.font("Dialog", FontWeight.NORMAL, 10));
			loadOntologyButton.setStyle("-fx-text-fill: blue;");
			loadOntologyButton.setText("Load Ont");
			loadOntologyButton.setOnAction(this::OntologyButtonActionActionPerformed);
		}
		return loadOntologyButton;
	}

	public ComboBox<String> getReasonerCombo() {
		if (loadReasonerCombo == null) {
			loadReasonerCombo = new ComboBox<>();
			loadReasonerCombo.setEditable(true);
			loadReasonerCombo.setStyle("-fx-font-family: 'Dialog';" + "-fx-font-size: 10px;" +
					"-fx-font-weight: normal;");
			loadReasonerCombo.setItems(FXCollections.observableArrayList("Pellet", "JFact", "Elk", "Jcel"));
			loadReasonerCombo.setBorder(null);
			loadReasonerCombo.setDisable(true);
		}
		return loadReasonerCombo;
	}
	public ComboBox<Object> getOntologyCombo() {
		if (loadOntologyCombo == null) {
			loadOntologyCombo = new ComboBox<>();
			loadOntologyCombo.setEditable(true);
			loadOntologyCombo.setStyle("-fx-font-family: 'Dialog';" + "-fx-font-size: 10px;" +
					"-fx-font-weight: normal;");
			loadRecent();
			loadOntologyCombo.setBorder(null);
			loadOntologyCombo.setDisable(true);
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
	private Pane snapshotPanel;

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
	
	 
