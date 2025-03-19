package sid.OntView2.common;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.Cursor;

import javafx.scene.text.Font;
import javafx.scene.transform.Scale;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import reducer.StructuralReducer;

public class PaintFrame extends Canvas {
	private static final long serialVersionUID = 1L;
	public ScrollPane scroll;
	static final int BORDER_PANEL = 50;
	static final int MIN_SPACE = 20;
	static final int MIN_INITIAL_SPACE = 40;
	private final int MIN_POS_SHAPE = 30;
	private static final int DOWN = 0;
	private static final int UP = -1;
	boolean stable = false;
	boolean repulsion = true;
	public boolean renderLabel = false;

	// CBL: added the qualified names rendering
	public boolean qualifiedNames = false;
	private String kceOption = VisConstants.NONECOMBOOPTION;
	Dimension2D prevSize;
	PaintFrame paintFrame = this;
	OWLOntology activeOntology;
	private String activeOntologySource;
	OWLReasoner reasoner;
	VisGraph visGraph, rVisGraph; // visGraph will handle both depending on which is currently selected
	private VisShapeContext menuVisShapeContext = null;
	private VisGeneralContext menuVisGeneralContext = null;
	public int screenWidth;
	public int screenHeight;
	public int canvasWidth = 0;
	public int canvasHeight = 0;
    public int nTopPanelHeight = 0;

	public boolean isStable() {
		return stable;
	}

	public void setReasoner(OWLReasoner pReasoner) {
		reasoner = pReasoner;
	}

	public OWLReasoner getReasoner() {
		return reasoner;
	}

	public void setOntology(OWLOntology ac) {
		activeOntology = ac;
	}

	public OWLOntology getOntology() {
		return activeOntology;
	}

	public void setActiveOntologySource(String p) {
		activeOntologySource = p;
	}

	public String getActiveOntologySource() {
		return activeOntologySource;
	}

	public String getKceOption() {
		return kceOption;
	}

	public void setKceOption(String itemAt) {
		kceOption = itemAt;
	}
	private boolean showConnectors = false;
	public void setShowConnectors(boolean b) { showConnectors = b; }
	public boolean getShowConnectors() { return showConnectors; }

