
package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

public abstract class Shape{
	
	public static final int CLOSED = 0 ;
	public static final int OPEN = 1 ;
	public static final int PARTIALLY_CLOSED = 2 ;
	
	public int posx,posy;
	private int height,width;
	int depthlevel;
	VisLevel vdepthlevel;
	VisGraph graph;
	Point2D connectionPointsL;
	Point2D connectionPointsR;
	ArrayList<VisConnector> inConnectors,
							outConnectors,
	                        inDashedConnectors,
	                        outDashedConnectors;

	//when showing i need to keep track of those that were closed
	ArrayList<Shape> hiddenSubClasses;
	
	
	int state = OPEN;
	boolean wasOpened = true;
	boolean visible = true;
	boolean wasVisible = true;
	boolean selected = false;
	boolean moved = false;
   
	private int getZoomLevel(){return 1;}
	public boolean isVisible(){ return visible;}
	public void setVisible(boolean b){visible = b;}
	public int  getPosX(){return posx;}
	public int  getPosY(){return posy;}
	public int  getHeight(){return height*getZoomLevel();}	
	public int  getTotalHeight(){return height*getZoomLevel();}
	public int  getWidth(){return width;}
	public void setPosX(int x) {posx = x;}
	public void setPosY(int x) {posy = x;}
	public void setHeight(int x) {height = x;}
	public void setWidth(int x) {width = x;}
	public VisClass asVisClass(){return (VisClass)this;}
	public void setState(int pstate){state= pstate;}
	public int  getState() {return state;}
	public abstract OWLClassExpression getLinkedClassExpression();
	public abstract String getToolTipInfo();
	public abstract int getLevelRelativePos();
	public abstract void drawShape(GraphicsContext g);
	public abstract Point2D getConnectionPoint(Point2D point, boolean b);
    PaintFrame frame;

    /**************************************************************/
	public Shape(){
		inConnectors = new ArrayList<VisConnector>();
		outConnectors = new ArrayList<VisConnector>();
		outDashedConnectors = new ArrayList<VisConnector>();
		inDashedConnectors = new ArrayList<VisConnector>();
		hiddenSubClasses = new ArrayList<Shape>();
	}

	public void setPosition(int x, int y) {
		this.posx = x;
		this.posy = y;
	}

	/** 
	 * Marks as closed and hides subLevels 
	 * Then looks for those remaining visible nodes and adds a reference (dashed line)
	 */
	public void close (){
		setState(CLOSED);
        hideSubLevels(this);
	}
    
	/**
	 * hides outconnectors and checks if children need to be hidden 
	 * @param closedShape
	 */
	private void hideSubLevels(Shape closedShape){
	// hides outconnectors and 
    // checks if children need to be hidden 
    // if so, it hides it
		
		Shape child;
		for (VisConnector connector : outConnectors) {
			child =  connector.to;
			connector.hide();
			child.checkAndHide(closedShape);
		}
	}
	/** 
	 *  Checks references
	 *  Before setting invisible a shape we need to check if there's still 
	 *  any reference ( an in Connector)
	 * @param closedShape
	 */
	public void checkAndHide(Shape closedShape){
		if (getVisibleInReferences()==0) {
		   this.visible = false;
		   hideSubLevels(closedShape);
		   return;
		}   
	
		hideSubLevels(closedShape);
	}
	
	/**
	 *  hides shape, connector and notifies parents
     */
	public void hide (){

		 this.visible = false;
		 for (VisConnector c : inConnectors) {
			 c.hide();
			 //notify Parent
			 c.from.notifyHidden(this);       
		 }
		 for (VisConnector c : outConnectors) {
			 c.hide();       
		 }
		 //Wakes observer thread on hide event
		graph.updateObservers(VisConstants.GENERALOBSERVER);
		graph.getDashedConnectorList().clear();
		graph.addDashedConnectors();
	}
	
	private int getVisibleInReferences(){
		int count = 0;
		for (VisConnector c : inConnectors){
			if (c.visible) {
				count++; 
			}
		}
		return count;
	}
	
	/**
	 * this is called by a hidden node
	 * it notifies parents that it's hidden	
	 */
	private void notifyHidden(Shape s){

		 if (!(this instanceof VisConstraint)) {
	    	 if (allSubHidden()){ 
				setState(CLOSED);
	         }
		 	 else {
			    setState(PARTIALLY_CLOSED);	
		     }
		 }
		 else {
			 for (VisConnector c : inConnectors) {
				 if (allSubHidden()){
					 this.visible = false;
				 }
				 c.hide();
				 //notify Parent
				 c.from.notifyHidden(this);       
			 }
		 }
//	     this.hiddenSubClasses.add(s); 
	 }		 
	/**
	 * @return all subclasses hidden or not
	 */
	public boolean allSubHidden (){
		//if all subclasses are hidden
		for (VisConnector c : this.outConnectors){
			if (c.visible) 
			return false;
		}	
		return true;
	}		 
	
	/**************************************************************/
	
	public void open (){
        setState(OPEN);
        hiddenSubClasses.clear();
        showSubLevels();
	}
	
	public void show(Shape parent){
		this.visible = true;
		
		switch (getState()) {
			case CLOSED : 
	   			break;
	   			
		   	case OPEN :
		   	    for (VisConnector connector : outConnectors) {
				   connector.show();
			       connector.to.show(this);
			    }
		   	    break;
		  
		   	case PARTIALLY_CLOSED :
		   	    for (VisConnector connector : outConnectors) {
		   	       //if its not a previously hidden node we'll show it	
				   if (!hiddenSubClasses.contains(connector.to)){
					  connector.show();
					  connector.to.show(this);
				   }
			 } 
	    }
	}
	
	public void showSubLevels(){
		for (VisConnector c : outConnectors) {
           c.to.show(this);
           c.show();
		}	
	}
	
	public void setVisLevel(VisLevel v){
		
		v.addShape(this);
		if (v.width <  width)
			v.width = width + VisLevel.MIN_WIDTH;
		
	}
	public VisLevel getVisLevel(){
	    return	vdepthlevel;
	}
	

    public int stateMapping (String stringVal){
    	if (stringVal.equals("closed"))
    		return CLOSED;
    	else if (stringVal.equals("open"))
    		return OPEN;
    	else if (stringVal.equals("partClosed"))
    		return PARTIALLY_CLOSED;
    	return OPEN;
    }
	
	/**
     * Inverts lookup in the shapeMap by returning the key out of an owlclassexpression
	 * @param e
	 * @return
	 */
 	 public static String getKey(OWLClassExpression e){	
    	if (e instanceof OWLClass) {
    		return e.asOWLClass().getIRI().toString(); }
  	    else { 
    	    return e.toString();
    	}    
     }
		
		
	 public static final Comparator<Shape> POSY_ORDER = 
            new Comparator<Shape>() {
			public int compare(Shape s1, Shape s2) {
			return s1.getPosY()-s2.getPosY();
		 }
	 };
    
	
	
}
