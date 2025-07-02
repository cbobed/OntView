package sid.OntView2.common;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.Cursor;

import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import sid.OntView2.main.TopPanel;
import sid.OntView2.kcExtractors.KConceptExtractor;
import sid.OntView2.kcExtractors.KConceptExtractorFactory;
import sid.OntView2.utils.ExpressionManager;

public class PaintFrame extends Canvas {
    private static final Logger logger = LogManager.getLogger(GraphReorder.class);
    private static final long serialVersionUID = 1L;
    public Stage loadingStage = null;
	public ScrollPane scroll;
	static final int BORDER_PANEL = 60;
	static final int MIN_SPACE = 30;
	static final int MIN_INITIAL_SPACE = 40;
	private static final int DOWN = 0;
	private static final int UP = -1;
	boolean stable = false;
	boolean repulsion = true;
	public boolean renderLabel = false;
    public Set<String> languagesLabels = new HashSet<>();
	// CBL: added the qualified names rendering
	public boolean qualifiedNames = false;
	private String kceOption = VisConstants.NONECOMBOOPTION;
	Dimension2D prevSize;
	PaintFrame paintFrame = this;
    public TopPanel nTopPanel;
	OWLOntology activeOntology;
	private String activeOntologySource;
	OWLReasoner reasoner;
	VisGraph visGraph, rVisGraph; // visGraph will handle both depending on which is currently selected
	public VisShapeContext menuVisShapeContext = null;
	private VisGeneralContext menuVisGeneralContext = null;
    private Stage sliderStage = null;
	public int screenWidth;
	public int screenHeight;
	public int canvasWidth = 0;
	public int canvasHeight = 0;
    public int nTopPanelHeight = 0;
    public Set<Shape> orderedShapesByRDF = new HashSet<>();
    private int percentageShown = 0;
    private String strategyOptionStep = VisConstants.STEPSTRATEGY_RDFRANK;
    private String strategyOptionGlobal = VisConstants.GLOBALSTRATEGY_RDFRANK;
    public String selectedLanguage = "";
    public String getStrategyOptionStep() { return strategyOptionStep; }
    public void setStrategyOptionStep(String strategyOptionStep) { this.strategyOptionStep = strategyOptionStep; }
    public String getStrategyOptionGlobal() { return strategyOptionGlobal; }
    public void setStrategyOptionGlobal(String strategyOptionGlobal) { this.strategyOptionGlobal = strategyOptionGlobal; }
    public int getPercentageShown() {
        return percentageShown;
    }
    public void setPercentageShown(int percentageShown) {
        this.percentageShown = percentageShown;
    }

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
    public Stage getSliderStage() { return sliderStage; }
    public void setSliderStage(Stage stage) { sliderStage = stage; }
    public Shape selectedShape = null;
    public Shape focusOnShape = null;
    public Image prohibitedImage;

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

    public double getFactor() { return factor; }
    public void setFactorValue(double newFactor) { factor = newFactor; }

