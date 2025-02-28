package sid.OntView2.main;

import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import javafx.scene.control.Alert.AlertType;

import openllet.owlapi.OpenlletReasonerFactory;
import org.semanticweb.HermiT.ReasonerFactory;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.ConsoleProgressMonitor;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

import sid.OntView2.common.*;
import sid.OntView2.expressionNaming.SIDClassExpressionNamer;
import sid.OntView2.utils.ExpressionManager;

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

	public Stage getPrimaryStage() { return primaryStage; }

	public ScrollPane getScrollPane() { return scroll; }

	public static void createAndShowGUI(Stage primaryStage) {
		primaryStage.setTitle("Viewer");
		primaryStage.setMinWidth(1400);
		primaryStage.setOnCloseRequest(event -> System.exit(0));
		ClassLoader c = Thread.currentThread().getContextClassLoader(); 
		primaryStage.getIcons().add(new Image(Objects.requireNonNull(c.getResource("icon.png")).toExternalForm()));

		Mine viewer = new Mine();
		viewer.self = viewer;
		viewer.primaryStage = primaryStage;

		viewer.entityNameSet = new HashSet<>();
		viewer.artPanel = new PaintFrame();
		viewer.artPanel.setParentFrame(viewer);

		viewer.nTopPanel = new TopPanel(viewer);

		viewer.scroll = new ScrollPane(viewer.artPanel);
		viewer.artPanel.scroll = viewer.scroll;

		VBox root = new VBox();
		root.getChildren().addAll(viewer.nTopPanel.getMainPane(), viewer.scroll);
		VBox.setVgrow(viewer.scroll, Priority.ALWAYS);

		viewer.artPanel.setStyle("-fx-background-color: white;");
		viewer.nTopPanel.setStyle("-fx-border-color: black; -fx-border-width: 1;");


		Scene scene = new Scene(root, 1500, 600);
		scene.getStylesheets().add(Objects.requireNonNull(c.getResource("styles.css")).toExternalForm());
		viewer.primaryStage.setScene(scene);
		viewer.primaryStage.setMaximized(true);
		viewer.primaryStage.show();

	}

	/* Rest of methods */
	protected void createButtonAction() {
		if (reasoner!= null) {
			artPanel.setCursor(Cursor.WAIT);
			//cant cast to set<OWLClassExpression> from set<OWLClass>
			HashSet<OWLClassExpression> set = new HashSet<>(reasoner.getTopClassNode().getEntitiesMinusBottom());

			try {
				//set reasoner and ontology before creating
				artPanel.createReasonedGraph(set,check);
				artPanel.setCursor(Cursor.DEFAULT);
				artPanel.cleanConnectors();
			}
			catch (Exception e1) {
				e1.printStackTrace();
				artPanel.setCursor(Cursor.DEFAULT);
				throw new RuntimeException(e1);
			}

			while (!artPanel.isStable()){
				try {
					Thread.sleep(2000);
					System.err.println("wait");
				}
				catch (InterruptedException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
			nTopPanel.restorePropertyCheckboxes();
			nTopPanel.restoreSliderValue();
			artPanel.start();
		}
	}

	protected void loadActiveOntologyTask(IRI source) {
		manager = OWLManager.createOWLOntologyManager();
		artPanel.setCursor(Cursor.WAIT);

		Task<Void> task = new Task<>() {
			@Override
			protected Void call() {
				loadActiveOntology(source);
				return null;
			}
		};

		Stage loadingStage = artPanel.showLoadingStage(task);
		task.setOnSucceeded(e -> loadingStage.close());
		task.setOnFailed(e -> {
			task.getException().printStackTrace();
			loadingStage.close();
			//showAlertDialog("Error", "Ontology could not be loaded.", "The ontology might be " +
			//		"damaged or the URI may be incorrect.", Alert.AlertType.ERROR);
			showAlertDialog("Error", "Ontology could not be loaded.", task.getException().getMessage(),
					Alert.AlertType.ERROR);
		});

		new Thread(task).start();
	}

	protected void loadActiveOntology(IRI source) {
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
			throw new RuntimeException(e);
		}
		artPanel.setCursor(Cursor.DEFAULT);
		artPanel.setOntology(activeOntology);

		artPanel.setActiveOntologySource(source.toString()); //this might FAIL

		// CBL expression manager
		if (activeOntology != null && manager != null) {
			ExpressionManager.setNamespaceManager(manager, activeOntology);

			for (String ns: ExpressionManager.getNamespaceManager().getNamespaces()) {
				System.out.println("prefix: "+ExpressionManager.getNamespaceManager().getPrefixForNamespace(ns));
				System.out.println("  ns: "+ns);
			}
		}

		nTopPanel.getLoadReasonerButton().setDisable(false);
		nTopPanel.getReasonerCombo().setDisable(false);
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

	protected boolean loadReasoner(String reasonerString){
		if (activeOntology!=null) {
			ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
			// Specify the progress monitor via a configuration.  We could also specify other setup parameters in
			// the configuration, and different reasoners may accept their own defined parameters this way.
			OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor);

			try {
				// Create a reasoner that will reason over our ontology and its imports closure.  Pass in the configuration.
				reasoner = getReasonerFactory(reasonerString).createReasoner(activeOntology, config);

				// between creating and precomputing
				if (VisConfig.APPLY_RENAMING_DEBUG_PURPOSES) {
					applyRenaming();
				}
				
				reasoner.precomputeInferences();
				artPanel.setReasoner(reasoner);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				Platform.runLater(() -> showAlertDialog("Error", "Failed to load reasoner.",
						e.getMessage(), AlertType.ERROR));
				return false;
			}
		} else {
			artPanel.stop();
			Platform.runLater(() -> showAlertDialog("Error", "No active ontology.",
				"Please load an ontology before loading a reasoner.", AlertType.ERROR));

			return false;
		}
	}

	public void applyRenaming(){
		SIDClassExpressionNamer renamer = new SIDClassExpressionNamer(activeOntology, reasoner);
		renamer.applyNaming(true);
	}

	private OWLReasonerFactory getReasonerFactory(String r){
		OWLReasonerFactory reasonerFactory = null;
		if (r.equalsIgnoreCase("Openllet")) {
			reasonerFactory = new OpenlletReasonerFactory();
		}
		else if (r.equalsIgnoreCase("HermiT")){
			reasonerFactory = new ReasonerFactory();
		}
//		else if (r.equalsIgnoreCase("JFact")) {
//			reasonerFactory = new JFactFactory();
//		}
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
				String path;
				try {
					path = file.getCanonicalFile().toString();
					if(!path.endsWith(".xml"))
						path += ".xml";
					VisPositionConfig.saveState(path, graph);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void restoreViewTask(ActionEvent arg0, String[] info) {
		// load ontology and reasoner
		nTopPanel.getOntologyCombo().setValue(info[0]);
		nTopPanel.getReasonerCombo().setValue(info[1]);

		Task<Void> task = new Task<>() {
			@Override
			protected Void call() {
				loadActiveOntology(IRI.create(info[0]));
				nTopPanel.loadReasonerButtonActionActionPerformed(arg0);
				VisPositionConfig.restoreState(artPanel.getVisGraph());
				VisLevel.adjustWidthAndPos(artPanel.getVisGraph().getLevelSet());
				return null;
			}
		};

		Stage loadingStage = artPanel.showLoadingStage(task);

		task.setOnSucceeded(e -> {
			loadingStage.close();
			Platform.runLater(artPanel.getDrawerRunnable());
		});
		task.setOnFailed(e -> {
			task.getException().printStackTrace();
			loadingStage.close();
			artPanel.clearCanvas();
			String message = """
				  • Ontology loading ✔
				  • Reasoner loading ✔
				  • View loading ❌
				""";
			showAlertDialog("Error", "Failed to load view.", message +
					"\nView might be corrupted.", Alert.AlertType.ERROR);

		});
		new Thread(task).start();
	}

	public void restoreViewButtonAction(ActionEvent arg0) {
		// TODO Auto-generated method stub
		FileChooser selector = new FileChooser();
		selector.setInitialDirectory(new File(System.getProperty("user.dir")));
		File file = selector.showOpenDialog(primaryStage);
		if (file != null) {
			String path = null;
			try {
				path = file.getCanonicalFile().toString();
			} catch (IOException e) {
				e.printStackTrace();
			}

			String[] info = VisPositionConfig.restoreOntologyReasoner(path);
			restoreViewTask(arg0, info);
		}
	}

	public void createImage(Canvas canvas) {
		int w = (int) canvas.getWidth();
		int h = (int) canvas.getHeight();
		imageDialog(canvas, w, h);

	}

	public void createImageFromVisibleRect(Canvas canvas) {
		int w = (int) scroll.getWidth();
		int h = (int) scroll.getHeight();

		double xIni = scroll.getHvalue() * (canvas.getWidth() - w);
		double yIni = scroll.getVvalue() * (canvas.getHeight() - h);

		System.out.println(w + " " + h + " " + xIni + " " + yIni);

		imageDialog(canvas, w, h, (int) xIni, (int) yIni);
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
		ArrayList<String> valid = new ArrayList<>();
		valid.add("png");

		WritableImage writableImage = new WritableImage(w, h);
		SnapshotParameters params = new SnapshotParameters();
		params.setViewport(new Rectangle2D(xIni, yIni, w, h));
		panel.snapshot(params, writableImage);

		FileChooser fileChooser = new FileChooser();
		fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));

		while (!done) {
			File file = fileChooser.showSaveDialog(primaryStage);

			if (file != null) {
				fl = file;

				if (fl.exists()){
					Alert alert = new Alert(AlertType.CONFIRMATION, "Overwrite?", ButtonType.OK, ButtonType.CANCEL);
					Optional<ButtonType> result = alert.showAndWait();
					if (result.isPresent() && result.get() == ButtonType.CANCEL) {
						continue;
					}
				}
				String extension = valid.contains(fileChooser.getSelectedExtensionFilter().getDescription().toLowerCase()) ?
						fileChooser.getSelectedExtensionFilter().getDescription().toLowerCase() : "png";
				boolean hasSuffix = hasValidSuffix(fl.getName().toLowerCase());

				if (!hasSuffix){
					String newName = fl.getPath() + "." + extension;

					File fl2 = new File(newName);
					if (fl2.exists()) {
						Alert alert = new Alert(AlertType.CONFIRMATION, "Overwrite?", ButtonType.OK, ButtonType.CANCEL);
						Optional<ButtonType> result = alert.showAndWait();
						if (result.isPresent() && result.get() == ButtonType.CANCEL) {
							continue;
						}
					}
					saveViewTask(writableImage, extension, fl2);
				}
				else {
                    saveViewTask(writableImage, extension, fl);
				}
				done = true;
			}
			else {
				break;
			}
		}
	}

	private void saveViewTask(WritableImage writableImage, String extension, File finalFl) {
		Task<Void> task = new Task<>() {
			@Override
			protected Void call() throws IOException {
				ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), extension, finalFl);
				return null;
			}
		};
		Stage loadingStage = artPanel.showLoadingStage(task);
		task.setOnSucceeded(e -> loadingStage.close());
		task.setOnFailed(e -> {
			task.getException().printStackTrace();
			loadingStage.close();
			showAlertDialog("Error", "Unable to save the ontology view.", task.getException().getMessage(),
					AlertType.ERROR);
		});
		new Thread(task).start();
	}

	public boolean hasValidSuffix(String in){
		if (in.endsWith("jpg")) return true;
		if (in.endsWith("png")) return true;
		return false;
	}

	@Override
	public void loadSearchCombo() {
		// TODO Auto-generated method stub
		nTopPanel.loadSearchCombo();
	}

	void showAlertDialog(String title, String header, String content, AlertType type) {
		Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(content);
		ClassLoader c = Thread.currentThread().getContextClassLoader();
		alert.getDialogPane().getStylesheets().add(Objects.requireNonNull(c.getResource("styles.css")).toExternalForm());
		alert.getDialogPane().getStyleClass().add("custom-alert");
		alert.showAndWait();
	}


}
 
    

