package sid.OntView2.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import javafx.animation.PauseTransition;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.Cursor;

import javafx.scene.text.Font;
import javafx.util.Duration;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.xml.sax.SAXException;

import reducer.StructuralReducer;
import sid.OntView2.utils.ProgressBarDialogThread;


public class PaintFrame extends Canvas implements Runnable{

	private static final long serialVersionUID = 1L;
	public ScrollPane scroll;
	static final int BORDER_PANEL = 50;
	static final int MIN_Y_SEP = 3;
	static final int SEP = 200;
	private static final int DOWN = 0;
	private static final int UP = -1;
	int     		width,height;
	boolean 		stable      = false;
	boolean 		repulsion   = true;
	public  boolean renderLabel = false;
//	private boolean kceEnabled = false;

	// CBL: added the qualified names rendering
	public boolean qualifiedNames = false;
	private String kceOption      = VisConstants.KCECOMBOOPTION1;

	Thread         relaxer;
	Dimension2D	   prevSize;
	PaintFrame     paintFrame = this;
	OWLOntology    activeOntology;
	private String activeOntologySource;
//    boolean        reduceCheck = false;

	OWLReasoner    reasoner;
	VisGraph       visGraph,oVisGraph,rVisGraph; //visGraph will handle both depending on which is currently selected
//    public boolean reduceChecked = false;

	String positionGraph = "LEFT";
	private VisShapeContext menuVisShapeContext = null;
	private VisGeneralContext menuVisGeneralContext = null;



	public boolean isStable(){
		System.out.println("isStable is " + stable);

		return stable;
	}
	public void setReasoner(OWLReasoner preasoner){reasoner = preasoner;}
	public OWLReasoner getReasoner(){return reasoner;}
	public void setOntology(OWLOntology ac){activeOntology = ac;}
	public OWLOntology getOntology(){return activeOntology;}
	public String getActiveOntologySource() {return activeOntologySource;}
	public void setActiveOntolgySource (String p ){ activeOntologySource = p;}
	public String getKceOption() {return kceOption;}
	public void setKceOption(String itemAt) {kceOption = itemAt;}
//	public boolean isReduceChecked(){return reduceChecked;}
//	public boolean setReduceChecke(boolean b){return reduceCheck;}


