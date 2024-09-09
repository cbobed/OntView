package sid.OntView2.common;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.Cursor;

import javafx.scene.text.Font;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import reducer.StructuralReducer;
import sid.OntView2.main.Mine;
import sid.OntView2.utils.ProgressBarDialogThread;

public class PaintFrame extends Canvas {

	private CountDownLatch latch;
	private static final long serialVersionUID = 1L;
	public ScrollPane scroll;
	static final int BORDER_PANEL = 50;
	static final int MIN_SPACE = 20;
	static final int MIN_INITIAL_SPACE = 40;
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
	VisGraph visGraph, oVisGraph, rVisGraph; // visGraph will handle both depending on which is currently selected
	private VisShapeContext menuVisShapeContext = null;
	private VisGeneralContext menuVisGeneralContext = null;

	public boolean isStable() {
		return stable;
	}

	public void setReasoner(OWLReasoner preasoner) {
		reasoner = preasoner;
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

	public String getActiveOntologySource() {
		return activeOntologySource;
	}

	public void setActiveOntolgySource(String p) {
		activeOntologySource = p;
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

	public PaintFrame() {
		super();
		try {
			this.setWidth(800);
			this.setHeight(600);
			prevSize = new Dimension2D(getWidth(), getHeight());
			VisConfig.getInstance().setConstants();
			// visGraph = new VisGraph(this);
			addEventHandlers();

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

	/*-*************************************************************
	 * Scaling  issues
	 *-*************************************************************/

	double factor = 1.0;
	double prevFactor = 1.0;
	Dimension2D oSize;

	/**
	 * sets scaling/zoom factor
	 *
	 * @param d d
	 */
	public void setFactor(double d) { factor = d; }

	public void setOriginalSize(Dimension2D in) { oSize = in; }
	
	/*** 
	 * Runnables required to push the drawing to the javaFx application thread using Platform.runLater()
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
	
	public class GlobalDrawer implements Runnable {
		public void run() {
			draw(); 
		}
	}
	
	public class Compacter implements Runnable {
		public void run() {
			compactGraph();
		}
	}
	
	Relaxer relaxerRunnable = new Relaxer();
	GlobalDrawer drawerRunnable = new GlobalDrawer();

	public Relaxer getRelaxerRunnable() {
		return relaxerRunnable; 
	}
	
	public GlobalDrawer getDrawerRunnable() {
		return drawerRunnable; 
	}
	
	/**
	 * scales by factor and adjusts panel size
	 *
	 * @param factor
	 */

	public void scale(double factor, Dimension2D size) {
		double newWidth = size.getWidth();
		double newHeight = size.getHeight();


		if (factor >= 1.0){
			setWidth(newWidth * factor);
			setHeight(newHeight * factor);
		} else {
			setWidth(newWidth / factor);
			setHeight(newHeight / factor);
		}

		GraphicsContext gc = this.getGraphicsContext2D();
		if (gc != null) {
			gc.restore();
			gc.save();
			gc.clearRect(0, 0, getWidth(), getHeight());
			gc.scale(factor, factor);
			Platform.runLater(drawerRunnable);
		}

	}

	/*-*************************************************************/
	public void drawDisjointShape() {
		if (this.getScene() != null && !this.isDisabled() && this.isVisible() && this.getGraphicsContext2D() != null) {
			GraphicsContext g = this.getGraphicsContext2D();

			if (visGraph != null) {
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

	public void drawConnectorShape(Shape shape) {
		if (this.getScene() != null && !this.isDisabled() && this.isVisible() && this.getGraphicsContext2D() != null) {
			GraphicsContext g = this.getGraphicsContext2D();

			if (visGraph != null && shape != null) {
				for (VisConnector c: shape.inConnectors) {
					c.draw(g); 
				}
				for (VisConnector c: shape.outConnectors) {
					c.draw(g); 
				}
				for (VisConnector c: shape.inDashedConnectors) {
					c.draw(g); 
				}
				for (VisConnector c: shape.outDashedConnectors) {
					c.draw(g); 
				}
			}
		}
	}

	public void draw() {
		if (this.getScene() != null && !this.isDisabled() && this.isVisible() && this.getGraphicsContext2D() != null) {
			GraphicsContext g = this.getGraphicsContext2D();
			g.clearRect(0, 0, getWidth(), getHeight());

			if (prevFactor != factor) {
				prevFactor = factor;
				if ((getWidth() != prevSize.getWidth() || getHeight() != prevSize.getHeight())) {
					prevSize = new Dimension2D(getWidth(), getHeight());
				}
			}

			if (visGraph != null) {
				
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

				if(!selectedDisjoints.isEmpty()){
					drawDisjointShape();
				}

				g.setStroke(Color.LIGHTGRAY);
				for (VisLevel lvl : visGraph.levelSet) {
					(g).strokeLine(lvl.getXpos(), 0, lvl.getXpos(), (int) (getHeight() / factor));
					// Uncomment this to get a vertical line in every level
					g.setStroke(Color.LIGHTGRAY);

				}
				g.setStroke(Color.BLACK);

				for (Entry<String, Shape> entry : visGraph.shapeMap.entrySet()) {
					Shape shape = entry.getValue();
					if (shape instanceof SuperNode) {
						shape.drawShape(g);
					}
				}

				for (Entry<String, Shape> entry : visGraph.shapeMap.entrySet()) {
					Shape shape = entry.getValue();
					if (!(shape instanceof SuperNode)) {
						shape.drawShape(g);
					}
				}
				drawPropertyBoxes(g);
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
		latch = new CountDownLatch(1);

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


		//new Thread(visGraph).start();
		paintFrame.setCursor(Cursor.WAIT);

		ProgressBarDialogThread progressBarObserver = new ProgressBarDialogThread(this);
		visGraph.addProgressBarObserver((observable, oldValue, newValue) -> progressBarObserver.update());

		VisGraphObserver graphObserver = new VisGraphObserver(this.getVisGraph());
		visGraph.addGeneralObserver((observable, oldValue, newValue) -> graphObserver.update());

		stable = true;
		stateChanged = true;
		factor = 1.0;
		paintFrame.setCursor(Cursor.DEFAULT);
		// scroll.getVerticalScrollBar().setUnitIncrement(15);

		try {
			latch.await(); // Block until latch.countDown() is called in VisGraph
		} catch (InterruptedException e) {
			e.printStackTrace();
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
			Throwable throwable = task.getException();
			throwable.printStackTrace();
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

	public Shape getEraseConnector() {
		return eraseConnector;
	}

	private boolean stateChanged = true;

	public void setStateChanged(boolean value) {
		stateChanged = value;
	}

	/**
	 * Updates node positions Avoids shapes being on top of each other updates y
	 * coord until there's no overlap
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
				draw();
			}
		}

	}

	/**
	 *
	 * MOUSELISTENER METHODS
	 *
	 **/

	// to keep track of the increment when moving the shapes

	int mouseLastY = 0;
	int mouseLastX = 0;
	Shape pressedShape = null;
	List<Shape> selectedShapes = new ArrayList<>();
	List<Shape> selectedDisjoints = new ArrayList<>();
	Shape drawConnector = null;
	Shape eraseConnector = null;
	Cursor cursorState = Cursor.DEFAULT;
	public boolean hideRange = false;
	private Embedable parentframe;
	private final Tooltip tooltip = new Tooltip();
	private WebView web = new WebView();
	private WebEngine webEngine = web.getEngine();

	public void cleanConnectors() {
		selectedShapes.clear();
		Platform.runLater(drawerRunnable);
	}

	public void handleMouseEntered(MouseEvent e) {
		setCursor(Cursor.DEFAULT);
	}

	public void handleMouseExited(MouseEvent e) {
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
			mouseLastY = (int) p.getY();
		} else {
			mouseLastX = (int) p.getX();
			mouseLastY = (int) p.getY();
			setCursor(Cursor.MOVE);
			Platform.runLater(new ConnectorDrawer(pressedShape));
		}
	}

	public void handleMouseReleased(MouseEvent e) {
		if (visGraph == null) {
			return;
		}

		pressedShape = null;
		repulsion = true;
		mouseLastY = 0;
		mouseLastX = 0;
		setCursor(Cursor.DEFAULT);
	}

	/*
	 * MOUSEMOTIONLISTENER
	 */

	public void handleMouseDragged(MouseEvent e) {
		int draggedY, draggedX;
		int direction;
		repulsion = (e.getButton() != MouseButton.SECONDARY);
		Point2D p = translatePoint(new Point2D(e.getX(), e.getY()));
		if ((mouseLastX == 0) && (mouseLastY == 0)) {
			draggedX = 0;
			draggedY = 0;
		} else {
			draggedY = (int) p.getY() - mouseLastY;
			draggedX = (int) p.getX() - mouseLastX;
		}
		if (pressedShape != null) {
			direction = ((draggedY > 0) ? DOWN : UP);
			pressedShape.setPosY(pressedShape.getPosY() + draggedY);
			stateChanged = true;
			shapeRepulsion(pressedShape, direction);
			mouseLastX = (int) p.getX();
			mouseLastY = (int) p.getY();

			checkAndResizeCanvas();

			Platform.runLater(drawerRunnable);
			Platform.runLater(new ConnectorDrawer(pressedShape));
		} else {
			double scrollHValue, scrollVValue;
			double contentWidth = scroll.getContent().getBoundsInLocal().getWidth();
			double contentHeight = scroll.getContent().getBoundsInLocal().getHeight();

			scrollHValue = scroll.getHvalue() - draggedX / contentWidth;
			scrollVValue = scroll.getVvalue() - draggedY / contentHeight;

			scrollHValue = Math.max(0, Math.min(scrollHValue, 1));
			scrollVValue = Math.max(0, Math.min(scrollVValue, 1));

			scroll.setHvalue(scrollHValue);
			scroll.setVvalue(scrollVValue);
		}
	}


	/*private void configureTooltipLockHandler() {
		this.sceneProperty().addListener((observable, oldScene, newScene) -> {
			if (newScene != null) {
				newScene.addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
			}
		});
	}

	public void handleKeyPressed(KeyEvent event) {
		if (event.getEventType() == KeyEvent.KEY_PRESSED && event.isAltDown() && event.getCode() == KeyCode.S) { // O Fn+F2 dependiendo del sistema
			System.out.println("Alt + S pressed");
			tooltipLocked = !tooltipLocked; // Cambiar el estado de bloqueo
			System.out.println("Tooltip locked: " + tooltipLocked);
		}
	}*/

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
		Point2D p = translatePoint(new Point2D(e.getX(), e.getY()));
		int x = (int) p.getX();
		int y = (int) p.getY();
		if (clickedOnClosePropertyBox(x, y) || clickedOnCloseDisjointBox(x, y)) {
			return;
		}
		if (clickedOnShape(x, y, e))
			return;
		if (e.getButton() == MouseButton.SECONDARY) {
			System.out.println("Right click");
			if (menuVisGeneralContext != null) {
				closeContextMenu(menuVisGeneralContext);
			}
			showContextMenu((int) e.getScreenX(), (int) e.getScreenY());
		}
		Platform.runLater(drawerRunnable);
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
		double maxY = Double.MIN_VALUE;
		double minY = Double.MAX_VALUE;
		
		for (VisLevel level : visGraph.getLevelSet()) {
			for (Shape shape : level.levelShapes) {
				double shapeMaxY = shape.getPosY() * factor + shape.getHeight() * factor;
				if (shapeMaxY > maxY) { maxY = shapeMaxY; }

				double shapeMinY = shape.getPosY() * factor - shape.getHeight() / 2.0 * factor;
				if (shapeMinY < minY) { minY = shapeMinY; }
			}
		}

		boolean needsResize = false;
		double newHeight = getHeight();

		if (maxY >= getHeight()) {
			newHeight = maxY + BORDER_PANEL;
			needsResize = true;
		}

		if (minY < 0) {
			newHeight = getHeight() - minY + BORDER_PANEL;
			// Adjust the shape position
			for (VisLevel level : visGraph.getLevelSet()) {
				ArrayList<Shape> orderedShapeList = level.orderedList();
				for (Shape shape : orderedShapeList) {
					shape.setPosY((int) ((shape.getPosY() * factor - minY + BORDER_PANEL) / factor));
				}
			}
			needsResize = true;
		}

		if (needsResize) {
			setHeight(newHeight);
		}
	}

	/**
	 * method to convert HTML-like content into JavaFX Tooltip styled text
	 *
	 * @param html
	 * @return String
	 */
	// web engine
	private String formatToolTipText(String html) {
		tooltip.setFont(new Font("Dialog", 12));
		// tooltip.setStyle("-fx-background-color: #cedef7; -fx-text-fill: #000000;");

		String formattedText = html.replaceAll("<html>", "").replaceAll("</html>", "").replaceAll("<b>", "")
				.replaceAll("</b>", "").replaceAll("<br>", "\n").replaceAll("<ul>", "").replaceAll("</ul>", "")
				.replaceAll("<li>", "\u2022 ").replaceAll("</li>", "\n");

		formattedText = formattedText.replaceAll("(?m)^.*SIDClass_.*\n\n?", "");

		return formattedText;
	}

	/**
	 * translates point to current zoom factor
	 *
	 * @param p
	 * @return Point
	 */

	private Point2D translatePoint(Point2D p) {
		/*
		 * when scaling positions get messed up so to keep actions as in a 1,1 ratio i
		 * need to scale down event points
		 */
		return new Point2D((int) (p.getX() / factor), (int) (p.getY() / factor));

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
		Platform.runLater(relaxerRunnable); 
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

	private boolean clickedOnShape(int x, int y, MouseEvent e) {
		if (visGraph == null) {
			return false;
		}
		Shape shape = visGraph.findShape(new Point2D(x, y));
		if (shape != null) {
			if (e.getClickCount() == 2 && !e.isConsumed() && e.getButton() == MouseButton.PRIMARY) {
				// double click
				e.consume();
				if (shape instanceof VisClass visClass) {
					OWLClassExpression classExpression = visClass.getLinkedClassExpression();
					if (classExpression != null && (classExpression.isOWLThing() || classExpression.isOWLNothing())) {
						return false;
					}
				}
				if (shape.allSubHidden()) {
					shape.hide();
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
					// Click on the open symbol
					if (pressedOpen(shape, x, y, e)) {
						if (shape.getState() == Shape.CLOSED || shape.getState() == Shape.PARTIALLY_CLOSED) {
							// si estaba cerrado el nodo [+] abrirlo
							shape.open();
							refreshDashedConnectors();
							VisLevel.adjustWidthAndPos(visGraph.getLevelSet());
							Platform.runLater(drawerRunnable);
						}
					}
					// Click on the close symbol
					else if (pressedClose(shape, x, y, e)) {
						if (shape.getState() == Shape.OPEN || shape.getState() == Shape.PARTIALLY_CLOSED) {
							// if [-] clicked, close the node
							shape.close();
							refreshDashedConnectors();
							VisLevel.adjustWidthAndPos(visGraph.getLevelSet());
							Platform.runLater(drawerRunnable);
						}
					} else { // pressed elsewhere on the shape
						if (!getShowConnectors()) {
							if (selectedShapes.contains(shape)) {
								selectedShapes.remove(shape);
							} else {
								selectedShapes.add(shape);
							}
							Platform.runLater(drawerRunnable);
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

	private boolean clickedOnClosePropertyBox(int x, int y) {
		if (visGraph == null) {
			return false;
		}
		for (Entry<String, Shape> entry : visGraph.shapeMap.entrySet()) {
			Shape shape = entry.getValue();
			if ((shape instanceof VisClass) && (shape.asVisClass().propertyBox != null)) {
				if (shape.asVisClass().onCloseBox(x, y)) {
					boolean b = shape.asVisClass().propertyBox.visible;
					shape.asVisClass().propertyBox.setVisible(!b);
					showRelatedProperties(shape.asVisClass(), visGraph, !b);
					setStateChanged(true);
					Platform.runLater(relaxerRunnable);
					return true;
				}
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
					Platform.runLater(drawerRunnable);
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
		/*
		 * focus the frame on the shape pos If shapeKey is null, it will look for it
		 */
		Shape shape = pshape != null ? pshape : visGraph.getShape(shapeKey);

		if (shape != null) {
			int x = (int) (shape.getPosX() * factor);
			int y = (int) (shape.getPosY() * factor);

			double scrollPaneWidth = scroll.getViewportBounds().getWidth();
			double scrollPaneHeight = scroll.getViewportBounds().getHeight();

			double hValue = (x - scrollPaneWidth / 2) / (getWidth() - scrollPaneWidth);
			double vValue = (y - scrollPaneHeight / 2) / (getHeight() - scrollPaneHeight);

			scroll.setHvalue(Math.max(0, Math.min(hValue, 1)));
			scroll.setVvalue(Math.max(0, Math.min(vValue, 1)));
		}

	}

	private boolean pressedOpen(Shape shape, int x, int y, MouseEvent e) {
		return (x >= shape.posx + shape.getWidth() / 2 + 1 && x <= shape.posx + shape.getWidth() / 2 + 10
				&& y >= shape.posy - 10 && y <= shape.posy && !e.isMetaDown());
	}

	private boolean pressedClose(Shape shape, int x, int y, MouseEvent e) {
		return x >= shape.posx + shape.getWidth() / 2 + 1 && x <= shape.posx + shape.getWidth() / 2 + 10
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
		menu = null;
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

						GraphicsContext g = this.getGraphicsContext2D();

						int upperShapeHeight = upperShape.getHeight();
						if ((upperShape instanceof VisClass) && (upperShape.asVisClass().propertyBox != null) && (upperShape.asVisClass().propertyBox.visible))
							upperShapeHeight += upperShape.asVisClass().getTotalHeight();

						if (repellingShape.getTopCorner() < (upperShape.getPosY() + upperShapeHeight/2 + MIN_SPACE)) {
							upperShape.setPosY(upperShape.getPosY() - upperShape.getHeight() / 2 );
						}

						shapeRepulsion(upperShape, direction);

					}
					break;

				case DOWN:
					if (repellingIndex < orderedList.size() - 1) {
						Shape lowerShape = getLowerShape(repellingIndex, orderedList);
						if (lowerShape == null) // it's the visible upper shape
							return;

						int lowerShapeHeight = repellingShape.getHeight();
						if ((repellingShape instanceof VisClass) && (repellingShape.asVisClass().propertyBox != null) && (repellingShape.asVisClass().propertyBox.visible)) {
							if (repellingShape.getBottomCorner() > lowerShape.getTopCorner() - MIN_SPACE) {
								lowerShape.setPosY(lowerShape.getPosY() + lowerShape.getTotalHeight() / 2);
							}
						}
						else {
							if (repellingShape.getBottomCorner() > lowerShape.getTopCorner() - MIN_SPACE) {
								lowerShape.setPosY(lowerShape.getPosY() + lowerShape.getHeight() / 2);
							}
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
		KCEConceptExtraction extractorKCE = new KCEConceptExtraction();
		RDFRankConceptExtraction extractorRDFRank = new RDFRankConceptExtraction();
		PageRankConceptExtraction extractorPageRank = new PageRankConceptExtraction();

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
		}
		
		compactGraph();
		Platform.runLater(drawerRunnable);
	}

	private void compactGraph() {
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
					maxY = (maxY<currentY?currentY:maxY); 
				}
			}
			ySpanPerLevel.put(level.id, currentY-minY); 
			// Adjust the x position of the level if needed
			level.setXpos(level.getXpos() + levelHeight);
		}

		span = maxY - minY; // this is the maximum level height we have witnessed
		// TODO: This must be refined to take into account the size of the intermediate boxes as well (getHeight)
		for (VisLevel level : visGraph.getLevelSet()) {
			currentY = BORDER_PANEL + (span - ySpanPerLevel.get(level.id))/ 2; 
			for (Shape shape : visibleShapesPerLevel.get(level.id)) {
				shape.setPosY(currentY);
				currentY += shape.getHeight() + levelHeight;
			}
		}
		setStateChanged(true);
		Platform.runLater(relaxerRunnable); 
	}

	public void applyStructuralReduction() {
		StructuralReducer.applyStructuralReduction(getOntology());
	}

	public boolean doApplyReductionCheck() {
		if (getOntology() != null) {
			applyStructuralReduction();
			return true;
		}
		return false;
	}

	public void setParentFrame(Embedable pemb) {
		parentframe = pemb;
	}

	public Embedable getParentFrame() {
		return parentframe;
	}
}