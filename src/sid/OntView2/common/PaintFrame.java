package sid.OntView2.common;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.xml.sax.SAXException;
import reducer.StructuralReducer;
import sid.OntView2.utils.ProgressBarDialogThread;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;


public class PaintFrame extends JPanel implements MouseListener,Runnable,MouseMotionListener{
    
	
	private static final long serialVersionUID = 1L;
    public JScrollPane scroll;
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
	Dimension	   prevSize;
	PaintFrame     paintFrame = this;
    OWLOntology    activeOntology;
    private String activeOntologySource;
//    boolean        reduceCheck = false;
    
    OWLReasoner    reasoner;
    VisGraph visGraph,oVisGraph,rVisGraph; //visGraph will handle both depending on which is currently selected
//    public boolean reduceChecked = false;
	
    
    public boolean isStable(){return stable;}
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
    		this.setDoubleBuffered(true);
    		setSize(new Dimension (800,600));
    		prevSize = getSize();
			VisConfig.getInstance().setConstants();
		} 
		catch (Exception e) {
			e.printStackTrace();
		} 
    }
    
    /*-*************************************************************
     * Scaling  issues
     *-*************************************************************/
    
    double 		factor = 1.0;
    double 		prevFactor = 1.0;
    Dimension 	oSize;
    
    /**
     * sets scaling/zoom factor
     *
     */
    public void setFactor(double d){factor = d;}
    public void setOriginalSize(Dimension in){ oSize = in;}
    /**
     * scales by factor and adjusts panel size
     * @param factor
     * @param size
     */
    public void scale(double factor,Dimension size){
    	
    	Graphics2D g2d = (Graphics2D) this.getGraphics();
		g2d.scale(factor, factor);
		if (factor>1.0)
			setPreferredSize(new Dimension( (int)(size.getWidth()*factor),
										(int)(size.getHeight()*factor)));
    }
    
    /*-*************************************************************/
    
	@Override
    public  void  paintComponent(Graphics g) {
         super.paintComponent(g);
         Graphics2D  g2d = (Graphics2D) g;
         if ((factor !=1.0)&&(stable)){
        	 g2d.scale(factor, factor);
         }
         if (prevFactor!=factor){
        	    prevFactor = factor;
        	    if ((factor >=1.0) && (getSize() != prevSize)){
        	    	prevSize = getSize();
        	    	setSize(new Dimension( (int)(oSize.getWidth()*factor),
        	    						   (int)(oSize.getHeight()*factor)));
        			revalidate();	
        	         
        	    }	
        }
        if (visGraph!=null){
             for (VisConnector c : visGraph.connectorList){
            	 c.draw(g);
             }
             for (VisConnector c : visGraph.dashedConnectorList){
      	       	 c.draw(g);
             }      	
	         g.setColor(Color.LIGHT_GRAY);
        	 for (VisLevel lvl : visGraph.levelSet){
        		 g2d.drawLine(lvl.getXpos(), 0, lvl.getXpos(), (int) (getHeight()/factor));
        		 //Uncomment this to get a vertical line in every level
        		 g.setColor(Color.LIGHT_GRAY);

        	 }
        	 g.setColor(Color.BLACK);
        	 drawPropertyBoxes(g2d);
    	 	 for (Entry<String, Shape> entry : visGraph.shapeMap.entrySet()){
        		 entry.getValue().drawShape(g2d);
        	 }
        	 
         }
    }
	
	
	
	private void drawPropertyBoxes(Graphics2D g2d){
	   	 for (Entry<String, Shape> entry : visGraph.shapeMap.entrySet()){
	   		 if (entry.getValue() instanceof VisClass){
     	   		 FontMetrics fm = g2d.getFontMetrics();
	   			 VisClass v = entry.getValue().asVisClass();
	   			 if ((v.getPropertyBox()!=null) && (v.getPropertyBox().visible==true)) {
				    	Font c = g2d.getFont();
				    	g2d.setColor(Color.BLACK);
				    	v.getPropertyBox().draw(g2d,fm);
				    	g2d.setFont(c);
				}  
	   		}
	   	}
	}   	 
	
	
	public  void createReasonedGraph(HashSet<OWLClassExpression> set,boolean check) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException{
       rVisGraph = new VisGraph(this);
       
	   visGraph = rVisGraph; 
	   paintFrame.revalidate();
	   stable = false;
	   visGraph.setActiveOntology(activeOntology);
//	   applyStructuralReduction();
	   visGraph.setOWLClassExpressionSet(set);
	   visGraph.setCheck(check);
	   visGraph.setReasoner(reasoner);	   
	   
	   new Thread(visGraph).start();
	   paintFrame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
	   
	   visGraph.addObserver(new ProgressBarDialogThread(this));

	   //visGraph.buildReasonedGraph(activeOntology, reasoner, set,check);
	   
	   VisGraphObserver graphObserver = new VisGraphObserver(this.getVisGraph());
//	   graphObserver.start();
	   visGraph.addObserver(graphObserver);
	   
//	   
	   stable       = true;
	   stateChanged = true;
//	   relax();
	   factor = 1.0;
	   removeMouseListener(this);
	   removeMouseMotionListener(this);
       addMouseListener(this);
	   addMouseMotionListener(this);
	   setAutoscrolls(true);
	   paintFrame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
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
	public void setStateChanged(boolean b){stateChanged = b;}
	public boolean stateChanged(){
		return stateChanged;
	}
    /**
    * Updates node positions
    * Avoids shapes being on top of each other
    * updates y coord until there's no overlap
	**/
	synchronized void relax() {
    
      // repulsion and atraction between nodes
      Shape shape_j,s_i;
      if (!stable) {
    	  
      }   
	  else { //stable

		  if (stateChanged) {
			  
			 stateChanged = false;
			for (Entry<String, Shape> e_i : visGraph.shapeMap.entrySet()){
				for (Entry<String, Shape> e_j: visGraph.shapeMap.entrySet()){
					s_i = e_i.getValue();
					shape_j = e_j.getValue();
					if ((s_i!=shape_j)&&(s_i.visible)){
						if ((s_i.getPosY()< shape_j.getPosY()) &&(s_i.getPosY()+s_i.getTotalHeight())> shape_j.getPosY()){
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
		  repaint();
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
    int cursorState = Cursor.DEFAULT_CURSOR;
	public boolean hideRange = false;
	private Embedable parentframe;
    
  	
//    @Override
//  	public void mouseClicked(MouseEvent e) {}
  	@Override
  	public void mouseEntered(MouseEvent e) {
  		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
  	}
  	@Override
  	public void mouseExited(MouseEvent e) {
  		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
  	}
    
  	@Override
  	public void mousePressed(MouseEvent e) {

  		Point p = translatePoint(e.getPoint());
  		pressedShape= visGraph.findShape(p);
  		
  		if (pressedShape!=null) {
  		    mouseLastY= (int) p.getY();
  		}
  		else {
  			mouseLastX = (int) p.getX();
  			mouseLastY = (int) p.getY();
  			setCursor(new Cursor(Cursor.MOVE_CURSOR));
  		}
  		repaint();
  	}

  	@Override
  	public void mouseReleased(MouseEvent e) {

  		pressedShape=null;
  		repulsion = true;
  		mouseLastY=0;
  		mouseLastX=0;
  		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
  		
  	}
  	
/*
* MOUSEMOTIONLISTENER 
*/
  		
	@Override
	public void mouseDragged(MouseEvent e) {
		int draggedY,draggedX;
		int direction;
		repulsion = !((e.getModifiers() & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK);
		Point p = translatePoint(e.getPoint());
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
			repaint();
		}
		else {
			int scrollx,scrolly;
			if (draggedX < 0) {
				scrollx = (int) (getVisibleRect().x + getVisibleRect().getWidth()  - draggedX*2);
			}
			else {
				scrollx = getVisibleRect().x - draggedX;
			}	
			if (draggedY < 0){ 
				scrolly = (int) (getVisibleRect().y + getVisibleRect().getHeight() - draggedY*2);
			}	
			else {
				scrolly = (int) (getVisibleRect().y - draggedY);
			}	
			Rectangle r = new Rectangle(scrollx, scrolly, 1, 1);
	        scrollRectToVisible(r);

		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		

		Point p = translatePoint(e.getPoint());
		int x = (int) p.getX();
		int y = (int) p.getY();
		VisObjectProperty prop = null;
		Shape shape = visGraph.findShape(p);
		String tip;
		
		if (shape !=null) {
			tip = shape.getToolTipInfo();
			ToolTipManager.sharedInstance().setDismissDelay(15000);
			this.setToolTipText(tip);
		}
		else  {
			prop = movedOnVisPropertyDescription(x, y);
			if (prop != null){
				tip = prop.getTooltipText();
				ToolTipManager.sharedInstance().setDismissDelay(15000);
				this.setToolTipText(tip);
			}
		}
		if ((shape !=null )||(prop != null)){
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			cursorState=Cursor.HAND_CURSOR;	
		}
		else{
			cursorState= Cursor.DEFAULT_CURSOR;
			setCursor(Cursor.getDefaultCursor());
		}
		
		
	}
	
	/**
	 * translates point to current zoom factor
	 * @param p
	 * @return Point
	 */
	
	private Point translatePoint(Point p){
	/*
	 *  when scaling positions get messed up
	 *  so to keep actions as in a 1,1 ratio
	 *  i need to scale down event points
	 */
		return  new Point ((int) (p.x/factor), (int) (p.y/factor));
		
	}
	
	private VisObjectProperty movedOnVisPropertyDescription(int x, int y){
		for ( Entry<String, VisObjectProperty> entry : visGraph.propertyMap.entrySet()){
			if (entry.getValue().onProperty(new Point(x,y)))
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
	
	@Override
    public void mouseClicked(MouseEvent e) {
		
		Point p = translatePoint(e.getPoint());
	    int x = (int) p.getX();
	    int y = (int) p.getY();
	    if (clickedOnShape(x, y,e))          return;
	    if (clickedOnClosePropertyBox(x, y)) return;
	    if (e.getButton()==MouseEvent.BUTTON3){
	    	showContextMenu(e.getX(),e.getY());
	    }
	    repaint();
	}
	
	
	private boolean clickedOnShape(int x, int y,MouseEvent e){
	    Shape shape = visGraph.findShape(new Point(x, y));
	    if (shape!=null) {
		    if (e.getClickCount() == 2 && !e.isConsumed() && e.getButton()==MouseEvent.BUTTON1) {
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
			        case MouseEvent.BUTTON3 :
			        	showContextMenu(shape,e);
			    	    break;
			        case MouseEvent.BUTTON1 :
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
			        	    Rectangle visiblePart = new Rectangle();
			    	    	paintFrame.computeVisibleRect(visiblePart);
				        	paintFrame.focusOnShape(null,shape);
			    	    	break;
				    	}
		    		}
		    	    //notify graphObserver
		    	    getVisGraph().updateObservers(VisConstants.GENERALOBSERVER);
		    		return true;
		    }		
	    }

	    //notify graphObserver
	    getVisGraph().updateObservers(VisConstants.GENERALOBSERVER);
	    return false;
	}
	
  	private  boolean clickedOnClosePropertyBox(int x, int y){
  		for (Entry<String, Shape> entry : visGraph.shapeMap.entrySet()){
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
		
	public void focusOnShape(String shapeKey, Shape pshape) {
	/*
	 * focus the frame on the shape pos
	 * If shapeKey is  null, it will look for it	
	 */
		Shape shape;
		shape = pshape;
		if (shape == null)
			shape = visGraph.getShape(shapeKey);
		if (shape!= null) {
        	Rectangle visible = paintFrame.getVisibleRect(); 
        	int x  = (int) (shape.getPosX()*factor);
        	int y  = (int) (shape.getPosY()*factor);
        	int w  = (int) (shape.getWidth()/2*factor);
        	int h  = (int) (shape.getHeight()/2*factor);
        	int vw = (int) (visible.getWidth());
        	int vh = (int) (visible.getHeight());
        	paintFrame.scrollRectToVisible(new Rectangle(x-w-vw/2, y-h-vh/2,vw,vh));
        }
       
	}
	
	
	private boolean pressedOpen(Shape shape, int x, int y, MouseEvent e){
		return (x >= shape.posx + shape.getWidth()/2 + 1 && x <= shape.posx + shape.getWidth()/2 + 10
                  && y >= shape.posy - 10 && y <= shape.posy && !e.isMetaDown());
	}
	
	private boolean pressedClose(Shape shape, int x, int y, MouseEvent e){
         return x >= shape.posx + shape.getWidth()/2 + 1 && x <= shape.posx + shape.getWidth()/2 + 10
                          && y >= shape.posy + 1 && y <= shape.posy + 10 && !e.isMetaDown();
    }
	
	public void refreshDashedConnectors(){
          visGraph.clearDashedConnectorList();
          visGraph.addDashedConnectors();
	}
	
    private void showContextMenu(Shape s, MouseEvent e) {
    	int x,y;
    	x = e.getX();
    	y = e.getY();
    	
    	VisShapeContext menu = new VisShapeContext(s,this,e);
    	menu.show(this,x,y);
    	return;
    }
   
    private void showContextMenu(int x, int y){
    	VisGeneralContext menu = new VisGeneralContext(this);
    	menu.show(this,x,y);
    	return;
    }

    public VisGraph getVisGraph() {return visGraph;}
	 
    
    private Shape getUpperShape(int index, ArrayList<Shape> list){
    	for (int z = index-1;z>=0;z--){
    		if (list.get(z).isVisible())
    			return list.get(z);
    	}
    	return null;
    	
    }
    private Shape getLowerShape(int index, ArrayList<Shape> list){
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