	/**
	 * sets scaling/zoom factor
	 */
	public void setFactor(double newZoom) {
        double oldZoom = this.factor;
        double viewportWidth = scroll.getViewportBounds().getWidth();
        double viewportHeight = scroll.getViewportBounds().getHeight();

        double centerX = offsetX + viewportWidth / (2 * oldZoom);
        double centerY = offsetY + viewportHeight / (2 * oldZoom);

        this.factor = newZoom;
        offsetX = centerX - viewportWidth / (2 * newZoom);
        offsetY = centerY - viewportHeight / (2 * newZoom);

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
	
	public class GlobalRedraw implements Runnable {
		public void run() {
			redraw();
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
                for (VisLevel lvl : visGraph.levelSet.values()) {
                    double neededHeight = Math.max(getHeight(), canvasHeight);
                    g.strokeLine(lvl.getXpos(), 0, lvl.getXpos(), neededHeight);
					// Uncomment this to get a vertical line in every level
					g.setStroke(Color.LIGHTGRAY);
				}
				g.setStroke(Color.BLACK);

                if (selectedShape != null) selectedShape.paintFocus(g);
                if (focusOnShape != null) focusOnShape.paintFocus(g);

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
					shape.drawShape(g);
				}
				drawPropertyBoxes(g);


           } else {
                logger.error("visGraph is null in draw method.");
			}
		}
	}

	private void drawConnectorsForSelectedShapes() {
		for (Shape shape : selectedShapes) {
			drawConnectorShape(shape);
		}
	}

	private void drawPropertyBoxes(GraphicsContext g2d) {
        Set<Shape> propertyBoxes = new HashSet<>();
		if (!this.isDisabled() && this.isVisible() && g2d != null) {
			for (Entry<String, Shape> entry : visGraph.shapeMap.entrySet()) {
				if (entry.getValue() instanceof VisClass) {
					VisClass v = entry.getValue().asVisClass();
					if ((v.getPropertyBox() != null) && (v.getPropertyBox().visible)) {
                        if (propertyBoxes.contains(v)) continue;
                        propertyBoxes.add(v);

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
        visGraph.run();

		paintFrame.setCursor(Cursor.WAIT);

		stable = true;
		stateChanged = true;
		factor = 1.0;
		paintFrame.setCursor(Cursor.DEFAULT);

		try {
			latch.await(); // Block until latch.countDown() is called in VisGraph
		} catch (InterruptedException e) {
            if (!VisGraph.isVoluntaryCancel()) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } else {
                VisGraph.voluntaryCancel(false);
            }
		}

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
		if (visGraph == null) { return; }

		boolean recentChange = false;

		if (stable) {
			while (stateChanged) {
                logger.debug("relax");
				stateChanged = false;

				// Faster version
				for (VisLevel level: visGraph.levelSet.values()) {
					for (Shape s_i: level.getShapeSet()) {
                        if (s_i.getTopCorner() < BORDER_PANEL) {
                            s_i.setPosY(BORDER_PANEL + (s_i.getHeight() / 2));
                            stateChanged = true;
                            shapeRepulsion(s_i, DOWN);
                        }

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
                        if (visGraph == null) return;
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
	public boolean hideRange = false;
	private Embedable parentFrame;
	private final Tooltip tooltip = new Tooltip();
    private Point2D pinchPoint = null;

    public void cleanConnectors() {
		selectedShapes.clear();
        selectedDisjoints.clear();
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
			selectedShapes.add(pressedShape);
			mouseLastY = (int) p.getY();
		} else {
            pinchPoint = new Point2D(e.getX() + offsetX, e.getY() + offsetY);
            mouseLastX = (int) p.getX();
			mouseLastY = (int) p.getY();
			setCursor(Cursor.CLOSED_HAND);
			Platform.runLater(redrawRunnable);
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
        Platform.runLater(relaxerRunnable);
	}

	/*
	 * MOUSE MOTION LISTENER
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
            if (pinchPoint != null) {
                double newOffsetX = pinchPoint.getX() - e.getX();
                double newOffsetY = pinchPoint.getY() - e.getY();

                newOffsetX = Math.max(0, Math.min(newOffsetX, scroll.getHmax()));
                newOffsetY = Math.max(0, Math.min(newOffsetY, scroll.getVmax()));

                scroll.setHvalue(newOffsetX);
                scroll.setVvalue(newOffsetY);
            }
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
            if (!shape.getToolTipInfo().isBlank()) {
                configurationTooltip(shape.getToolTipInfo());
            } else {
                configurationTooltip("No information for this node");
            }
		} else {
			Tooltip.uninstall(this, tooltip);
			prop = movedOnVisPropertyDescription(x, y);
			if (prop != null) {
				configurationTooltip(prop.getTooltipText());
			}
		}
		if ((shape != null) || (prop != null)) {
			setCursor(Cursor.HAND);
		} else {
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

	}

    public void configurationTooltip(String content) {
        double maxHeight = 500, maxWidth  = 500;
        int additionalSpace = 30;

        Text textNode = new Text(content);
        textNode.setFont(Font.font(12));
        double rawWidth  = textNode.getLayoutBounds().getWidth();
        double wrappedHeight = textNode.getLayoutBounds().getHeight();

        VBox container = new VBox(textNode);
        container.setPadding(new Insets(2));
        container.setBackground(new Background(
            new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)
        ));

        boolean needScroll = (rawWidth > maxWidth) || (wrappedHeight > maxHeight);

        Node graphic;
        if (needScroll) {
            ScrollPane scroll = new ScrollPane(container);
            scroll.setPrefWidth(Math.min(rawWidth + additionalSpace, maxWidth));
            scroll.setPrefHeight(Math.min(wrappedHeight, maxHeight));
            scroll.setMaxWidth(Math.min(rawWidth + additionalSpace, maxWidth));
            scroll.setMaxHeight(Math.min(wrappedHeight, maxHeight));
            scroll.setFitToWidth(true);

            scroll.setBackground(new Background(
                new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)
            ));
            scroll.setBorder(new Border(new BorderStroke(
                Color.BLUE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN
            )));

            scroll.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == KeyCode.DOWN) {
                    scroll.setVvalue(Math.min(scroll.getVvalue() + 0.02, 1.0));
                    e.consume();
                } else if (e.getCode() == KeyCode.UP) {
                    scroll.setVvalue(Math.max(scroll.getVvalue() - 0.02, 0.0));
                    e.consume();
                }
            });
            graphic = scroll;
        } else {
            graphic = container;
        }

        tooltip.setGraphic(graphic);
        tooltip.setStyle("-fx-background-color: white; -fx-border-color: blue; -fx-border-width: 1;");
        tooltip.setOnShown(evt -> {
            if (needScroll) {
                graphic.requestFocus();
            }
        });

        Tooltip.install(this, tooltip);
    }

    /**
	 * Method to check if it needs to expand the canvas size
	 */
    public void checkAndResizeCanvas() {
        if (visGraph == null) { return; }

        double maxY = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;

        for (VisLevel level : visGraph.getLevelSet().values()) {
            for (Shape shape : level.levelShapes) {
                if (shape.isVisible()) {
                    double shapeMaxY = shape.getBottomCorner();
                    double shapeMinY = shape.getTopCorner();
                    double shapeMaxX = shape.getRightCorner();

                    maxY = Math.max(maxY, shapeMaxY);
                    minY = Math.min(minY, shapeMinY);
                    maxX = Math.max(maxX, shapeMaxX);
                }
            }
        }

        if (minY < 0) {
            // Adjust the shape position
            for (VisLevel level : visGraph.getLevelSet().values()) {
                for (Shape shape : level.orderedList()) {
                    shape.setPosY((int) ((shape.getPosY() - minY + BORDER_PANEL)));
                    maxY = Math.max(maxY, shape.getBottomCorner());
                }
            }
        }

        if (minY > BORDER_PANEL) {
            double offset = minY - BORDER_PANEL;
            for (VisLevel level : visGraph.getLevelSet().values()) {
                for (Shape shape : level.orderedList()) {
                    shape.setPosY((int) (shape.getPosY() - offset));
                }
            }
            maxY -= offset;
        }

        double viewportHeight = scroll.getViewportBounds().getHeight() / factor;
        double viewportWidth = scroll.getViewportBounds().getWidth() / factor;
        canvasHeight = (int) (maxY + VisConstants.NEEDED_HEIGHT / factor);
        canvasWidth = (int) maxX + VisConstants.WIDTH_MARGIN;

        scroll.setVmax(Math.max(0, maxY - viewportHeight));
        scroll.setHmax(Math.max(0, maxX - viewportWidth));
    }

    /**
	 * translates point to current zoom factor
	 */
	private Point2D translatePoint(Point2D p) {
		/*
		 * when scaling positions get messed up so to keep actions as in a 1,1 ratio I
		 * need to scale down event points
		 */
		return new Point2D((int) ((p.getX() / factor) + offsetX), (int) ((p.getY() / factor) + offsetY));

	}

	private VisObjectProperty movedOnVisPropertyDescription(int x, int y) {
		for (Entry<String, VisObjectProperty> entry : visGraph.propertyMap.entrySet()) {
			if (entry.getValue().onProperty(new Point2D(x, y)))
				return entry.getValue();
		}
		return null;
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
                            if (shape.canShowFunctionalityRight()) {
                                // if [+] clicked, open the node
                                shape.openRight();
                                refreshDashedConnectors();
                                VisLevel.adjustWidthAndPos(visGraph.getLevelSet());
                                compactGraph();
                                if (menuVisShapeContext != null) {
                                    menuVisShapeContext.updateSliderView();
                                }
                            }
						}
					}
					// Right: Click on the close symbol
					else if (pressedRightClose(shape, x, y, e)) {
						if (shape.getState() == Shape.OPEN || shape.getState() == Shape.PARTIALLY_CLOSED) {
                            if (shape.canShowFunctionalityRight()) {
                                // if [-] clicked, close the node
                                shape.closeRight();
                                refreshDashedConnectors();
                                VisLevel.adjustWidthAndPos(visGraph.getLevelSet());
                                compactGraph();
                                if (menuVisShapeContext != null) {
                                    menuVisShapeContext.updateSliderView();
                                }
                            }
						}
					}
					// Left: Click on the open symbol
					else if (pressedLeftOpen(shape, x, y, e)) {
                        if (shape.canShowFunctionalityLeft()) {
                            if (shape.getLeftState() == Shape.LEFTCLOSED || shape.getLeftState() == Shape.LEFT_PARTIALLY_CLOSED) {
                                // if [+] clicked, open the node
                                shape.openLeft();
                                refreshDashedConnectors();
                                VisLevel.adjustWidthAndPos(visGraph.getLevelSet());
                                compactGraph();
                            }
						}
					}
					// Left: Click on the close symbol
					else if (pressedLeftClose(shape, x, y, e)) {
                        if (shape.canShowFunctionalityLeft()) {
                            if (shape.getLeftState() == Shape.LEFTOPEN || shape.getLeftState() == Shape.LEFT_PARTIALLY_CLOSED) {
                                // if [-] clicked, close the node
                                shape.closeLeft();
                                refreshDashedConnectors();
                                VisLevel.adjustWidthAndPos(visGraph.getLevelSet());
                                compactGraph();
                            }
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

                        if (isCancelled()){
                            shape.asVisClass().propertyBox.setVisible(visibility);
                            if(visibility) compactNodes(shape);
                            showRelatedProperties(shape.asVisClass(), visGraph, visibility);
                            Platform.runLater(() -> loadingStage.close());
                            Platform.runLater(redrawRunnable);
                        }
						return null;
					}
				};

                loadingStage = showLoadingStage(task);

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
        if (shapeKey != null) {
            focusOnShape = visGraph.getShape(shapeKey);
            Platform.runLater(redrawRunnable);
            PauseTransition pause = new PauseTransition(Duration.seconds(1));
            pause.setOnFinished(event -> {
                focusOnShape = null;
                Platform.runLater(redrawRunnable);
            });
            pause.play();
        }
		Shape shape = pshape != null ? pshape : visGraph.getShape(shapeKey);

		if (shape != null) {
            double viewportWidth = scroll.getViewportBounds().getWidth() / factor;
            double viewportHeight = scroll.getViewportBounds().getHeight() / factor;

			double x = (shape.getPosX()) - (viewportWidth / 2);
			double y = (shape.getPosY()) - (viewportHeight / 2);

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

	public void closeContextMenu(ContextMenu menu) {
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
								? (upperShape.asVisClass().propertyBox.height + upperShape.getHeight()/2)
								: upperShape.getHeight()/2;

						int repellingTopCorner = repellingShape.getTopCorner() - repellingShape.asVisClass().topToBarDistance;

                        int upperShapeLimit = upperShape.getBottomCorner();

						if (repellingTopCorner < upperShapeLimit  + MIN_SPACE) {
                            upperShape.setPosY(repellingTopCorner - MIN_SPACE - upperShapeHeight);
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

						int lowerShapeTopCorner = lowerShape.getTopCorner() - lowerShape.asVisClass().topToBarDistance;

						if (repellingShape.getBottomCorner() + MIN_SPACE > lowerShapeTopCorner) {
							lowerShape.setPosY(repellingShape.getBottomCorner() + MIN_SPACE + lowerShape.getHeight()/2
                                + lowerShape.asVisClass().topToBarDistance);
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
        cleanConnectors();

		if (Objects.equals(getKceOption(), VisConstants.NONECOMBOOPTION)) {
			getVisGraph().clearDashedConnectorList();
			getVisGraph().showAll();
		}
		else {
            CustomKCEModal modal = new CustomKCEModal(getVisGraph().shapeMap, new ArrayList<>());
            KConceptExtractor extractor = KConceptExtractorFactory.getInstance(getKceOption(), modal.getSelectedConcepts());

            switch (getKceOption()) {
                case VisConstants.KCECOMBOOPTION1,
                    VisConstants.PAGERANKCOMBOOPTION1,
                    VisConstants.RDFRANKCOMBOOPTION1 -> { // "KCE10"
                    getVisGraph().showAll();
                    extractor.hideNonKeyConcepts(activeOntology, this.getVisGraph(), 10);
                }
                case VisConstants.KCECOMBOOPTION2,
                    VisConstants.PAGERANKCOMBOOPTION2,
                    VisConstants.RDFRANKCOMBOOPTION2 -> { // "KCE20"
                    getVisGraph().showAll();
                    extractor.hideNonKeyConcepts(activeOntology, this.getVisGraph(), 20);
                }
                case VisConstants.CUSTOMCOMBOOPTION3 -> { // "Custom"
                    getVisGraph().showAll();
                    modal.showConceptSelectionPopup();
                    if (modal.getSelectedConcepts().isEmpty()) return;
                    extractor.hideNonKeyConcepts(activeOntology, this.getVisGraph(), modal.getSelectedConcepts().size());

                }
            }
        }
		getParentFrame().loadSearchCombo();
        compactGraph();
        if (menuVisShapeContext != null) {
            menuVisShapeContext.updateSliderView();
        }
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
		for (VisLevel level : visGraph.getLevelSet().values()) {
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
		for (VisLevel level : visGraph.getLevelSet().values()) {
			currentY = BORDER_PANEL + (span - ySpanPerLevel.get(level.id))/ 2; 
			for (Shape shape : visibleShapesPerLevel.get(level.id)) {
				shape.setPosY(currentY);
				currentY += shape.getHeight() + levelHeight + MIN_SPACE;
			}
		}
		VisLevel.adjustWidthAndPos(visGraph.getLevelSet());
		setStateChanged(true);
		Platform.runLater(relaxerRunnable);
        Platform.runLater(canvasAdjusterRunnable);
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

	public void setParentFrame(Embedable pEmb) {
		parentFrame = pEmb;
	}

	public Embedable getParentFrame() {
		return parentFrame;
	}

	public void clearCanvas(){
		visGraph = null;
        Tooltip.uninstall(this, tooltip);
        scroll.setHvalue(0);
        scroll.setVvalue(0);
		GraphicsContext gc = this.getGraphicsContext2D();
		if ((canvasHeight != 0) && (canvasWidth != 0)) {
			gc.clearRect(0, 0, canvasWidth, canvasWidth);
		} else {
			gc.clearRect(0, 0, getWidth(), getHeight());
		}
	}

	public Stage showLoadingStage(Task<?> task) {
		loadingStage = new Stage();
		loadingStage.initModality(Modality.APPLICATION_MODAL);
		loadingStage.initStyle(StageStyle.UNDECORATED);

		Label loadingLabel = new Label("Please, wait...");
		loadingLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #ffffff;");

        Label timerLabel = new Label("00:00");
        timerLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffffff;");

        final IntegerProperty secondsElapsed = new SimpleIntegerProperty(0);
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> {
                secondsElapsed.set(secondsElapsed.get() + 1);
                timerLabel.setText(String.format("%02d:%02d", secondsElapsed.get() / 60, secondsElapsed.get() % 60));
            })
        );
        timeline.setCycleCount(Animation.INDEFINITE);

		ProgressIndicator progressIndicator = new ProgressIndicator();
		progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
		progressIndicator.setStyle("-fx-accent: #3498db;");

		Button cancelButton = new Button("Cancel");
		cancelButton.getStyleClass().add("cancelButton");
		cancelButton.setOnAction(event -> {
            task.cancel();
            loadingLabel.setText("Cancelling task...");
            cancelButton.setDisable(true);
            logger.info("Task cancellation requested");
        });

		VBox loadingBox = new VBox(15.0, progressIndicator, loadingLabel, timerLabel, cancelButton);
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
        loadingStage.setOnShowing(e -> {
            timeline.play();
            loadingStage.requestFocus();
        });
        loadingStage.setOnHidden(e -> timeline.stop());

        //loadingStage.requestFocus();
        loadingStage.show();
		return loadingStage;
	}

    public void showAlertDialog(String title, String header, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        ClassLoader c = Thread.currentThread().getContextClassLoader();
        alert.getDialogPane().getStylesheets().add(Objects.requireNonNull(c.getResource("styles.css")).toExternalForm());
        alert.getDialogPane().getStyleClass().add("custom-alert");
        alert.getDialogPane().requestFocus();
        alert.showAndWait();
    }

}