	public PaintFrame(double width, double height) {
		super();
		try {
			this.setWidth(width);
			this.setHeight(height);
			prevSize = new Dimension2D(getWidth(), getHeight());
			VisConfig.getInstance().setConstants();
			addEventHandlers();
			//scroll = new ScrollPane(this);
			visGraph = new VisGraph(this);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void addEventHandlers() {
		this.addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
		this.addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
		this.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
		this.addEventHandler(MouseEvent.MOUSE_MOVED, this::handleMouseMoved);
		this.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleMouseClicked);
		this.addEventHandler(MouseEvent.MOUSE_ENTERED, this::handleMouseEntered);
		this.addEventHandler(MouseEvent.MOUSE_EXITED, this::handleMouseExited);
	}

	public double offsetX = 0, offsetY = 0;

	public void setOffsetX(double offsetX) {
		this.offsetX = offsetX;
		redraw();
	}

	public void setOffsetY(double offsetY) {
		this.offsetY = offsetY;
		redraw();
	}

	private void redraw() {
		GraphicsContext gc = getGraphicsContext2D();
		if (gc != null && this.getScene() != null) {
			gc.clearRect(0, 0, getWidth(), getHeight());
			gc.save();
            gc.scale(factor, factor);
			gc.translate(-offsetX, -offsetY);
			draw(gc);
			gc.restore();
		}
	}

	private void redrawConnector(GraphicsContext gc, VisConnector c) {
		if (gc != null && this.getScene() != null) {
			gc.save();
			c.draw(gc);
			gc.restore();
		}
	}

	/*-*************************************************************
	 * Scaling  issues
	 *-*************************************************************/

	double factor = 1.0;
	double prevFactor = 1.0;
	Dimension2D oSize;

	/**
	 * sets scaling/zoom factor
	 */
	public void setFactor(double newFactor) {
        double oldZoom = this.factor;
        this.factor = newFactor;
        offsetX = offsetX * oldZoom / newFactor;
        offsetY = offsetY * oldZoom / newFactor;

        Platform.runLater(redrawRunnable);

    }

	public void setOriginalSize(Dimension2D in) { oSize = in; }
	
	/*** 
	 * Runnable required to push the drawing to the javaFx application thread using Platform.runLater()
	 */

	public class Relaxer implements Runnable {
		public void run() {
			relax();
		}
	}
	
	public class ConnectorDrawer implements Runnable {
		Shape s = null;
		public ConnectorDrawer (Shape s) {
			this.s = s;
		}
		public void run() {
			drawConnectorShape(s);
		}
	}
	
	public class GlobalRedraw implements Runnable {
		public void run() {
			redraw();
		}
	}
	
	public class Compacter implements Runnable {
		public void run() {
			compactGraph();
		}
	}
	public class CanvasAdjuster implements Runnable {
		public void run() {
			checkAndResizeCanvas();
		}
	}

	Relaxer relaxerRunnable = new Relaxer();
	GlobalRedraw redrawRunnable = new GlobalRedraw();
	CanvasAdjuster canvasAdjusterRunnable = new CanvasAdjuster();


	public Relaxer getRelaxerRunnable() {
		return relaxerRunnable; 
	}
	
	public GlobalRedraw getRedrawRunnable() {
		return redrawRunnable;
	}

	public CanvasAdjuster getCanvasAdjusterRunnable(){ return canvasAdjusterRunnable; }


	/*-*************************************************************/
	public void drawDisjointShape() {
		if (this.getScene() != null && !this.isDisabled() && this.isVisible() && this.getGraphicsContext2D() != null) {
			GraphicsContext g = this.getGraphicsContext2D();

			if (visGraph != null || g == null) {
				for (Shape shape : selectedDisjoints) {
					if (shape instanceof VisClass visClass) {
						for (VisConnectorDisjoint disjoint : visClass.getDisjointConnectors()) {
							disjoint.draw(g);
						}
					}
				}
			}
		}
	}

	public boolean hideDisjoint = true;
	public void drawAllDisjointShapes() {
		if (!hideDisjoint) {
			for (Entry<String, Shape> entry : visGraph.shapeMap.entrySet()) {
				Shape shape = entry.getValue();
				selectedDisjoints.add(shape);
			}
		} else {
			selectedDisjoints.clear();
		}
		Platform.runLater(redrawRunnable);
	}

	public void drawConnectorShape(Shape shape) {
		if (this.getScene() != null && !this.isDisabled() && this.isVisible() && this.getGraphicsContext2D() != null) {
			GraphicsContext gc = this.getGraphicsContext2D();
			if (visGraph != null && shape != null) {
				for (VisConnector c: shape.inConnectors) {
					redrawConnector(gc,c);
					//c.draw(gc);
				}
				for (VisConnector c: shape.outConnectors) {
					redrawConnector(gc,c);
					//c.draw(gc);
				}
				for (VisConnector c : shape.inDashedConnectors) {
					if (c.from.isVisible()) {
						redrawConnector(gc,c);
						//c.draw(gc);
					}
				}
				for (VisConnector c : shape.outDashedConnectors) {
					if (c.to.isVisible()) {
						redrawConnector(gc,c);
						//c.draw(gc);
					}
				}
			}
		}
	}
	public void draw(GraphicsContext g) {
		if (this.getScene() != null && !this.isDisabled() && this.isVisible() && g != null) {

			if (prevFactor != factor) {
				prevFactor = factor;
				if ((getWidth() != prevSize.getWidth() || getHeight() != prevSize.getHeight())) {
					prevSize = new Dimension2D(getWidth(), getHeight());
				}
			}

			if (visGraph != null) {

				// draw levels
				g.setStroke(Color.LIGHTGRAY);
				for (VisLevel lvl : visGraph.levelSet) {
					g.strokeLine(lvl.getXpos(), 0, lvl.getXpos(), canvasHeight);
					// Uncomment this to get a vertical line in every level
					g.setStroke(Color.LIGHTGRAY);
				}
				g.setStroke(Color.BLACK);

				// draw connectors
				if (showConnectors) {
					for (VisConnector c : visGraph.connectorList) {
						c.draw(g);
					}
					for (VisConnector c : visGraph.dashedConnectorList) {
						c.draw(g);
					}
				}

				if (!selectedShapes.isEmpty()){
					drawConnectorsForSelectedShapes();
				}

				if (!selectedDisjoints.isEmpty()){
					drawDisjointShape();
				}

				for (Entry<String, Shape> entry : visGraph.shapeMap.entrySet()) {
					Shape shape = entry.getValue();
					if (!(shape instanceof SuperNode)) {
						shape.drawShape(g);
					}
				}
				drawPropertyBoxes(g);
			} else {
				System.err.println("visGraph is null in draw method.");
			}
		}
	}

	private void drawConnectorsForSelectedShapes() {
		for (Shape shape : selectedShapes) {
			drawConnectorShape(shape);
		}
	}

	private void drawPropertyBoxes(GraphicsContext g2d) {
		if (!this.isDisabled() && this.isVisible() && g2d != null) {
			for (Entry<String, Shape> entry : visGraph.shapeMap.entrySet()) {
				if (entry.getValue() instanceof VisClass) {
					VisClass v = entry.getValue().asVisClass();
					if ((v.getPropertyBox() != null) && (v.getPropertyBox().visible)) {
						Font c = g2d.getFont();
						g2d.setStroke(Color.BLACK);
						v.getPropertyBox().draw(g2d);
						g2d.setFont(c);
					}
				}
			}
		}
	}

	public void createReasonedGraph(HashSet<OWLClassExpression> set, boolean check) {
		CountDownLatch latch = new CountDownLatch(1);

		if (visGraph != null) {
			visGraph.clearShapeMap();
		}

		rVisGraph = new VisGraph(this);

		visGraph = rVisGraph;
		visGraph.setLatch(latch);
		stable = false;
		visGraph.setActiveOntology(activeOntology);
//	   applyStructuralReduction();
		visGraph.setOWLClassExpressionSet(set);
		visGraph.setCheck(check);
		visGraph.setReasoner(reasoner);

		Task<Void> task = getVoidTask();

		new Thread(task).start();

		paintFrame.setCursor(Cursor.WAIT);

		VisGraphObserver graphObserver = new VisGraphObserver(this.getVisGraph());
		visGraph.addGeneralObserver((observable, oldValue, newValue) -> graphObserver.update());

		stable = true;
		stateChanged = true;
		factor = 1.0;
		paintFrame.setCursor(Cursor.DEFAULT);

		try {
			latch.await(); // Block until latch.countDown() is called in VisGraph
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	private Task<Void> getVoidTask() {
		Task<Void> task = new Task<>() {
			@Override
			protected Void call() {
				visGraph.run();
				return null;
			}
		};

		task.setOnSucceeded(e -> {
			paintFrame.setCursor(Cursor.DEFAULT);
		});

		task.setOnFailed(e -> {
			paintFrame.setCursor(Cursor.DEFAULT);
			task.getException().printStackTrace();
		});
		return task;
	}

	/**
	 * returns pressed shape
	 *
	 * @return Shape
	 */
	public Shape getPressedShape() {
		return pressedShape;
	}

	private boolean stateChanged = true;

	public void setStateChanged(boolean value) {
		stateChanged = value;
	}

	/**
	 * Updates node positions Avoids shapes being on top of each other updates y
	 * coordinate until there's no overlap
	 **/

	// Relax is already encapsulated in the relaxerRunnable so all the calls should already be done 
	// properly according to javaFX requirements
	public synchronized void relax() {
		if (visGraph == null) {
			System.err.println("visGraph is null in relax method.");
			return;
		}

		boolean recentChange = false;

		if (stable) {
			while (stateChanged) {
				System.out.println("relax");
				stateChanged = false;

				// Faster version
				for (VisLevel level: visGraph.levelSet) {
					for (Shape s_i: level.getShapeSet()) {
						for (Shape shape_j: level.getShapeSet()) {
							if (s_i.getVisLevel() == shape_j.getVisLevel()) {
								if ((s_i != shape_j) && (s_i.visible) && (shape_j.visible)) {
									if ((s_i.getTopCorner() < shape_j.getTopCorner()) &&
											(shape_j.getTopCorner() < (s_i.getBottomCorner() + MIN_SPACE )) ) {
										stateChanged = true;
										shapeRepulsion(s_i, DOWN);
									}
								}
							}
						}
						if (s_i.getPosY() < BORDER_PANEL) {
							s_i.setPosY(BORDER_PANEL + MIN_SPACE);
							stateChanged = true;
							shapeRepulsion(s_i, DOWN);
						}
						visGraph.adjustPanelSize((float) factor);
						recentChange = true;
					}
				}
			}
			if (recentChange) {
				redraw();
			}
		}
	}

	/**
	 *
	 * MOUSE-LISTENER METHODS
	 *
	 **/

	// to keep track of the increment when moving the shapes

	int mouseLastY = 0;
	int mouseLastX = 0;
	Shape pressedShape = null;
	List<Shape> selectedShapes = new ArrayList<>();
	List<Shape> selectedDisjoints = new ArrayList<>();
	Cursor cursorState = Cursor.DEFAULT;
	public boolean hideRange = false;
	private Embedable parentFrame;
	private final Tooltip tooltip = new Tooltip();
	private final WebView web = new WebView();
	private final WebEngine webEngine = web.getEngine();
    private Point2D pinchPoint = null;


    public void cleanConnectors() {
		selectedShapes.clear();
	}

	public void handleMouseEntered(MouseEvent e) {
		if (visGraph == null) {
			return;
		}
		setCursor(Cursor.DEFAULT);
	}

	public void handleMouseExited(MouseEvent e) {
		if (visGraph == null) {
			return;
		}
		setCursor(Cursor.DEFAULT);
	}

	private void handleMousePressed(MouseEvent e) {
		if (visGraph == null) {
			return;
		}
		Point2D p = translatePoint(new Point2D(e.getX(), e.getY()));
		if (isInsidePropertyBox((int) p.getX(), (int) p.getY())) {
			pressedShape = null;
			return;
		}

		pressedShape = visGraph.findShape(p);

		if (pressedShape != null) {
            //System.out.println("********* PRESSED SHAPE " + pressedShape.getLabel() + " y: " + pressedShape.getPosY() + "*********");
			selectedShapes.add(pressedShape);
			mouseLastY = (int) p.getY();
		} else {
            //System.out.println("********* PRESSED ELSEWHERE *********");
            pinchPoint = new Point2D(e.getX() + offsetX, e.getY() + offsetY);
            //System.out.println("pinchPoint x:" + pinchPoint.getX() + " , y:" + pinchPoint.getY());
            mouseLastX = (int) p.getX();
			mouseLastY = (int) p.getY();
			setCursor(Cursor.CLOSED_HAND);
			Platform.runLater(redrawRunnable);
            //Platform.runLater(new ConnectorDrawer(pressedShape));
		}
	}

	public void handleMouseReleased(MouseEvent e) {
		if (visGraph == null) {
			return;
		}

        selectedShapes.remove(pressedShape);
        pinchPoint = null;
		pressedShape = null;
		repulsion = true;
		mouseLastY = 0;
		mouseLastX = 0;
		setCursor(Cursor.DEFAULT);

        Platform.runLater(canvasAdjusterRunnable);
        Platform.runLater(redrawRunnable);
	}

	/*
	 * MOUSEMOTIONLISTENER
	 */

	private boolean isDragging = false;

	public void handleMouseDragged(MouseEvent e) {
		if (visGraph == null) {
			return;
		}
		isDragging = true;

		int draggedY;
		int direction;
		repulsion = (e.getButton() != MouseButton.SECONDARY);
		Point2D p = translatePoint(new Point2D(e.getX(), e.getY()));
		if ((mouseLastX == 0) && (mouseLastY == 0)) {
			draggedY = 0;
		} else {
			draggedY = (int) p.getY() - mouseLastY;
		}
		if (pressedShape != null) {
			direction = ((draggedY > 0) ? DOWN : UP);
			pressedShape.setPosY(pressedShape.getPosY() + draggedY);
			stateChanged = true;
			shapeRepulsion(pressedShape, direction);
			mouseLastX = (int) p.getX();
			mouseLastY = (int) p.getY();

			Platform.runLater(redrawRunnable);
		} else {
            double newOffsetX = pinchPoint.getX() - e.getX();
            double newOffsetY = pinchPoint.getY() - e.getY();

            newOffsetX = Math.max(0, Math.min(newOffsetX, scroll.getHmax()));
            newOffsetY = Math.max(0, Math.min(newOffsetY, scroll.getVmax()));

            scroll.setHvalue(newOffsetX);
            scroll.setVvalue(newOffsetY);
        }
	}

	public void handleMouseMoved(MouseEvent e) {
		if (visGraph == null) {
			return;
		}
		Point2D p = translatePoint(new Point2D(e.getX(), e.getY()));
		int x = (int) p.getX();
		int y = (int) p.getY();
		VisObjectProperty prop = null;
		Shape shape = visGraph.findShape(p);

		if (shape != null) {
			configurationTooltip(shape.getToolTipInfo());
		} else {
			Tooltip.uninstall(this, tooltip);
			prop = movedOnVisPropertyDescription(x, y);
			if (prop != null) {
				configurationTooltip(prop.getTooltipText());
			}
		}
		if ((shape != null) || (prop != null)) {
			setCursor(Cursor.HAND);
			cursorState = Cursor.HAND;
		} else {
			cursorState = Cursor.DEFAULT;
			setCursor(Cursor.DEFAULT);
		}
	}

	public void handleMouseClicked(MouseEvent e) {
		if (visGraph == null) {
			return;
		}
		if (isDragging) {
			e.consume();
			isDragging = false;
			return;
		}

		Point2D p = translatePoint(new Point2D(e.getX(), e.getY()));
		int x = (int) p.getX();
		int y = (int) p.getY();
		//System.out.println("x = " + x + " AND y = " + y);
		Shape shape = visGraph.findShape(p);
		if (clickedOnClosePropertyBox(x, y, shape) || clickedOnCloseDisjointBox(x, y)) {
			return;
		}
		if (clickedOnShape(x, y, e, shape))
			return;
		if (e.getButton() == MouseButton.SECONDARY) {
			if (menuVisGeneralContext != null) {
				closeContextMenu(menuVisGeneralContext);
			}
			showContextMenu((int) e.getScreenX(), (int) e.getScreenY());
		}
		Platform.runLater(redrawRunnable);
	}

	private void configurationTooltip(String tip) {
		String styledContent = "<html><head><style>"
				+ "body {"
				+ "  font-size: 13px;"
				+ "  background-color: rgba(0, 0, 0, 0.9);"
				+ "  color: white;"
				+ "  margin: 0;"
				+ "  padding: 5px;"
				+ "}"
				+ "b {"
				+ "  color: lightblue;"
				+ "  font-size: 14px;"
				+ "}"
				+ "</style></head><body>"
				+ tip
				+ "</body></html>";

		webEngine.loadContent(styledContent);
		tooltip.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		web.setPrefSize(0, 0);
		webEngine.documentProperty().addListener((obs, oldDoc, newDoc) -> {
			if (newDoc != null) {
				webEngine.executeScript("document.body.style.width = 'auto'; document.body.style.height = 'auto';");
				Object scrollWidth = webEngine.executeScript("document.body.scrollWidth");
				Object scrollHeight = webEngine.executeScript("document.body.scrollHeight");

				double width = ((Number) scrollWidth).doubleValue();
				double height = ((Number) scrollHeight).doubleValue();

				if (height > 600) {
					web.setPrefSize(width, 600);
				} else {
					web.setPrefSize(width, height);
				}
			}
		});
		tooltip.setGraphic(web);
		Tooltip.install(this, tooltip);
	}



	/**
	 * Method to check if it needs to expand the canvas size
	 */
    public void checkAndResizeCanvas() {
        // deja un espacio de 245px
        double maxY = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;

        for (VisLevel level : visGraph.getLevelSet()) {
            for (Shape shape : level.levelShapes) {
                double shapeMaxY = shape.getBottomCorner() * factor;
                if (shapeMaxY > maxY) {
                    maxY = shapeMaxY;
                }

                double shapeMinY = shape.getTopCorner() * factor;
                if (shapeMinY < minY) {
                    minY = shapeMinY;
                }

                double shapeMaxX = shape.getRightCorner() * factor;
                if (shapeMaxX > maxX) {
                    maxX = shapeMaxX;
                }
            }
        }

        //System.out.println("maxY " + maxY + " - minY " + minY + " - maxX " + maxX);

        double viewportHeight = scroll.getViewportBounds().getHeight();
        canvasHeight = (int) maxY + 245;// + VisConstants.HEIGHT_MARGIN;
        //canvasWidth = (int) maxX + VisConstants.WIDTH_MARGIN;

       // printScrollAttributes();

        scroll.setVmax(Math.max(0, maxY - viewportHeight));

        if (minY < 0) {
            // Adjust the shape position
            for (VisLevel level : visGraph.getLevelSet()) {
                ArrayList<Shape> orderedShapeList = level.orderedList();
                for (Shape shape : orderedShapeList) {
                    shape.setPosY((int) ((shape.getPosY() * factor - minY + BORDER_PANEL) / factor));
                }
            }
        }

    }

    public void printScrollAttributes() {
        System.out.println("ScrollPane Attributes:--------------------------------------");
        System.out.println("Hvalue: " + scroll.getHvalue());
        System.out.println("Vvalue: " + scroll.getVvalue());
        System.out.println("Hmin: " + scroll.getHmin());
        System.out.println("Hmax: " + scroll.getHmax());
        System.out.println("Vmin: " + scroll.getVmin());
        System.out.println("Vmax: " + scroll.getVmax());
        System.out.println("Viewport Bounds: " + scroll.getViewportBounds());
        System.out.println("Content Bounds: " + scroll.getContent().getBoundsInParent());
        System.out.println("Pannable: " + scroll.isPannable());
        System.out.println("FitToWidth: " + scroll.isFitToWidth());
        System.out.println("FitToHeight: " + scroll.isFitToHeight());
        System.out.println("PrefWidth: " + scroll.getPrefWidth());
        System.out.println("PrefHeight: " + scroll.getPrefHeight());
        System.out.println("Width: " + scroll.getWidth());
        System.out.println("Height: " + scroll.getHeight()  + "\n");
    }



    public void checkAndResizeCanvas3() {
        double maxY = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;

        //double currentHvalue = scroll.getHvalue();
        //double currentVvalue = scroll.getVvalue();

        for (VisLevel level : visGraph.getLevelSet()) {
            for (Shape shape : level.levelShapes) {
                double shapeMaxY = shape.getBottomCorner() * factor;
                if (shapeMaxY > maxY) {
                    maxY = shapeMaxY;
                }

                double shapeMinY = shape.getTopCorner() * factor;
                if (shapeMinY < minY) {
                    minY = shapeMinY;
                }

                double shapeMaxX = shape.getRightCorner() * factor;
                if (shapeMaxX > maxX) {
                    maxX = shapeMaxX;
                }
            }
        }

        System.out.println("maxY " + maxY + " - minY " + minY + " - maxX " + maxX);

        double viewportHeight = scroll.getViewportBounds().getHeight();
        canvasHeight = (int) maxY + VisConstants.HEIGHT_MARGIN;
        //canvasWidth = (int) maxX + VisConstants.WIDTH_MARGIN;

        System.out.println("screenHeight: " + screenHeight + " , screenWidth: " + screenWidth + " canvasHeight: " + canvasHeight + " , canvasWidth: " + canvasWidth);
        System.out.println("canvasHeight - viewportHeight: " + (canvasHeight- viewportHeight) + "\n");

        scroll.setVmax(Math.max(0, canvasHeight - viewportHeight));
        //scroll.setVmax(canvasHeight - viewportHeight);
        //scroll.setHmax(screenWidth - canvasWidth);

        if (minY < 0) {
            // Adjust the shape position
            for (VisLevel level : visGraph.getLevelSet()) {
                ArrayList<Shape> orderedShapeList = level.orderedList();
                for (Shape shape : orderedShapeList) {
                    shape.setPosY((int) ((shape.getPosY() * factor - minY + BORDER_PANEL) / factor));
                }
            }
        }

        //scroll.setHvalue(currentHvalue);
        //scroll.setVvalue(currentVvalue);
    }

	/**
	 * translates point to current zoom factor
	 */
	private Point2D translatePoint(Point2D p) {
		/*
		 * when scaling positions get messed up so to keep actions as in a 1,1 ratio I
		 * need to scale down event points
		 */
		return new Point2D((int) ((p.getX() + offsetX) / factor), (int) ((p.getY() + offsetY) / factor));

	}

	private VisObjectProperty movedOnVisPropertyDescription(int x, int y) {
		for (Entry<String, VisObjectProperty> entry : visGraph.propertyMap.entrySet()) {
			if (entry.getValue().onProperty(new Point2D(x, y)))
				return entry.getValue();
		}
		return null;
	}

	public void start() {
		System.out.println("start");
		//Platform.runLater(relaxerRunnable);
	}

	public void stop() {
		System.out.println("stop"); 
	}

	private boolean isInsidePropertyBox(int x, int y) {
		if (visGraph == null) {
			return false;
		}
		for (Entry<String, Shape> entry : visGraph.shapeMap.entrySet()) {
			Shape shape = entry.getValue();
			if ((shape instanceof VisClass) && (shape.asVisClass().propertyBox != null)) {
				if (shape.asVisClass().onCloseBox(x, y)) {
					return true;
				}
			}
			if ((shape instanceof VisClass) && (shape.asVisClass().getDisjointConnectors() != null)) {
				if (shape.asVisClass().onCloseDisjoints(x, y)) {
					return true;
				}
			}
		}
		return false;
	}


	private boolean clickedOnShape(int x, int y, MouseEvent e, Shape shape) {
		if (visGraph == null) {
			return false;
		}
		if (shape != null) {
			if (e.getClickCount() == 2 && !e.isConsumed() && e.getButton() == MouseButton.PRIMARY) {
				// double click
				e.consume();
				if (shape instanceof VisClass visClass) {
					OWLClassExpression classExpression = visClass.getLinkedClassExpression();
					if (classExpression != null && (classExpression.isOWLThing() || classExpression.isOWLNothing())
							|| visClass.isBottom) {
						return false;
					}
				}
				if (shape.allSubHidden()) {
					shape.hide();
					shape.updateParents();
					refreshDashedConnectors();
					setStateChanged(true);
					Platform.runLater(relaxerRunnable);
					return true;
				}
			} else {
				switch (e.getButton()) {

				// if right-click on the figure
				case SECONDARY:
					if (menuVisShapeContext != null) {
						closeContextMenu(menuVisShapeContext);
					}
					if (menuVisGeneralContext != null) {
						closeContextMenu(menuVisGeneralContext);
					}
					showContextMenu(shape, e);
					break;
				case PRIMARY:
					// Right: Click on the open symbol
					if (pressedRightOpen(shape, x, y, e)) {
						if (shape.getState() == Shape.CLOSED || shape.getState() == Shape.PARTIALLY_CLOSED) {
							// if [+] clicked, open the node
							shape.openRight();
							shape.updateVisibleDescendantsForParents();
							refreshDashedConnectors();
							VisLevel.adjustWidthAndPos(visGraph.getLevelSet());
							setStateChanged(true);
							Platform.runLater(relaxerRunnable);
						}
					}
					// Right: Click on the close symbol
					else if (pressedRightClose(shape, x, y, e)) {
						if (shape.getState() == Shape.OPEN || shape.getState() == Shape.PARTIALLY_CLOSED) {
							// if [-] clicked, close the node
							shape.closeRight();
							shape.updateHiddenDescendantsForParents();
							refreshDashedConnectors();
							VisLevel.adjustWidthAndPos(visGraph.getLevelSet());
							setStateChanged(true);
							Platform.runLater(relaxerRunnable);
						}
					}
					// Left: Click on the open symbol
					else if (pressedLeftOpen(shape, x, y, e)) {
						if (shape.getLeftState() == Shape.LEFTCLOSED || shape.getLeftState() == Shape.LEFT_PARTIALLY_CLOSED) {
							// if [+] clicked, open the node
							shape.openLeft();
							refreshDashedConnectors();
							VisLevel.adjustWidthAndPos(visGraph.getLevelSet());
							setStateChanged(true);
							Platform.runLater(relaxerRunnable);
						}
					}
					// Left: Click on the close symbol
					else if (pressedLeftClose(shape, x, y, e)) {
						if (shape.getLeftState() == Shape.LEFTOPEN || shape.getLeftState() == Shape.LEFT_PARTIALLY_CLOSED) {
							// if [-] clicked, close the node
							shape.closeLeft();
							refreshDashedConnectors();
							VisLevel.adjustWidthAndPos(visGraph.getLevelSet());
							setStateChanged(true);
							Platform.runLater(relaxerRunnable);
						}


					} else { // pressed elsewhere on the shape
						if (!getShowConnectors()) {
							if (selectedShapes.contains(shape)) {
								selectedShapes.remove(shape);
							} else {
								selectedShapes.add(shape);
							}
							Platform.runLater(redrawRunnable);
						}

						paintFrame.focusOnShape(null, shape);
						break;
					}
				}
				getVisGraph().updateObservers(VisConstants.GENERALOBSERVER);
				return true;
			}
		} else {
			if (menuVisShapeContext != null) {
				closeContextMenu(menuVisShapeContext);
			}
			if (menuVisGeneralContext != null) {
				closeContextMenu(menuVisGeneralContext);
			}
		}

		getVisGraph().updateObservers(VisConstants.GENERALOBSERVER);
		return false;
	}

	private boolean clickedOnClosePropertyBox2(int x, int y, Shape shape) {
		if (visGraph == null || shape == null) {
			return false;
		}

		if(shape.asVisClass().propertyBox != null){
			if (shape.asVisClass().onCloseBox(x, y)) {
				boolean b = shape.asVisClass().propertyBox.visible;
				shape.asVisClass().propertyBox.setVisible(!b);
				showRelatedProperties(shape.asVisClass(), visGraph, !b);
				setStateChanged(true);
				Platform.runLater(relaxerRunnable);
				return true;
			}
		}
		return false;
	}

	private boolean clickedOnClosePropertyBox(int x, int y, Shape shape) {
		if (visGraph == null || shape == null) {
			return false;
		}

		if(shape.asVisClass().propertyBox != null){
			if (shape.asVisClass().onCloseBox(x, y)) {
				Task<Void> task = new Task<>() {
					@Override
					protected Void call() {
						boolean visibility = shape.asVisClass().propertyBox.visible;
						shape.asVisClass().propertyBox.setVisible(!visibility);
						if(visibility) compactNodes(shape);
						showRelatedProperties(shape.asVisClass(), visGraph, !visibility);

						return null;
					}
				};

				Stage loadingStage = showLoadingStage(task);

				task.setOnSucceeded(e -> {
					loadingStage.close();
					setStateChanged(true);
					Platform.runLater(relaxerRunnable);
				});
				task.setOnFailed(e -> {
					task.getException().printStackTrace();
					loadingStage.close();

				});

				new Thread(task).start();
				return true;
			}
		}
		return false;
	}

	private boolean clickedOnCloseDisjointBox(int x, int y) {
		if (visGraph == null) {
			return false;
		}
		for (Entry<String, Shape> entry : visGraph.shapeMap.entrySet()) {
			Shape shape = entry.getValue();
			if (shape instanceof VisClass visClass) {
				if (visClass.getDisjointConnectors() != null && shape.asVisClass().onCloseDisjoints(x, y)) {
					if (selectedDisjoints.contains(shape)) {
						selectedDisjoints.remove(shape);
					} else {
						selectedDisjoints.add(shape);
					}
					Platform.runLater(redrawRunnable);
					return true;
				}
			}
		}
		return false;
	}

	private void showRelatedProperties(VisClass visClass, VisGraph visGraph, boolean visibility) {
		for (VisObjectProperty property : visClass.getPropertyBox().getProperties()) {
			for (Entry<String, Shape> entry : visGraph.shapeMap.entrySet()) {
				Shape relatedShape = entry.getValue();
				if (relatedShape instanceof VisClass && relatedShape != visClass) {
					VisClass relatedVisClass = relatedShape.asVisClass();
					if (relatedVisClass.getPropertyBox() != null) {
						for (VisObjectProperty relatedProperty : relatedVisClass.getPropertyBox().getProperties()) {
							if (isRelated(property, relatedProperty)) {
								relatedVisClass.getPropertyBox().setVisible(visibility);
								if(!visibility) compactNodes(relatedShape);
							}
						}
					}
				}
			}
		}
	}

	private boolean isRelated(VisObjectProperty property, VisObjectProperty relatedProperty) {
		OWLReasoner reasoner = property.getReasoner();
		OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();
		return reasoner
				.isEntailed(dataFactory.getOWLSubObjectPropertyOfAxiom(property.oPropExp, relatedProperty.oPropExp))
				|| reasoner.isEntailed(dataFactory.getOWLSubObjectPropertyOfAxiom(relatedProperty.oPropExp, property.oPropExp));
	}

	public void focusOnShape(String shapeKey, Shape pshape) {
		Shape shape = pshape != null ? pshape : visGraph.getShape(shapeKey);

		if (shape != null) {
            double viewportWidth = scroll.getViewportBounds().getWidth();
            double viewportHeight = scroll.getViewportBounds().getHeight();

			double x = (shape.getPosX() * factor) - (viewportWidth / 2);
			double y = (shape.getPosY() * factor) - (viewportHeight / 2);

            double hMax = canvasWidth - viewportWidth;
            double vMax = canvasHeight - viewportHeight;

            x = Math.max(0, Math.min(x, hMax));
            y = Math.max(0, Math.min(y, vMax));

			scroll.setHvalue(x);
			scroll.setVvalue(y);
		}
	}

	private boolean pressedRightOpen(Shape shape, int x, int y, MouseEvent e) {
		return (x >= shape.posx + shape.getWidth() / 2 + 1 && x <= shape.posx + shape.getWidth() / 2 + 10
				&& y >= shape.posy - 10 && y <= shape.posy && !e.isMetaDown());
	}

	private boolean pressedLeftOpen(Shape shape, int x, int y, MouseEvent e) {
		return (x >= shape.posx - shape.getWidth() / 2 - 10 && x <= shape.posx - shape.getWidth() / 2 - 1
				&& y >= shape.posy - 10 && y <= shape.posy && !e.isMetaDown());
	}


	private boolean pressedRightClose(Shape shape, int x, int y, MouseEvent e) {
		return x >= shape.posx + shape.getWidth() / 2 + 1 && x <= shape.posx + shape.getWidth() / 2 + 10
				&& y >= shape.posy + 1 && y <= shape.posy + 10 && !e.isMetaDown();
	}

	private boolean pressedLeftClose(Shape shape, int x, int y, MouseEvent e) {
		return x >= shape.posx - shape.getWidth() / 2 - 10 && x <= shape.posx - shape.getWidth() / 2 - 1
				&& y >= shape.posy + 1 && y <= shape.posy + 10 && !e.isMetaDown();
	}

	public void refreshDashedConnectors() {
		visGraph.clearDashedConnectorList();
		visGraph.addDashedConnectors();
	}

	private void showContextMenu(Shape s, MouseEvent e) {
		double x, y;
		x = e.getScreenX();
		y = e.getScreenY();

		menuVisShapeContext = new VisShapeContext(s, this, e);
		menuVisShapeContext.show(this, x, y);
	}

	private void closeContextMenu(ContextMenu menu) {
		menu.hide();
	}

	private void showContextMenu(int x, int y) {
		menuVisGeneralContext = new VisGeneralContext(this);
		menuVisGeneralContext.show(this, x, y);
	}

	public VisGraph getVisGraph() {
		return visGraph;
	}

	private Shape getUpperShape(int index, ArrayList<Shape> list) {
		for (int z = index - 1; z >= 0; z--) {
			if (list.get(z).isVisible())
				return list.get(z);
		}
		return null;

	}

	private Shape getLowerShape(int index, ArrayList<Shape> list) {
		for (int z = index + 1; z < list.size(); z++) {
			if (list.get(z).isVisible())
				return list.get(z);
		}
		return null;
	}

	private void shapeRepulsion(Shape repellingShape, int direction) {
		if (repulsion) {
			VisLevel currentLevel = repellingShape.getVisLevel();
			ArrayList<Shape> orderedList = currentLevel.orderedList();
			int repellingIndex = orderedList.indexOf(repellingShape);
			switch (direction) {
				case UP:
					if (repellingIndex > 0) {
						Shape upperShape = getUpperShape(repellingIndex, orderedList);
						if (upperShape == null) // it's the visible upper shape
							return;

						int upperShapeHeight = (upperShape instanceof VisClass && upperShape.asVisClass().propertyBox != null
								&& upperShape.asVisClass().propertyBox.visible)
								? upperShape.getTotalHeight()
								: upperShape.getHeight();

						int repellingTopCorner = repellingShape.getTopCorner() - repellingShape.asVisClass().topToBarDistance;

						int upperShapeLimit = upperShape.getPosY() + upperShapeHeight / 2 + MIN_SPACE;

						if (repellingTopCorner < upperShapeLimit) {
							upperShape.setPosY(upperShape.getPosY() - upperShapeHeight / 2);
						}

						shapeRepulsion(upperShape, direction);

					}
					break;

				case DOWN:
					if (repellingIndex < orderedList.size() - 1) {
						Shape lowerShape = getLowerShape(repellingIndex, orderedList);
						// it's the visible lower shape
						if (lowerShape == null)
							return;

						int lowerShapeHeight = (lowerShape instanceof VisClass && lowerShape.asVisClass().propertyBox != null
								&& lowerShape.asVisClass().propertyBox.visible)
								? lowerShape.getTotalHeight()
								: lowerShape.getHeight();

						int lowerShapeTopCorner = lowerShape.getTopCorner() - lowerShape.asVisClass().topToBarDistance;

						if (repellingShape.getBottomCorner() + MIN_SPACE > lowerShapeTopCorner) {
							lowerShape.setPosY(lowerShape.getPosY() + lowerShapeHeight / 2);
						}

						shapeRepulsion(lowerShape, direction);
					}
					break;
			}
		}
	}

	/**
	 * Action done when changing kce Combo
	 */
	public void doKceOptionAction() {
		if (getVisGraph() == null) {
			return;
		}
		cleanSelectedConcepts();
		KCEConceptExtraction extractorKCE = new KCEConceptExtraction();
		RDFRankConceptExtraction extractorRDFRank = new RDFRankConceptExtraction();
		PageRankConceptExtraction extractorPageRank = new PageRankConceptExtraction();
		CustomConceptExtraction extractorCustom = new CustomConceptExtraction(getSelectedConcepts());

		switch (getKceOption()) {
			case VisConstants.NONECOMBOOPTION -> { // "None"
				getVisGraph().clearDashedConnectorList();
				getVisGraph().showAll();
			}
			case VisConstants.KCECOMBOOPTION1 -> { // "KCE10"
				getVisGraph().showAll();
				extractorKCE.hideNonKeyConcepts(activeOntology, this.getVisGraph(), 10);
			}
			case VisConstants.KCECOMBOOPTION2 -> { // "KCE20"
				getVisGraph().showAll();
				extractorKCE.hideNonKeyConcepts(activeOntology, this.getVisGraph(), 20);
			}
			case VisConstants.PAGERANKCOMBOOPTION1 -> { // "PageRank10"
				getVisGraph().showAll();
				extractorPageRank.hideNonKeyConcepts(activeOntology, this.getVisGraph(), 10);
			}
			case VisConstants.PAGERANKCOMBOOPTION2 -> { // "PageRank20"
				getVisGraph().showAll();
				extractorPageRank.hideNonKeyConcepts(activeOntology, this.getVisGraph(), 20);
			}
			case VisConstants.RDFRANKCOMBOOPTION1 -> { // "RDFRank10"
				getVisGraph().showAll();
				extractorRDFRank.hideNonKeyConcepts(activeOntology, this.getVisGraph(), 10);
			}
			case VisConstants.RDFRANKCOMBOOPTION2 -> { // "RDFRank20"
				getVisGraph().showAll();
				extractorRDFRank.hideNonKeyConcepts(activeOntology, this.getVisGraph(), 20);
			}
			case VisConstants.CUSTOMCOMBOOPTION3 -> { // "Custom"
				getVisGraph().showAll();
				showConceptSelectionPopup(getVisGraph().shapeMap);
				if (getSelectedConcepts().isEmpty()) return;
				extractorCustom.hideNonKeyConcepts(activeOntology, this.getVisGraph(), getSelectedConcepts().size());

			}
		}
		compactGraph();
	}

	public void compactGraph() {
		int currentY = BORDER_PANEL;
		int minY = -1; 
		int maxY = -1; 
		int span = -1; 
		int levelHeight = MIN_INITIAL_SPACE;
		Map<Integer, ArrayList<Shape>> visibleShapesPerLevel = new HashMap<>(); 
		Map<Integer, Integer> ySpanPerLevel = new HashMap<>(); 
		maxY = Integer.MIN_VALUE; 
		for (VisLevel level : visGraph.getLevelSet()) {
			currentY = BORDER_PANEL;
			ArrayList<Shape> orderedShapeList = level.orderedList();
			visibleShapesPerLevel.put(level.id, new ArrayList<>());
			minY = BORDER_PANEL; 	
			for (Shape shape : orderedShapeList) {
				if (shape.isVisible()) { 
					shape.setPosY(currentY);
					visibleShapesPerLevel.get(level.id).add(shape); 
					currentY += shape.getHeight() + levelHeight;
					maxY = (Math.max(maxY, currentY));
				}
			}
			ySpanPerLevel.put(level.id, currentY-minY);
		}

		span = maxY - minY; // this is the maximum level height we have witnessed
		// TODO: This must be refined to take into account the size of the intermediate boxes as well (getHeight)
		for (VisLevel level : visGraph.getLevelSet()) {
			currentY = BORDER_PANEL + (span - ySpanPerLevel.get(level.id))/ 2; 
			for (Shape shape : visibleShapesPerLevel.get(level.id)) {
				shape.setPosY(currentY);
				currentY += shape.getHeight() + levelHeight + MIN_SPACE;
			}
		}
		VisLevel.adjustWidthAndPos(visGraph.getLevelSet());
		setStateChanged(true);
		Platform.runLater(relaxerRunnable);
	}

	/**
	 * Repositions the displaced nodes located after the given shape.
	 */
	public void compactNodes(Shape s) {
		int currentY = s.getBottomCorner() + MIN_INITIAL_SPACE + MIN_SPACE;
        Map<Integer, ArrayList<Shape>> visibleShapesPerLevel = new HashMap<>();
		ArrayList<Shape> orderedShapeList = s.vdepthlevel.orderedList();
		visibleShapesPerLevel.put(s.vdepthlevel.id, new ArrayList<>());

		boolean readyToProcess = false;
		for (Shape shape : orderedShapeList) {
			if (shape == s){
				readyToProcess = true;
				continue;
			}
			if (readyToProcess && shape.isVisible()) {
				shape.setPosY(currentY);
				visibleShapesPerLevel.get(s.vdepthlevel.id).add(shape);
				currentY += shape.getHeight() + MIN_INITIAL_SPACE + MIN_SPACE;
			}
		}
		setStateChanged(true);
		Platform.runLater(relaxerRunnable);
	}

	public void applyStructuralReduction() {
		StructuralReducer.applyStructuralReduction(getOntology());
	}

	public void setParentFrame(Embedable pemb) {
		parentFrame = pemb;
	}

	public Embedable getParentFrame() {
		return parentFrame;
	}

	public void clearCanvas(){
		visGraph = null;
        scroll.setHvalue(0);
        scroll.setVvalue(0);
		GraphicsContext gc = this.getGraphicsContext2D();
		if ((canvasHeight != 0) && (canvasWidth != 0)) {
			gc.clearRect(0, 0, canvasWidth, canvasWidth);
		} else {
			gc.clearRect(0, 0, getWidth(), getHeight());
		}
	}

	private final Set<Shape> selectedConcepts = new HashSet<>();

	public Set<Shape> getSelectedConcepts() {
		return selectedConcepts;
	}

	public void cleanSelectedConcepts() {
		selectedConcepts.clear();
	}

	public void showConceptSelectionPopup(Map<String, Shape> shapeMap) {
		Stage popupStage = new Stage();
		popupStage.initModality(Modality.APPLICATION_MODAL);
		popupStage.setTitle("Select Key Concepts");

		ObservableList<Shape> allConcepts = FXCollections.observableArrayList(shapeMap.values());
		ObservableList<Shape> selectedConceptsList = FXCollections.observableArrayList(selectedConcepts);

		ListView<Shape> allConceptsView = createConceptListView(allConcepts);
		ListView<Shape> selectedConceptsView = createConceptListView(selectedConceptsList);

		setupDoubleClickActions(allConceptsView, selectedConceptsView, selectedConceptsList);

		TextField searchAllField = createSearchField(allConcepts, allConceptsView);
		TextField searchSelectedField = createSearchField(selectedConceptsList, selectedConceptsView);

		Button addButton = createAddButton(allConceptsView, selectedConceptsList);
		Button removeButton = createRemoveButton(selectedConceptsView, selectedConceptsList);
		Button saveButton = createSaveButton(selectedConceptsList, popupStage);

		HBox buttonBox = new HBox(5, addButton, removeButton);
		buttonBox.setAlignment(Pos.CENTER);

		VBox allConceptsBox = new VBox(5, searchAllField, allConceptsView);
		VBox selectedConceptsBox = new VBox(5, searchSelectedField, selectedConceptsView);

		HBox mainLayout = new HBox(10, allConceptsBox, buttonBox, selectedConceptsBox);
		VBox root = new VBox(10, mainLayout, saveButton);
		root.setAlignment(Pos.CENTER);
		root.setPadding(new Insets(20));

		Scene scene = new Scene(root, 600, 400);
		ClassLoader c = Thread.currentThread().getContextClassLoader();
		scene.getStylesheets().add(Objects.requireNonNull(c.getResource("styles.css")).toExternalForm());
		popupStage.setScene(scene);
		popupStage.showAndWait();
	}

	private void setupDoubleClickActions(ListView<Shape> allConceptsView, ListView<Shape> selectedConceptsView,
										 ObservableList<Shape> selectedConceptsList) {
		allConceptsView.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2) {
				Shape selected = allConceptsView.getSelectionModel().getSelectedItem();
				if (selected != null && !selectedConceptsList.contains(selected)) {
					selectedConceptsList.add(selected);
				}
			}
		});

		selectedConceptsView.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2) {
				Shape selected = selectedConceptsView.getSelectionModel().getSelectedItem();
				if (selected != null) {
					selectedConceptsList.remove(selected);
				}
			}
		});
	}

	private ListView<Shape> createConceptListView(ObservableList<Shape> concepts) {
		ListView<Shape> listView = new ListView<>(concepts);
		listView.setCellFactory(param -> new ListCell<Shape>() {
			@Override
			protected void updateItem(Shape item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
				} else {
					setText(item.getLabel());
				}
			}
		});
		return listView;
	}

	private TextField createSearchField(ObservableList<Shape> concepts, ListView<Shape> listView) {
		TextField searchField = new TextField();
		searchField.setPromptText("Search nodes");
		searchField.textProperty().addListener((observable, oldValue, newValue) -> {
			listView.setItems(concepts.filtered(item -> item.getLabel().toLowerCase().contains(newValue.toLowerCase())));
		});
		return searchField;
	}

	private Button createAddButton(ListView<Shape> allConceptsView, ObservableList<Shape> selectedConceptsList) {
		Button addButton = new Button("→");
		addButton.setOnAction(e -> {
			Shape selected = allConceptsView.getSelectionModel().getSelectedItem();
			if (selected != null && !selectedConceptsList.contains(selected)) {
				selectedConceptsList.add(selected);
			}
		});
		return addButton;
	}

	private Button createRemoveButton(ListView<Shape> selectedConceptsView, ObservableList<Shape> selectedConceptsList) {
		Button removeButton = new Button("←");
		removeButton.setOnAction(e -> {
			Shape selected = selectedConceptsView.getSelectionModel().getSelectedItem();
			if (selected != null) {
				selectedConceptsList.remove(selected);
			}
		});
		return removeButton;
	}

	private Button createSaveButton(ObservableList<Shape> selectedConceptsList, Stage popupStage) {
		Button saveButton = new Button("Save");
		saveButton.setOnAction(e -> {
			selectedConcepts.clear();
			selectedConcepts.addAll(selectedConceptsList);
			popupStage.close();
		});
		return saveButton;
	}


	public Stage showLoadingStage(Task<?> task) {
		Stage loadingStage = new Stage();
		loadingStage.initModality(Modality.APPLICATION_MODAL);
		loadingStage.initStyle(StageStyle.UNDECORATED);

		Label loadingLabel = new Label("Please, wait...");
		loadingLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #ffffff;");

		ProgressIndicator progressIndicator = new ProgressIndicator();
		progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
		progressIndicator.setStyle("-fx-accent: #3498db;");

		Button cancelButton = new Button("Cancel");
		cancelButton.getStyleClass().add("cancelButton");
		cancelButton.setOnAction(event -> {
			task.cancel();
			loadingStage.close();
			System.err.println("Task cancelled");
		});

		VBox loadingBox = new VBox(15.0, progressIndicator, loadingLabel, cancelButton);
		loadingBox.setAlignment(javafx.geometry.Pos.CENTER);
		loadingBox.setStyle(
				"-fx-background-color: rgba(0, 0, 0, 0.8); " +
						"-fx-padding: 20px; " +
						"-fx-background-radius: 10px; " +
						"-fx-effect: dropshadow(gaussian, black, 10, 0.5, 0, 0);"
		);

		ClassLoader c = Thread.currentThread().getContextClassLoader();
		Scene loadingScene = new Scene(loadingBox, 300, 200);
		loadingScene.getStylesheets().add(Objects.requireNonNull(c.getResource("styles.css")).toExternalForm());
		loadingStage.setScene(loadingScene);

		loadingStage.show();
		return loadingStage;
	}

}