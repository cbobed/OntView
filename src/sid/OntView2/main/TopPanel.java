package sid.OntView2.main;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Dimension2D;
import javafx.geometry.Insets;
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
import org.dyno.visual.swing.layouts.Bilateral;
import org.dyno.visual.swing.layouts.Constraints;
import org.dyno.visual.swing.layouts.GroupLayout;
import org.dyno.visual.swing.layouts.Leading;
import org.semanticweb.owlapi.model.IRI;
import sid.OntView2.common.Shape;
import sid.OntView2.common.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

//VS4E -- DO NOT REMOVE THIS LINE!
public class TopPanel extends Canvas implements ControlPanelInterface {

	private static final long serialVersionUID = 1L;
	private Button loadOntologyButton;
	private ComboBox<String> loadOntologyCombo, loadReasonerCombo;
	private Button loadReasonerButton;
	private VBox panelLoad;
	private CheckBox expandCheckBox;
	private VBox panel1;
	private Button saveViewButton;
	private Button restoreViewButton;
	private VBox ViewPanel;
	private Button saveImageButton;
	private Button saveImagePartialButton;
	private Label label0;
	private ComboBox<String> comboBox;
	private ComboBox<String> kceComboBox;
	private VBox panel0;
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


	// REVISAR
	private void initComponents() {
		mainPane = new Pane();
		mainPane.maxWidth(2147483647);
		mainPane.maxHeight(90);
		mainPane.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-border-style: solid;");

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
	
	private VBox getSnapshotPanel() {
		if (snapshotPanel == null) {
			snapshotPanel = new VBox();
			snapshotPanel.setSpacing(10);
			snapshotPanel.setStyle("-fx-border-color: blue; -fx-border-width: 1; -fx-border-radius: 5; -fx-border-style: solid;");

			//HBox buttonBox = new HBox(10);
			//buttonBox.
			snapshotPanel.getChildren().addAll(getSaveImageButton(), getSaveImagePartialButton());

		}
		return snapshotPanel;
	}
	
	private Button getSaveImagePartialButton() {
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
	private VBox getPanel0() {
		if (panel0 == null) {
			panel0 = new VBox();
			panel0.setStyle("-fx-border-color: #a9a9a9; -fx-border-width: 1; -fx-border-style: solid;");
			panel0.getChildren().addAll(getJLabel0(), getComboBox0());
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

			kceComboBox.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					kceItemItemStateChanged(event);
				}
			});
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

	private Label getJLabel0(){
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

	private VBox getPanelLoad() {
		if (panelLoad == null) {
			panelLoad = new VBox();
			panelLoad.setStyle("-fx-border-color: #a9a9a9; -fx-border-width: 1; -fx-border-style: solid;");

			panelLoad.getChildren().addAll(
					getfileSystemButton(),
					getOntologyCombo(),
					getLoadOntologyButton(),
					getLoadReasonerButton(),
					getReasonerCombo(),
					getKceComboBox()
			);
			/*
			Pane layoutPane = new Pane();
			layoutPane.getChildren().addAll(
					createPositionedNode(getfileSystemButton(), 404, 12),
					createPositionedNode(getOntologyCombo(), 10, 12),
					createPositionedNode(getLoadOntologyButton(), 434, 12),
					createPositionedNode(getLoadReasonerButton(), 440, 45),
					createPositionedNode(getReasonerCombo(), 10, 45),
					createPositionedNode(getKceComboBox(), 373, 45)
			);

			panelLoad.getChildren().add(layoutPane);*/

		
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

	private VBox getViewPanel() {
		if (ViewPanel == null) {
			ViewPanel = new VBox();

			TitledPane titledPane = new TitledPane();
			titledPane.setText("View");
			titledPane.setFont(Font.font("Dialog", FontWeight.BOLD, FontPosture.ITALIC, 10));
			titledPane.setCollapsible(false);

			BorderStroke borderStroke = new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT);
			titledPane.setBorder(new Border(borderStroke));

			HBox buttonBox = new HBox(10);
			buttonBox.getChildren().addAll(getSaveViewButton(), getRestoreViewButton());

			titledPane.setContent(buttonBox);
			ViewPanel.getChildren().add(titledPane);
		}
		return ViewPanel;
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
	private AnchorPane getPanel1() {
		if (panel1 == null) {
			panel1 = new VBox();
			panel1.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-border-style: solid;");

			addComponent(panel1, getPropertiesCheckBox(), 8.0, 6.0);
			addComponent(panel1, getExpandCheckBox(), 8.0, 27.0);
			addComponent(panel1, getRenderLabel(), 8.0, 45.0);
			addComponent(panel1, getQualifiedNames(), 86.0, 45.0);
		}
		return panel1;
	}
	private void addComponent(VBox pane, javafx.scene.Node node, double x, double y) {
		node.relocate(x, y);
		pane.getChildren().add(node);
	}
	private Button getLoadOntologyButton() {
		if (loadOntologyButton == null) {
			loadOntologyButton = new Button();
			loadOntologyButton.setFont(new Font("Dialog", Font.PLAIN, 10));
			loadOntologyButton.setForeground(Color.blue);
			loadOntologyButton.setText("Load Ont");
			loadOntologyButton.addActionListener(new ActionListener() {
	
				public void actionPerformed(ActionEvent event) {
					OntologyButtonActionActionPerformed(event);
				}
			});
		}
		return loadOntologyButton;
	}
	public ComboBox getReasonerCombo() {
		if (loadReasonerCombo == null) {
			loadReasonerCombo = new ComboBox();
			loadReasonerCombo.setFont(new Font("Dialog", Font.PLAIN, 10));
			loadReasonerCombo.setModel(new DefaultComboBoxModel(new Object[] { "Pellet", "JFact", "Elk", "Jcel" }));
			loadReasonerCombo.setDoubleBuffered(false);
			loadReasonerCombo.setBorder(null);
			loadReasonerCombo.setEnabled(false);
		}
		return loadReasonerCombo;
	}
	public ComboBox getOntologyCombo() {
		if (loadOntologyCombo == null) {
			loadOntologyCombo = new ComboBox();
			loadOntologyCombo.setEditable(true);
			loadOntologyCombo.setFont(new Font("Dialog", Font.PLAIN, 10));
			loadRecent();

			loadOntologyCombo.setDoubleBuffered(false);
			loadOntologyCombo.setBorder(null);
			loadOntologyCombo.setRequestFocusEnabled(false);
		}
		return loadOntologyCombo;
	}
	
	private void loadRecent(){
		try {
			ClassLoader c = Thread.currentThread().getContextClassLoader();
			BufferedReader in;
			if (new File("recent.txt").exists()){
				in = new BufferedReader(new FileReader(new File("recent.txt")));
			}
			else {
				String path = c.getResource("recent.txt").getPath();
				in = new BufferedReader(new InputStreamReader(c.getResourceAsStream("recent.txt")));
			}
			String line = "";
			int no = 0;
			while ((line!=null)&&(no<15)){
				line = in.readLine();	
				if ((line!=null)&&(!line.equals(""))){
					loadOntologyCombo.addItem((Object)line);
					no++;
				}
			}
			in.close();
				
		}
		catch(IOException e ){
			e.printStackTrace();
		}	
		
	}
	
	
	private void OntologyButtonActionActionPerformed(ActionEvent event) {
		String x = (String)getOntologyCombo().getSelectedItem();
		if ((x!= null) && (!x.equals(""))){
			parent.loadActiveOntology(IRI.create(x));
			loadReasonerButton.setEnabled(true);
			getReasonerCombo().setEnabled(true);
		}	
	}

	private void saveImageButtonActionActionPerformed(ActionEvent event) {
		parent.createImage(parent.artPanel);
	}
	
	private void saveImageButtonPartialActionActionPerformed(ActionEvent event) {
		parent.createImageFromVisibleRect(parent.artPanel);
	}

	private void loadReasonerButtonActionActionPerformed(ActionEvent event) {
		String x = (String)getReasonerCombo().getSelectedItem();
		if ((x!= null) && (!x.equals(""))) {
			try{
				parent.loadReasoner(x);
				createButtonActionActionPerformed(event);
				ArrayList<String> recent = new ArrayList<String>();
				String selected = (String)getOntologyCombo().getSelectedItem();
			  	recent.add((String)selected);
				
				for (int i=0;i<getOntologyCombo().getItemCount();i++){
					recent.add((String)getOntologyCombo().getItemAt(i));
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
		    Set<Entry<String,Shape>> classesInGraph = parent.artPanel.getVisGraph().getClassesInGraph();

		    if (getComboBox0().getModel().getSize()!=0){
		    	getComboBox0().removeAllItems();
		    }
		    temp = new ArrayList<String>();
		    
		    
//		    for (Entry<String,Shape> entry : classesInGraph) {
//				 Shape s = entry.getValue();
//				 //load non-anonymous classes
//				 if ((s instanceof VisClass) && (!s.asVisClass().isAnonymous())){
//					 if (!temp.contains(entry.getKey())) {
//					 	temp.add(entry.getKey());
//					 }
//				}
//			 }
		    
		    for ( Entry<String,String> s : parent.artPanel.getVisGraph().getQualifiedLabelMap().entrySet()){
		    	temp.add(s.getKey());
		    }
		    
		    
			Collections.sort(temp);
			for (Object item : temp) {
				getComboBox0().addItem(item);
			}
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
		           answer =  JOptionPane.showConfirmDialog(parent,"Warning! this could modify source ontology","proceed", JOptionPane.OK_CANCEL_OPTION);
		           if (answer == JOptionPane.OK_OPTION){
//		        	   parent.artPanel.applyStructuralReduction();
		           }	  
		           else{
		        	   getReduceCheckBox().setSelected(false);
		           }
			}
			else {
				JOptionPane.showMessageDialog(parent, "Load ontology first");
			}
		}

	}
	
	private void fileSystemButtonActionActionPerformed(ActionEvent event) {
		JFileChooser selector = new JFileChooser();
		selector.addChoosableFileFilter(new owlFileFilter("owl"));
		selector.showOpenDialog(this);
		String x = null;
		try{
			x = selector.getSelectedFile().toURI().toURL().toString();
		}catch(Exception e){}
		if ((x!= null) && (!x.equals(""))){
			getOntologyCombo().getModel().setSelectedItem(x);
			parent.loadActiveOntology(x);
			loadReasonerButton.setEnabled(true);
			getReasonerCombo().setEnabled(true);
		}	
	}
	
	public void restoreSliderValue(){
		getZoomSlider().setValue(5);
	}
	
	public class owlFileFilter extends FileFilter
	{
	  String fileType;	
	  public owlFileFilter(String pextension){fileType = pextension;}	
	  public boolean accept(File file){
	      if (file.getName().toLowerCase().endsWith("."+fileType) || (file.isDirectory()))
	        return true;
	    return false;
	  }
	  @Override
	  public String getDescription() {return fileType;}
	}


	Dimension2D size = null;
	private CheckBox renderLabel;
	private CheckBox qualifiedNames;
	private VBox snapshotPanel;

	private void zoomSliderChangeStateChanged(Number newValue) {
		if (parent.artPanel!=null) {
			double factor = (0.5+getZoomSlider().getValue()/10.0);
			if (size == null) {
				size= parent.artPanel.getSize();
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
	
}
	
	 
