package sid.OntView2.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

import javax.imageio.ImageIO;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.*;
import javafx.scene.control.Alert.AlertType;

import openllet.owlapi.OpenlletReasonerFactory;
import org.semanticweb.HermiT.ReasonerFactory;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.ConsoleProgressMonitor;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import sid.OntView2.common.*;
import sid.OntView2.expressionNaming.SIDClassExpressionNamer;
import sid.OntView2.utils.ErrorHandler;
import sid.OntView2.utils.ExpressionManager;
import sid.OntView2.utils.ImageMerger;

public class Mine extends Application implements Embedable{
    private static final Logger logger = LogManager.getLogger(Mine.class);
	private Stage primaryStage;
	private static final long serialVersionUID = 1L;

    OWLOntology activeOntology;
	OWLReasoner reasoner;
	OWLOntologyManager manager;
	HashSet<String> entityNameSet;

	PaintFrame   artPanel;
	TopPanel     nTopPanel;
	ScrollPane  scroll;
	Mine         self= this;
	boolean      check = true;
    public static boolean cancelledFlag = false;
    private static final double SCROLL_INCREMENT = 10000;
    private static final double SPEED = 100;


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

    public static void createAndShowGUI(Stage primaryStage) {
		primaryStage.setTitle("OntView");
		primaryStage.setMinWidth(1700);
		primaryStage.setOnCloseRequest(event -> System.exit(0));
		ClassLoader c = Thread.currentThread().getContextClassLoader(); 
		primaryStage.getIcons().add(new Image(Objects.requireNonNull(c.getResource("icon.png")).toExternalForm()));

		Rectangle2D screenBounds = Screen.getPrimary().getBounds();

		Mine viewer = new Mine();
		viewer.self = viewer;
		viewer.primaryStage = primaryStage;

		double screenWidth = screenBounds.getWidth();
		double screenHeight = screenBounds.getHeight();

		viewer.entityNameSet = new HashSet<>();
		viewer.artPanel = new PaintFrame(screenWidth, screenHeight);
        viewer.artPanel.setCache(false); // buffer overflow
		viewer.artPanel.setParentFrame(viewer);
		viewer.artPanel.screenWidth = (int) screenWidth;
		viewer.artPanel.screenHeight = (int) screenHeight;

		viewer.nTopPanel = new TopPanel(viewer);
        viewer.artPanel.nTopPanelHeight = (int) viewer.nTopPanel.getHeight();
        viewer.scroll = new ScrollPane(viewer.artPanel);
		viewer.scroll.setPrefSize(screenWidth, screenHeight - viewer.nTopPanel.getHeight());
		viewer.scroll.setHmax(screenWidth);
		viewer.scroll.setVmax(screenHeight - viewer.nTopPanel.getHeight());
        viewer.scroll.setFitToWidth(false);
        viewer.scroll.setFitToHeight(false);
		viewer.artPanel.scroll = viewer.scroll;
        viewer.artPanel.nTopPanel = viewer.nTopPanel;

		viewer.artPanel.scroll.hvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() < 0.0) newVal = 0;
            viewer.artPanel.setOffsetX(newVal.doubleValue());
        });
		viewer.artPanel.scroll.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() < 0.0) newVal = 0;
            viewer.artPanel.setOffsetY(newVal.doubleValue());
        });

        viewer.artPanel.scroll.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                double newZoom = viewer.getNewZoom(event);
                double oldZoom = viewer.artPanel.getFactor();

                double mouseX = event.getX();
                double mouseY = event.getY();

                double worldX = viewer.artPanel.offsetX + mouseX / oldZoom;
                double worldY = viewer.artPanel.offsetY + mouseY / oldZoom;

                viewer.artPanel.setFactorValue(newZoom);
                viewer.artPanel.setOffsetX(worldX - mouseX / newZoom);
                viewer.artPanel.setOffsetY(worldY - mouseY / newZoom);

                viewer.artPanel.nTopPanel.ignoreSliderListener = true;
                viewer.artPanel.nTopPanel.getZoomSlider().setValue(newZoom);
            } else {
                if (event.getDeltaY() != 0) {
                    double delta = event.getDeltaY() > 0 ? SCROLL_INCREMENT : -SCROLL_INCREMENT;
                    viewer.artPanel.scroll.setVvalue(viewer.artPanel.scroll.getVvalue() - delta / SPEED);
                }
                if (event.getDeltaX() != 0) {
                    double delta = event.getDeltaX() > 0 ? SCROLL_INCREMENT : -SCROLL_INCREMENT;
                    viewer.artPanel.scroll.setHvalue(viewer.artPanel.scroll.getHvalue() - delta / SPEED);
                }
            }
            event.consume();
        });

        viewer.artPanel.prohibitedImage = new Image(Objects.requireNonNull(c.getResource("assets/prohibited.jpg")).toExternalForm());

		VBox root = new VBox();
		root.getChildren().addAll(viewer.nTopPanel.getMainPane(), viewer.scroll);

        viewer.artPanel.setStyle("-fx-background-color: white;");
		viewer.nTopPanel.setStyle("-fx-border-color: black; -fx-border-width: 1;");

		Scene scene = new Scene(root, 1700, 600);
		scene.getStylesheets().add(Objects.requireNonNull(c.getResource("styles.css")).toExternalForm());
		viewer.primaryStage.setScene(scene);
		viewer.primaryStage.setMaximized(true);
		viewer.primaryStage.show();

        viewer.calculateStageTopBar(viewer.primaryStage, root);
	}

    private void calculateStageTopBar(Stage primaryStage, VBox root) {
        Platform.runLater(() -> {
            double stageScreenY = primaryStage.getY();

            Point2D rootScreenPos = root.localToScreen(0, 0);
            double rootScreenY = rootScreenPos.getY();

            VisConstants.WINDOW_TITLE_BAR = (int) Math.round(rootScreenY - stageScreenY);
        });
    }

    private double getNewZoom(ScrollEvent event) {
        double sensitivity = 0.001;
        double zoomFactor = Math.exp(event.getDeltaY() * sensitivity);
        double currentZoom = artPanel.getFactor();
        double newZoom = currentZoom * zoomFactor;
        newZoom = Math.max(nTopPanel.getZoomSlider().getMin(), Math.min(newZoom, nTopPanel.getZoomSlider().getMax()));
        return newZoom;
    }

	/* Rest of methods */
	protected void createButtonAction() {
		if (reasoner!= null) {
			artPanel.setCursor(Cursor.WAIT);
			//cant cast to set<OWLClassExpression> from set<OWLClass>
            HashSet<OWLClassExpression> set;
            Future<HashSet<OWLClassExpression>> loaderFuture;
            ExecutorService exec = Executors.newSingleThreadExecutor();
            loaderFuture = exec.submit(() -> new HashSet<>(reasoner
                .getTopClassNode()
                .getEntitiesMinusBottom()));

			try {
                set = loaderFuture.get();
				//set reasoner and ontology before creating
				artPanel.createReasonedGraph(set,check);
				artPanel.setCursor(Cursor.DEFAULT);
				artPanel.cleanConnectors();
			}
            catch (CancellationException | InterruptedException e) {
                artPanel.clearCanvas();
                cancelledFlag = true;
                Platform.runLater(() -> artPanel.loadingStage.close());
                artPanel.setCursor(Cursor.DEFAULT);
                return;
            }
			catch (Exception e1) {
				e1.printStackTrace();
				artPanel.setCursor(Cursor.DEFAULT);
				throw new RuntimeException(e1);
			}

			while (!artPanel.isStable()){
				try {
					Thread.sleep(2000);
                    logger.error("wait");
				}
				catch (InterruptedException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		}
	}

	protected void loadActiveOntologyTask(IRI source) {
		manager = OWLManager.createOWLOntologyManager();
		artPanel.setCursor(Cursor.WAIT);

		Task<Void> task = new Task<>() {
			@Override
			protected Void call() {
				loadActiveOntology(source);
                if (isCancelled()) {
                    nTopPanel.getLoadReasonerButton().setDisable(true);
                    nTopPanel.getReasonerCombo().setDisable(true);
                    nTopPanel.getKceComboBox().setDisable(true);
                }
				return null;
			}
		};

		Stage loadingStage = artPanel.showLoadingStage(task);
        task.setOnCancelled(e -> loadingStage.close());
		task.setOnSucceeded(e -> loadingStage.close());
		task.setOnFailed(e -> {
			task.getException().printStackTrace();
			loadingStage.close();
            artPanel.showAlertDialog("Error", "Ontology could not be loaded.",
                ErrorHandler.getOntologyLoadError(task.getException()), Alert.AlertType.ERROR);
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
                logger.info("prefix: {}", ExpressionManager.getNamespaceManager().getPrefixForNamespace(ns));
                logger.info("  ns: {}", ns);
			}
		}

		nTopPanel.getLoadReasonerButton().setDisable(false);
		nTopPanel.getReasonerCombo().setDisable(false);
        nTopPanel.getKceComboBox().setDisable(false);
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
				// Create a reasoner that will reason over our ontology and its imports' closure.  Pass in the configuration.
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
				Platform.runLater(() -> artPanel.showAlertDialog("Error", "Failed to load reasoner.",
                    ErrorHandler.getReasonerError(e), AlertType.ERROR));
				return false;
			}
		} else {
			Platform.runLater(() -> artPanel.showAlertDialog("Error", "No active ontology.",
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
                    String finalPath = path;
                    artPanel.showAlertDialog("Success", "View saved successfully.",
                        "View has been saved to: " + finalPath, AlertType.INFORMATION);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

    private void selectOptionsFromRestoreView(ActionEvent arg0, String[] info) {
        nTopPanel.getPropertiesCheckBox().setSelected(Boolean.parseBoolean(info[2]));
        nTopPanel.getRenderLabel().setSelected(Boolean.parseBoolean(info[3]));
        nTopPanel.getQualifiedNames().setSelected(Boolean.parseBoolean(info[4]));

        nTopPanel.applyCheckBoxFunctions(arg0);
    }

    private void resetOptions() {
        nTopPanel.getPropertiesCheckBox().setSelected(false);
        nTopPanel.getRenderLabel().setSelected(false);
        nTopPanel.getQualifiedNames().setSelected(false);
    }

	public void restoreViewTask(ActionEvent arg0, String[] info, String path) {
		// load ontology and reasoner
		nTopPanel.getOntologyCombo().setValue(info[0]);
		nTopPanel.getReasonerCombo().setValue(info[1]);

        resetOptions();

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

		artPanel.loadingStage = artPanel.showLoadingStage(task);
        task.setOnCancelled(e -> {
            if (!cancelledFlag) VisGraph.voluntaryCancel(true);
            cancelledFlag = false;
        });
		task.setOnSucceeded(e -> {
            artPanel.loadingStage.close();
            selectOptionsFromRestoreView(arg0, info);
			Platform.runLater(artPanel.getRedrawRunnable());
            artPanel.showAlertDialog("Success", "View restored successfully.",
                "View has been restored from: " + path, AlertType.INFORMATION);
		});
		task.setOnFailed(e -> {
			task.getException().printStackTrace();
            artPanel.loadingStage.close();
			artPanel.clearCanvas();
			Platform.runLater(artPanel.getRedrawRunnable());
            String message = """
				  • Ontology loading \u2714
				  • Reasoner loading \u2714
				  • View loading \u274C
				""";
			artPanel.showAlertDialog("Error", "Failed to load view.", message +
					"\nView might be corrupted.", Alert.AlertType.ERROR);

		});
		new Thread(task).start();
	}

	public void restoreViewButtonAction(ActionEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		FileChooser selector = new FileChooser();
		selector.setInitialDirectory(new File(System.getProperty("user.dir")));

        FileChooser.ExtensionFilter xmlFilter = new FileChooser.ExtensionFilter(".xml", "*.xml");
        selector.getExtensionFilters().add(xmlFilter);
        selector.setSelectedExtensionFilter(xmlFilter);

		File file = selector.showOpenDialog(primaryStage);
		if (file != null) {
			String path = null;
			try {
				path = file.getCanonicalFile().toString();
			} catch (IOException e) {
				e.printStackTrace();
			}

			String[] info;
            try {
                info = VisPositionConfig.restoreOntologyReasoner(path);
            } catch (Exception e) {
                artPanel.showAlertDialog("Error", "Failed to load view.",
                    ErrorHandler.getXMLeError(e), Alert.AlertType.ERROR);
                return;
            }
			restoreViewTask(arg0, info, path);
		}
	}

	public void createImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG files", "*.png"));
        File fl = fileChooser.showSaveDialog(primaryStage);
        if (fl == null) return;

        boolean hasSuffix = fl.getName().toLowerCase().endsWith("png");
        if (!hasSuffix)  fl = new File(fl.getAbsolutePath() + ".png");

        File finalFl = fl;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call(){
                exportLargeCanvasAsPNGs(finalFl);
                return null;
            }
        };
        Stage loadingStage = artPanel.showLoadingStage(task);
        task.setOnSucceeded(e -> {
            loadingStage.close();
            artPanel.showAlertDialog("Success", "Image created successfully.",
                "The image have been created at: " + finalFl.getAbsolutePath(), AlertType.INFORMATION);
        });
        task.setOnFailed(e -> {
            task.getException().printStackTrace();
            Path path = Paths.get("src/canvasImages/");
            ImageMerger.deleteDirectory(path.toFile());
            loadingStage.close();
            artPanel.showAlertDialog("Error", "Unable to save the ontology view.",
                ErrorHandler.getGraphSaveError(task.getException()), AlertType.ERROR);
        });
        task.setOnCancelled(e -> {
            loadingStage.close();
            Path path = Paths.get("src/canvasImages/");
            ImageMerger.deleteDirectory(path.toFile());
            cancelledFlag = false;
        });
        new Thread(task).start();
	}

	public void createImageFromVisibleRect() {
		int w = (int) artPanel.getWidth();
		int h = (int) artPanel.getHeight();

		imageDialog(artPanel, w, h);
	}

    private void imageDialog(PaintFrame panel, int w, int h) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        File fl = fileChooser.showSaveDialog(primaryStage);
        if (fl != null) {
            boolean hasSuffix = fl.getName().toLowerCase().endsWith("png");
            if (!hasSuffix)  fl = new File(fl.getAbsolutePath() + ".png");

            WritableImage snapshot = new WritableImage(w, h);
            SnapshotParameters parameters = new SnapshotParameters();
            parameters.setViewport(new Rectangle2D(0, 0, w, h));

            panel.snapshot(parameters, snapshot);
            saveViewTask(snapshot, fl);
        }
    }
    public void exportLargeCanvasAsPNGs(File directory) {
        final int CHUNK_SIZE = 8000;
        int columns = (int) Math.ceil((double) artPanel.canvasWidth / CHUNK_SIZE);
        int rows = (int) Math.ceil((double) artPanel.canvasHeight / CHUNK_SIZE);

        Path tempDir;
        try {
            Path projectDir = Paths.get("src").toAbsolutePath();
            tempDir = projectDir.resolve("canvasImages");
            if (Files.exists(tempDir)) {
                ImageMerger.deleteDirectory(tempDir.toFile());
            }
            Files.createDirectories(tempDir);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                checkInterrupted();
                int x = col * CHUNK_SIZE;
                int y = row * CHUNK_SIZE;
                int width = Math.min(CHUNK_SIZE, artPanel.canvasWidth - x);
                int height = Math.min(CHUNK_SIZE, artPanel.canvasHeight - y);

                WritableImage snapshot = new WritableImage(width, height);
                // Draw on the image without affecting the screen
                GraphicsContext tempGc = new Canvas(width, height).getGraphicsContext2D();
                tempGc.save();
                tempGc.translate(-x, -y);
                tempGc.setFill(Color.WHITE);
                tempGc.fillRect(0, 0, width, height);
                artPanel.draw(tempGc);
                tempGc.restore();

                CountDownLatch latch = new CountDownLatch(1);
                Platform.runLater(() -> {
                    tempGc.getCanvas().snapshot(null, snapshot);
                    latch.countDown();
                });

                try {
                    latch.await();
                } catch (InterruptedException e) {
                    cancelledFlag = true;
                    return;
                }

                File outputFile = tempDir.resolve("i_" + row + "_" + col + ".png").toFile();
                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", outputFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        logger.debug("Done creating images in {}", tempDir.toAbsolutePath());
        checkInterrupted();
        ImageMerger.mergeImages("src/canvasImages/", directory.toString());
    }


    private void saveViewTask(WritableImage writableImage, File finalFl) {
		Task<Void> task = new Task<>() {
			@Override
			protected Void call() throws IOException {
				ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", finalFl);
				return null;
			}
		};
		Stage loadingStage = artPanel.showLoadingStage(task);
		task.setOnSucceeded(e -> {
            loadingStage.close();
            artPanel.showAlertDialog("Success", "Image created successfully.",
                "The image have been created at: " + finalFl.getAbsolutePath(), AlertType.INFORMATION);
        });
		task.setOnFailed(e -> {
			task.getException().printStackTrace();
			loadingStage.close();
			artPanel.showAlertDialog("Error", "Unable to save the ontology view.",
                ErrorHandler.getGraphSaveError(task.getException()), AlertType.ERROR);
		});
        task.setOnCancelled(e -> {
            loadingStage.close();
        });
		new Thread(task).start();
	}

    /**
     * Checks if the current thread is interrupted and throws a CancellationException if it is.
     */
    private void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            cancelledFlag = true;
            throw new CancellationException("Thread was interrupted");
        }
    }

    @Override
	public void loadSearchCombo() {
		// TODO Auto-generated method stub
		nTopPanel.loadSearchCombo();
	}
}
