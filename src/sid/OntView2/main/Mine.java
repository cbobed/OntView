package sid.OntView2.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import javax.imageio.ImageIO;
import java.net.URL;


import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.ConsoleProgressMonitor;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;

//import org.jdesktop.swingx.autocomplete.*;

import sid.OntView2.common.Embedable;
import sid.OntView2.common.PaintFrame;
import sid.OntView2.common.VisGraph;
import sid.OntView2.common.VisPositionConfig;
import sid.OntView2.expressionNaming.SIDClassExpressionNamer;
import sid.OntView2.utils.ExpressionManager;
import uk.ac.manchester.cs.jfact.JFactFactory;

import java.security.*;
import java.util.Objects;
import java.util.Optional;

public class Mine extends Application implements Embedable{

	private Stage primaryStage;
	private static final long serialVersionUID = 1L;
	boolean DEBUG=false;

	OWLOntology activeOntology;
	OWLReasoner reasoner;
	OWLOntologyManager manager;
	HashSet<String> entityNameSet;

	PaintFrame   artPanel;
	TopPanel     nTopPanel;
	ScrollPane  scroll;
	boolean      firstItemStateChanged = false;
	Mine         self= this;
	boolean      check = true;

	/* Code for standalone app initialization */

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		createAndShowGUI(primaryStage);
	}

	public Stage getPrimaryStage() {
		return primaryStage;
	}


	public static void createAndShowGUI(Stage primaryStage) {
		primaryStage.setTitle("Viewer");
		primaryStage.setOnCloseRequest(event -> System.exit(0));

		Mine viewer = new Mine();
		viewer.self = viewer;

		viewer.entityNameSet = new HashSet<>();
		viewer.artPanel = new PaintFrame();
		viewer.artPanel.setParentFrame(viewer);

		viewer.nTopPanel = new TopPanel(viewer);

		viewer.scroll = new ScrollPane(viewer.artPanel);

		VBox root = new VBox();
		VBox.setVgrow(viewer.scroll, Priority.ALWAYS);
		root.getChildren().add(viewer.nTopPanel.getMainPane());
		root.getChildren().add(viewer.scroll);

		viewer.artPanel.setStyle("-fx-background-color: white;");
		viewer.nTopPanel.setStyle("-fx-border-color: black; -fx-border-width: 1;");

		Scene scene = new Scene(root, 800, 600);
		
		ClassLoader c = Thread.currentThread().getContextClassLoader();		
		scene.getStylesheets().add(Objects.requireNonNull(c.getResource("styles.css")).toExternalForm());
		primaryStage.setScene(scene);
		primaryStage.setMaximized(true);
		primaryStage.show();
	}

	/* Rest of methods */

	public void createButtonAction(){
		if (reasoner!= null) {
			artPanel.setCursor(Cursor.WAIT);
			//cant cast to set<OWLClassExpression> from set<OWLClass>
			HashSet<OWLClassExpression> set = new HashSet<OWLClassExpression>();
			for (OWLClass d : reasoner.getTopClassNode().getEntities())
				set.add(d);
			try {
				//set reasoner and ontology before creating
				artPanel.createReasonedGraph(set,check);
				artPanel.setCursor(Cursor.DEFAULT);

			}
			catch (Exception e1) {
				e1.printStackTrace();
				artPanel.setCursor(Cursor.DEFAULT);
			}

			while (!artPanel.isStable()){
				try {
					Thread.sleep(2000);
					System.err.println("wait");
				}
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			nTopPanel.restoreSliderValue();

		}

		artPanel.start();
	}



	protected void loadActiveOntology(IRI source){
		manager = OWLManager.createOWLOntologyManager();
		artPanel.setCursor(Cursor.WAIT);
		try {
			activeOntology = manager.loadOntologyFromOntologyDocument(source);
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			artPanel.setCursor(Cursor.DEFAULT);
			activeOntology = null;
			manager = null;
		}
		artPanel.setCursor(Cursor.DEFAULT);
		artPanel.setOntology(activeOntology);

		artPanel.setActiveOntolgySource(source.toString()); //this might FAIL

		// CBL expression manager
		if (activeOntology != null && manager != null) {
			ExpressionManager.setNamespaceManager(manager, activeOntology);

			for (String ns: ExpressionManager.getNamespaceManager().getNamespaces()) {
				System.err.println("prefix: "+ExpressionManager.getNamespaceManager().getPrefixForNamespace(ns));
				System.err.println("  ns: "+ns);

			}

		}
	}

	protected void loadActiveOntologyFromString(String ontology) {

		manager = OWLManager.createOWLOntologyManager();
		artPanel.setCursor(Cursor.WAIT);
		try {
			activeOntology = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(ontology));
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			artPanel.setCursor(Cursor.DEFAULT);
			activeOntology = null;
			manager = null;
		}
		artPanel.setCursor(Cursor.DEFAULT);
		artPanel.setOntology(activeOntology);
		artPanel.setActiveOntolgySource(ontology);
	}

	protected void loadActiveOntology(String source){
		manager = OWLManager.createOWLOntologyManager();
		artPanel.setCursor(Cursor.WAIT);
		try {
			activeOntology = manager.loadOntologyFromOntologyDocument(IRI.create(source));
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			artPanel.setCursor(Cursor.DEFAULT);
			activeOntology = null;
			manager = null;
		}
		artPanel.setCursor(Cursor.DEFAULT);
		artPanel.setOntology(activeOntology);
	}

	protected void loadReasoner(String reasonerString){
		if (activeOntology!=null) {

			ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
			// Specify the progress monitor via a configuration.  We could also specify other setup parameters in
			// the configuration, and different reasoners may accept their own defined parameters this way.
			OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor);

			// Create a reasoner that will reason over our ontology and its imports closure.  Pass in the configuration.
			reasoner = getReasonerFactory(reasonerString).createReasoner(activeOntology,config);

			// between creating and precomputing
			applyRenaming();

			reasoner.precomputeInferences();
			artPanel.setReasoner(reasoner);
		}
	}

	public void applyRenaming(){
		SIDClassExpressionNamer renamer = new SIDClassExpressionNamer(activeOntology, reasoner);
		renamer.applyNaming(true);
	}
    /*public void refreshOntology (String ontologyString, String reasonerString) {

    	 AccessController.doPrivileged(new RefreshingAction(ontologyString, reasonerString));

    }*/

	class RefreshingAction implements PrivilegedAction {
		String ontologyString;
		String reasonerString;

		public RefreshingAction(String ontologyString, String reasonerString) {
			this.ontologyString = ontologyString;
			this.reasonerString = reasonerString;
		}

		public Object run() {

			loadActiveOntology(IRI.create(ontologyString));
			if (activeOntology != null) {
				nTopPanel.getOntologyCombo().setValue(ontologyString);
				nTopPanel.getLoadReasonerButton().setDisable(false);
				nTopPanel.getReasonerCombo().setDisable(false);
				if (artPanel != null){
					artPanel.setActiveOntolgySource(ontologyString);
				}
				loadReasoner(reasonerString);
				createButtonAction();

			}
			else {
				System.err.println("Can't load ontology");
			}

			return null;
		}
	}
    /*
    public void refreshOntologyFromString(String ontologyString, String reasonerString) {
    	 AccessController.doPrivileged(new RefreshingFromStringAction(ontologyString, reasonerString));
    }*/

	class RefreshingFromStringAction implements PrivilegedAction {
		String ontologyString;
		String reasonerString;

		public RefreshingFromStringAction(String ontologyString, String reasonerString) {
			this.ontologyString = ontologyString;
			this.reasonerString = reasonerString;
		}

		public Object run() {

			loadActiveOntologyFromString(ontologyString);
			if (activeOntology != null) {
				nTopPanel.getOntologyCombo().setValue(activeOntology.getOntologyID().getOntologyIRI());
				nTopPanel.getLoadReasonerButton().setDisable(false);
				nTopPanel.getReasonerCombo().setDisable(false);
				if (artPanel != null){
					artPanel.setActiveOntolgySource(ontologyString);
				}

				loadReasoner(reasonerString);

				createButtonAction();

			}
			else {
				System.err.println("Can't load ontology");
			}

			return null;
		}
	}


	private OWLReasonerFactory getReasonerFactory(String r){
		OWLReasonerFactory reasonerFactory = null;
		if (r.equalsIgnoreCase("Pellet")) {
			reasonerFactory = new PelletReasonerFactory();
		}
		else if (r.equalsIgnoreCase("JFact")) {
			reasonerFactory = new JFactFactory();
		}
//    	else if (r.equalsIgnoreCase("Elk")) {
//    		reasonerFactory = new ElkReasonerFactory();
//    	}
//    	else if (r.equalsIgnoreCase("Jcel")) {
//    		reasonerFactory = new JcelReasonerFactory();
//    	}
		return reasonerFactory;
	}

	public void saveViewButtonAction(ActionEvent arg0) {
		// TODO Auto-generated method stub
		VisGraph graph = artPanel.getVisGraph();
		if (graph!= null){
			FileChooser selector = new FileChooser() ;
			File file = selector.showSaveDialog(primaryStage);

			if (file != null) {
				String path = null;
				try {
					path = file.getCanonicalFile().toString();
					VisPositionConfig.saveState(path, graph);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void restoreViewButtonAction(ActionEvent arg0) {
		// TODO Auto-generated method stub
		if (artPanel.getVisGraph()!= null){
			FileChooser selector = new FileChooser();
			File file = selector.showOpenDialog(primaryStage);
			if (file != null) {
				String path = null;
				try {
					path = file.getCanonicalFile().toString();
				} catch (IOException e) {
					e.printStackTrace();
				}
				VisPositionConfig.restoreState(path, artPanel.getVisGraph());
			}
		}
	}
	public void createImageButtonAction(ActionEvent e) {
		createImage(artPanel);
	}

	public void createImage(Canvas panel) {
		int w = (int) panel.getWidth();
		int h = (int) panel.getHeight();
		imageDialog(panel, w, h);

	}

	public void createImageFromVisibleRect(Canvas panel){
		int w = (int) panel.getBoundsInLocal().getWidth();
		int h = (int) panel.getBoundsInLocal().getHeight();

		// <CBL 25/9/13>
		// we have to check the position of the scroll bars

		int xIni = (int) (scroll.getHvalue() * (panel.getWidth() - scroll.getViewportBounds().getWidth()));
		int yIni = (int) (scroll.getVvalue() * (panel.getHeight() - scroll.getViewportBounds().getHeight()));

		System.out.println(w + " "+h+" "+xIni+" "+yIni);

		imageDialog(panel, w, h, xIni, yIni);
	}

	// <CBL 25/9/13>
	// wrapper for the method
	private void imageDialog(Canvas panel,int w, int h){
		imageDialog(panel, w, h, 0, 0);
	}

	// <CBL 25/9/13>
	// modified to take an offset as argument

	private void imageDialog(Canvas panel,int w, int h, int xIni, int yIni){
		File fl = null;
		boolean done = false;
		int answer;
		//BufferedImage bi = new BufferedImage(w+xIni,h+yIni, BufferedImage.TYPE_INT_RGB);
		ArrayList<String> valid = new ArrayList<String>();
		valid.add("jpg");
		valid.add("png");

		WritableImage writableImage = new WritableImage(w + xIni, h + yIni);
		SnapshotParameters params = new SnapshotParameters();
		params.setViewport(new Rectangle2D(xIni, yIni, w, h));
		panel.snapshot(params, writableImage);

		FileChooser selector = new FileChooser();
		boolean hasSuffix= true;
		FileChooser.ExtensionFilter jpgFilter = new FileChooser.ExtensionFilter("JPG files (*.jpg)", "*.jpg");
		FileChooser.ExtensionFilter pngFilter = new FileChooser.ExtensionFilter("PNG files (*.png)", "*.png");
		selector.getExtensionFilters().addAll(jpgFilter, pngFilter);
		while (!done) {
			File file = selector.showSaveDialog(primaryStage);

			if (file != null) {
				fl = file;

				if (fl.exists()){
					Alert alert = new Alert(AlertType.CONFIRMATION, "Overwrite?", ButtonType.OK, ButtonType.CANCEL);
					Optional<ButtonType> result = alert.showAndWait();
					if (result.isPresent() && result.get() == ButtonType.CANCEL) {
						continue;
					}
				}
				try {
					String extension = getExtension(selector.getSelectedExtensionFilter().getDescription());
					hasSuffix = hasValidSuffix(fl.getName().toLowerCase());
					if (!hasSuffix){
						String newName = fl.getPath() + "." + extension;
						boolean exists = (new File(newName).exists());
						File fl2 = new File(newName);
						if (exists){
							Alert alert = new Alert(AlertType.CONFIRMATION, "Overwrite?", ButtonType.OK, ButtonType.CANCEL);
							Optional<ButtonType> result = alert.showAndWait();
							if (result.isPresent() && result.get() == ButtonType.CANCEL) {
								continue;
							}
						}
						ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), extension, fl2);					}
					else {
						ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), extension, fl);
					}
					done = true;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			else {
				break;
			}
		}
	}

	public boolean hasValidSuffix(String in){
		if (in.endsWith("jpg")) return true;
		if (in.endsWith("png")) return true;
		return false;
	}

	private String getExtension(String description) {
		if (description.contains("jpg")) {
			return "jpg";
		} else if (description.contains("png")) {
			return "png";
		}
		return "jpg";
	}

	@Override
	public void loadSearchCombo() {
		// TODO Auto-generated method stub
		nTopPanel.loadSearchCombo();
	}

}
 
    