	public PaintFrame(){
		super();
		try {
			this.setWidth(800);
			this.setHeight(600);
			prevSize = new Dimension2D(getWidth(), getHeight());
			VisConfig.getInstance().setConstants();
			//visGraph = new VisGraph(this);
			addEventHandlers();

		}
		catch (Exception e) {
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

	double 		factor = 1.0;
	double 		prevFactor = 1.0;
	Dimension2D 	oSize;

	/**
	 * sets scaling/zoom factor
	 * @param d d
	 */
	public void setFactor(double d){ factor = d; }
	public void setOriginalSize(Dimension2D in){ oSize = in;}
	/**
	 * scales by factor and adjusts panel size
	 * @param factor
	 * @param size
	 */
	public void scale(double factor,Dimension2D size){
		GraphicsContext g2d = this.getGraphicsContext2D();
		g2d.scale(factor, factor);
		if (factor>1.0) {
			setWidth(size.getWidth() * factor);
			setHeight(size.getHeight() * factor);
		}
	}

	/*-*************************************************************/

	public void draw() {
		GraphicsContext g = this.getGraphicsContext2D();

		g.clearRect(0, 0, getWidth(), getHeight());

        if ((factor !=1.0)&&(stable)){
			(g).scale(factor, factor);
		}
		if (prevFactor!=factor){
			prevFactor = factor;
			if ((factor >=1.0) && (getWidth() != prevSize.getWidth() || getHeight() != prevSize.getHeight())) {
				prevSize = new Dimension2D(getWidth(), getHeight());
				setWidth(oSize.getWidth() * factor);
				setHeight(oSize.getHeight() * factor);

			}
		}
		if (visGraph!=null){

			List<VisConnector> connectorsCopy;
			synchronized (visGraph.connectorList) {
				connectorsCopy = new ArrayList<>(visGraph.connectorList);
			}
			for (VisConnector c : connectorsCopy) {
				//System.out.println("Drawing connector with color: " + VisConnector.color);
				c.draw(g);
			}

			List<VisConnector> dashedConnectorsCopy;
			synchronized (visGraph.dashedConnectorList) {
				dashedConnectorsCopy = new ArrayList<>(visGraph.dashedConnectorList);
			}
			for (VisConnector c : dashedConnectorsCopy) {
				//System.out.println("Drawing dashed connector with color: " + VisConnector.color);
				c.draw(g);
			}
			g.setStroke(Color.LIGHTGRAY);

			List<VisLevel> levelsCopy;
			synchronized (visGraph.levelSet) {
				levelsCopy = new ArrayList<>(visGraph.levelSet);
			}
			for (VisLevel lvl : levelsCopy) {
				//System.out.println("Drawing level with light gray color: " + Color.LIGHTGRAY);
				(g).strokeLine(lvl.getXpos(), 0, lvl.getXpos(), (int) (getHeight()/factor));
				//Uncomment this to get a vertical line in every level
				g.setStroke(Color.LIGHTGRAY);

			}
			g.setStroke(Color.BLACK);
			drawPropertyBoxes(g);

			HashMap<String, Shape> shapeMapCopy;
			synchronized (visGraph.shapeMap) {
				shapeMapCopy = new HashMap<>(visGraph.shapeMap);
			}
			for (Entry<String, Shape> entry : shapeMapCopy.entrySet()) {
				//System.out.println("Drawing shape with current color: " + g.getStroke());
				entry.getValue().drawShape(g);
			}

		}
	}
	private void drawPropertyBoxes(GraphicsContext g2d){
		for (Entry<String,Shape> entry : visGraph.shapeMap.entrySet()){
			if (entry.getValue() instanceof VisClass){
				VisClass v = entry.getValue().asVisClass();
				if ((v.getPropertyBox()!=null) && (v.getPropertyBox().visible)) {
					Font c = g2d.getFont();
					g2d.setStroke(Color.BLACK);
					v.getPropertyBox().draw(g2d);
					g2d.setFont(c);
				}
			}
		}
	}


	public  void createReasonedGraph(HashSet<OWLClassExpression> set,boolean check) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException{
		rVisGraph = new VisGraph(this);

		visGraph = rVisGraph;
		stable = false;
		System.out.println("createReasonedGraph is " + stable);
		visGraph.setActiveOntology(activeOntology);
//	   applyStructuralReduction();
		visGraph.setOWLClassExpressionSet(set);
		visGraph.setCheck(check);
		visGraph.setReasoner(reasoner);

		new Thread(visGraph).start();
		paintFrame.setCursor(Cursor.WAIT);

		visGraph.addObserver(new ProgressBarDialogThread(this));

		//visGraph.buildReasonedGraph(activeOntology, reasoner, set,check);

		VisGraphObserver graphObserver = new VisGraphObserver(this.getVisGraph());
//	   graphObserver.start();
		visGraph.addObserver(graphObserver);

//
		stable       = true;
		System.out.println("createReasonedGraph is " + stable);
		stateChanged = true;
//	   relax();
		factor = 1.0;
		paintFrame.setCursor(Cursor.DEFAULT);
		// scroll.getVerticalScrollBar().setUnitIncrement(15);



	}
	/**
	 * returns pressed shape
	 * @return Shape
	 */
	public Shape getPressedShape(){
		return pressedShape;
	}


	boolean stateChanged = true;
	/**
	 * Updates node positions
	 * Avoids shapes being on top of each other
	 * updates y coord until there's no overlap
	 **/

	synchronized void relax() {
		if (visGraph == null) {
			System.err.println("visGraph is null in relax method.");
			return;
		}

		// repulsion and atraction between nodes
		Shape shape_j,s_i;

		if (stable) {
			if (stateChanged) {
				//System.out.println("relax is " + stateChanged);
				stateChanged = false;

				HashMap<String, Shape> shapeMapCopy;
				synchronized (visGraph.shapeMap) {
					shapeMapCopy = new HashMap<>(visGraph.shapeMap);
				}
				for (Entry<String, Shape> e_i : shapeMapCopy.entrySet()){
					for (Entry<String, Shape> e_j: shapeMapCopy.entrySet()){
						s_i = e_i.getValue();
						shape_j = e_j.getValue();

						if ((s_i!=shape_j)&&(s_i.visible)){
							if ((s_i.getPosY() < shape_j.getPosY()) && (s_i.getPosY() + s_i.getTotalHeight()) > shape_j.getPosY()){
								stateChanged = true;
								shapeRepulsion(s_i, DOWN);
							}
						}
					}
					s_i = e_i.getValue();
					if (s_i.getPosY() < BORDER_PANEL){
						s_i.setPosY(BORDER_PANEL);
						stateChanged = true;
						shapeRepulsion(s_i, DOWN);
					}
					visGraph.adjustPanelSize((float) factor);

				}
			}
			draw();
		}
	}



	/**
	 *
	 *  MOUSELISTENER METHODS
	 *
	 **/

	//to keep track of the increment when moving the shapes

	int mouseLastY = 0;
	int mouseLastX = 0;
	Shape pressedShape =null;
	Cursor cursorState = Cursor.DEFAULT;
	public boolean hideRange = false;
	private Embedable parentframe;
	private final Tooltip tooltip = new Tooltip();


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
		pressedShape= visGraph.findShape(p);

		if (pressedShape!=null) {
			mouseLastY= (int) p.getY();
		}
		else {
			mouseLastX = (int) p.getX();
			mouseLastY = (int) p.getY();
			setCursor(Cursor.MOVE);
		}
		draw();
	}

	public void handleMouseReleased(MouseEvent e) {

		pressedShape=null;
		repulsion = true;
		mouseLastY=0;
		mouseLastX=0;
		setCursor(Cursor.DEFAULT);

	}

	/*
	 * MOUSEMOTIONLISTENER
	 */

	public void handleMouseDragged(MouseEvent e) {
		int draggedY,draggedX;
		int direction;
		repulsion = (e.getButton() != MouseButton.SECONDARY);
		Point2D p = translatePoint(new Point2D(e.getX(), e.getY()));
		if ((mouseLastX == 0) && (mouseLastY==0)){
			draggedX = 0;
			draggedY = 0;
		}
		else {
			draggedY = (int)p.getY()-mouseLastY;
			draggedX = (int)p.getX()-mouseLastX;
		}

		if (pressedShape!=null) {
			direction = ((draggedY > 0) ?  DOWN : UP);
			pressedShape.setPosY(pressedShape.getPosY()+ draggedY);
			stateChanged = true;
			shapeRepulsion (pressedShape,direction);
			mouseLastX = (int)p.getX();
			mouseLastY = (int)p.getY();
			draw();
		}
		else {
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

	public void focusOnShape2(String shapeKey,Shape pshape) {
		/*
		 * focus the frame on the shape pos
		 * If shapeKey is  null, it will look for it
		 */
		Shape shape = pshape != null ? pshape : visGraph.getShape(shapeKey);

		if (shape!= null) {
			int x  = (int) (shape.getPosX() * factor);
			int y  = (int) (shape.getPosY() * factor);

			double scrollPaneWidth = scroll.getViewportBounds().getWidth();
			double scrollPaneHeight = scroll.getViewportBounds().getHeight();

			double hValue = (x - scrollPaneWidth / 2) / (getWidth() - scrollPaneWidth);
			double vValue = (y - scrollPaneHeight / 2) / (getHeight() - scrollPaneHeight);

			scroll.setHvalue(Math.max(0, Math.min(hValue, 1)));
			scroll.setVvalue(Math.max(0, Math.min(vValue, 1)));
		}

	}

	/*private void scrollRectToVisible(Rectangle rect) {
		Bounds bounds = rect.getBoundsInParent();
		double hValue = (bounds.getMinX() - scroll.getHmin()) / (scroll.getHmax() - scroll.getHmin());
		double vValue = (bounds.getMinY() - scroll.getVmin()) / (scroll.getVmax() - scroll.getVmin());
		scroll.setHvalue(hValue);
		scroll.setVvalue(vValue);
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
		String tip;

		PauseTransition pause = new PauseTransition(Duration.seconds(4));

		if (shape !=null) {
			tip = shape.getToolTipInfo();
			tooltip.setText(formatToolTipText(tip));
			Tooltip.install(this, tooltip);
			pause.playFromStart();
		}
		else  {
			tooltip.hide();
			pause.stop();
			Tooltip.uninstall(this, tooltip);
			prop = movedOnVisPropertyDescription(x, y);
			if (prop != null){
				tip = prop.getTooltipText();
				tooltip.setText(formatToolTipText(tip));
				Tooltip.install(this, tooltip);
				pause.playFromStart();
			}
		}
		if ((shape !=null )||(prop != null)){
			setCursor(Cursor.HAND);
			cursorState = Cursor.HAND;
		}
		else {
			cursorState = Cursor.DEFAULT;
			setCursor(Cursor.DEFAULT);
		}
	}

	/**
	 * method to convert HTML-like content into JavaFX Tooltip styled text
	 * @param html
	 * @return String
	 */
	private String formatToolTipText(String html) {
		tooltip.setFont(new Font("Dialog", 12));
		tooltip.setStyle("-fx-background-color: #cedef7; -fx-text-fill: #000000;");

		return html.replaceAll("<html>", "")
				.replaceAll("</html>", "")
				.replaceAll("<b>", "")
				.replaceAll("</b>", "")
				.replaceAll("<br>", "\n")
				.replaceAll("<ul>", "")
				.replaceAll("</ul>", "")
				.replaceAll("<li>", "\u2022 ")
				.replaceAll("</li>", "\n");
	}

	/**
	 * method to check if the mouse is inside a rectangle
	 * @param mouseX
	 * @param mouseY
	 * @param rectX
	 * @param rectY
	 * @return boolean
	 */
	private boolean isInsideRect(double mouseX, double mouseY, double rectX, double rectY) {
		return mouseX >= rectX - (rectX/2) && mouseX <= rectX + (rectX/2) &&
				mouseY >= rectY - (rectY/2) && mouseY <= rectY + (rectY/2);
	}

	/**
	 * translates point to current zoom factor
	 * @param p
	 * @return Point
	 */

	private Point2D translatePoint(Point2D p){
		/*
		 *  when scaling positions get messed up
		 *  so to keep actions as in a 1,1 ratio
		 *  i need to scale down event points
		 */
		return  new Point2D ((int) (p.getX()/factor), (int) (p.getY()/factor));

	}

	private Rectangle2D getVisibleRect() {
		// Implement this method to return the visible rectangle
		return new Rectangle2D(0, 0, getWidth(), getHeight());
	}

	private VisObjectProperty movedOnVisPropertyDescription(int x, int y){
		for ( Entry<String, VisObjectProperty> entry : visGraph.propertyMap.entrySet()){
			if (entry.getValue().onProperty(new Point2D(x,y)))
				return entry.getValue();
		}
		return null;
	}

	/*
	 *  RUNNABLE METHODS
	 */

	public void run() {
		Thread me = Thread.currentThread();
		while (relaxer == me) {
			relax();
			try {
				Thread.sleep(stable ? 500 : 300);
				while (pressedShape!=null){
					Thread.sleep(400);
				}
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	public void start() {
		relaxer = new Thread(this);
		relaxer.start();
	}

	public void stop() {
		relaxer = null;
	}

	public void handleMouseClicked(MouseEvent e) {

		Point2D p = translatePoint(new Point2D(e.getX(), e.getY()));
		int x = (int) p.getX();
		int y = (int) p.getY();
		if (clickedOnShape(x, y,e))          return;
		if (clickedOnClosePropertyBox(x, y)) return;
		if (e.getButton()==MouseButton.SECONDARY){
			if (menuVisGeneralContext != null){
				closeContextMenu(menuVisGeneralContext);
			}
			showContextMenu((int) e.getScreenX(), (int) e.getScreenY());
		}
		draw();
	}


	private boolean clickedOnShape(int x, int y,MouseEvent e){
		if (visGraph == null) {
			return false;
		}
		Shape shape = visGraph.findShape(new Point2D(x, y));
		if (shape!=null) {
			if (e.getClickCount() == 2 && !e.isConsumed() && e.getButton() == MouseButton.PRIMARY) {
				//double click
				e.consume();
				if (shape.allSubHidden()){
					shape.hide();
					return true;
				}
			}
			else {
				switch (e.getButton()) {

					//if right click on the figure
					case SECONDARY :
						if (menuVisShapeContext != null){ closeContextMenu(menuVisShapeContext); }
						if (menuVisGeneralContext != null){ closeContextMenu(menuVisGeneralContext); }
						showContextMenu(shape,e);
						break;
					case PRIMARY :
						//Click on the open symbol
						if (pressedOpen(shape, x, y,e)){
							if(shape.getState()== Shape.CLOSED || shape.getState()== Shape.PARTIALLY_CLOSED){
								//si estaba cerrado el nodo [+] abrirlo
								shape.open();
								refreshDashedConnectors();
							}
						}
						//Click on the close symbol
						else if (pressedClose(shape, x, y, e)){
							if(shape.getState()== Shape.OPEN || shape.getState()== Shape.PARTIALLY_CLOSED){
								//if [-] clicked, close the node
								shape.close();
								refreshDashedConnectors();
							}
						}
						else { // pressed elsewhere on the shape
							paintFrame.focusOnShape(null,shape);
							break;
						}
				}
				//notify graphObserver
				getVisGraph().updateObservers(VisConstants.GENERALOBSERVER);
				return true;
			}
		}
		else {
			if (menuVisShapeContext != null){ closeContextMenu(menuVisShapeContext); }
			if (menuVisGeneralContext != null){ closeContextMenu(menuVisGeneralContext); }
		}

		//notify graphObserver
		getVisGraph().updateObservers(VisConstants.GENERALOBSERVER);
		return false;
	}

	private  boolean clickedOnClosePropertyBox(int x, int y){
		if (visGraph == null) {
			return false;
		}
		for (Entry<String,Shape> entry : visGraph.shapeMap.entrySet()){
			Shape shape = entry.getValue();
			if ((shape instanceof VisClass) &&(shape.asVisClass().propertyBox!=null)){
				if (shape.asVisClass().onCloseBox(x, y)){
					boolean b = shape.asVisClass().propertyBox.visible;
					shape.asVisClass().propertyBox.setVisible(!b);
					return true;
				}
			}
		}
		return false;
	}

	public void focusOnShape(String shapeKey,Shape pshape) {
		/*
		 * focus the frame on the shape pos
		 * If shapeKey is  null, it will look for it
		 */
		Shape shape = pshape != null ? pshape : visGraph.getShape(shapeKey);

		if (shape!= null) {
			int x  = (int) (shape.getPosX() * factor);
			int y  = (int) (shape.getPosY() * factor);

			double scrollPaneWidth = scroll.getViewportBounds().getWidth();
			double scrollPaneHeight = scroll.getViewportBounds().getHeight();

			double hValue = (x - scrollPaneWidth / 2) / (getWidth() - scrollPaneWidth);
			double vValue = (y - scrollPaneHeight / 2) / (getHeight() - scrollPaneHeight);

			scroll.setHvalue(Math.max(0, Math.min(hValue, 1)));
			scroll.setVvalue(Math.max(0, Math.min(vValue, 1)));
		}

	}

	private boolean pressedOpen(Shape shape,int x, int y,MouseEvent e){
		return (x >= shape.posx + shape.getWidth()/2 + 1 && x <= shape.posx + shape.getWidth()/2 + 10
				&& y >= shape.posy - 10 && y <= shape.posy && !e.isMetaDown());
	}

	private boolean pressedClose(Shape shape,int x, int y,MouseEvent e){
		return x >= shape.posx + shape.getWidth()/2 + 1 && x <= shape.posx + shape.getWidth()/2 + 10
				&& y >= shape.posy + 1 && y <= shape.posy + 10 && !e.isMetaDown();
	}

	public void refreshDashedConnectors(){
		visGraph.clearDashedConnectorList();
		visGraph.addDashedConnectors();
	}

	private void showContextMenu(Shape s,MouseEvent e) {
		double x,y;
		x = e.getScreenX();
		y = e.getScreenY();

		menuVisShapeContext = new VisShapeContext(s,this,e);
		menuVisShapeContext.show(this,x,y);
    }

	private void closeContextMenu(ContextMenu menu){
		menu.hide();
		menu = null;
	}

	private void showContextMenu(int x, int y){
		menuVisGeneralContext = new VisGeneralContext(this);
		menuVisGeneralContext.show(this,x,y);
    }

	public VisGraph getVisGraph() {return visGraph;}


	private Shape getUpperShape(int index,ArrayList<Shape> list){
		for (int z = index-1;z>=0;z--){
			if (list.get(z).isVisible())
				return list.get(z);
		}
		return null;

	}
	private Shape getLowerShape(int index,ArrayList<Shape> list){
		for (int z = index+1;z<list.size();z++){
			if (list.get(z).isVisible())
				return list.get(z);
		}
		return null;
	}
	private void shapeRepulsion(Shape repellingShape, int direction){
		if (repulsion){
			VisLevel currentLevel = repellingShape.getVisLevel();
			ArrayList<Shape> orderedList = currentLevel.orderedList();
			int repellingIndex = orderedList.indexOf(repellingShape);
			switch (direction){
				case UP:
					if (repellingIndex> 0) {
//						Shape upperShape = orderedList.get(repellingIndex-1);
						Shape upperShape = getUpperShape(repellingIndex, orderedList);
						if (upperShape ==null) //it's the visible upper shape
							return;
						int upperShapeHeight = upperShape.getHeight();
						if ((upperShape instanceof VisClass)&&(upperShape.asVisClass().propertyBox!=null))
							upperShapeHeight +=  upperShape.asVisClass().getTotalHeight();
						if (repellingShape.getPosY()< (upperShape.getPosY()+upperShapeHeight+MIN_Y_SEP)) {
							upperShape.setPosY(upperShape.getPosY()-upperShape.getHeight()/2);
//							repaint();
						}
						shapeRepulsion(upperShape, direction);
					}
					break;


				case DOWN:
					if (repellingIndex < orderedList.size()-1) {
//						Shape lowerShape = orderedList.get(repellingIndex+1);
						Shape lowerShape = getLowerShape(repellingIndex, orderedList);
						if (lowerShape ==null) //it's the visible upper shape 
							return;
						int z = repellingShape.getHeight();
						if ((repellingShape instanceof VisClass)&& (repellingShape.asVisClass().propertyBox!=null))
							z = repellingShape.asVisClass().getTotalHeight();
						if (repellingShape.getPosY()+z > lowerShape.getPosY()-lowerShape.getHeight()-MIN_Y_SEP) {
							lowerShape.setPosY(lowerShape.getPosY()+lowerShape.getHeight()/2);
//							repaint();
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

		if (getKceOption().equals(VisConstants.KCECOMBOOPTION1)){ //"None"
			getVisGraph().showAll();
		}
		if (getKceOption().equals(VisConstants.KCECOMBOOPTION2)){ //"KCE10"
			getVisGraph().showAll();
			KConceptExtraction.hideNonKeyConcepts(activeOntology, this.getVisGraph(), 10);
		}
		if (getKceOption().equals(VisConstants.KCECOMBOOPTION3)){ //"KCE20"
			getVisGraph().showAll();
			KConceptExtraction.hideNonKeyConcepts(activeOntology, this.getVisGraph(), 20);
		}
	}



	public void applyStructuralReduction(){
//		StructuralReducer.customApplyStructuralReduction(getOntology(),getVisGraph().getShapeMap());
		StructuralReducer.applyStructuralReduction(getOntology());
	}

	public boolean doApplyReductionCheck(){
		if (getOntology() != null){
			applyStructuralReduction();
			return true;
		}
		return false;
	}
	public void setParentFrame(Embedable pemb) {parentframe = pemb;}
	public Embedable getParentFrame() {return parentframe;}


}


