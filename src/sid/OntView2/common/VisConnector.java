package sid.OntView2.common;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.*;
import javafx.scene.paint.Color;
import javafx.geometry.Point2D;

import java.util.ArrayList;

public abstract class VisConnector {
    Shape from,to;
    Point2D fromPoint,toPoint;
    public static Color altColor = Color.BLACK;
    public static Color color = Color.BLACK;
    boolean visible = true;
    boolean redundant = false;
	protected double controlx1;
	protected double controly1;
	protected double controlx2;
	protected double controly2;
    static float  width  = 1.0f;
	static float minWidth = 1.0f;
    
    public VisConnector(){}
    
    public VisConnector(Shape par_from, Shape par_to){
    	from = par_from;
    	to = par_to;
    }

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isVisible() {
		return visible;
	}

    public void setRedundant(){
    	redundant = true;
    }
    
    public boolean isRedundant(){ return redundant;}
    
    public abstract void draw (GraphicsContext g);

	public static void removeConnector(ArrayList<VisConnector> list , Shape origin, Shape dst){
	   VisConnector c = null;

	   for (VisConnector con : list){
		   if ((con.from == origin) && (con.to == dst)){
			   c=con;
			   origin.outConnectors.remove(con);
			   dst.inConnectors.remove(con);

			   break;
		   }
	   }
	   if (c!=null){
		   list.remove(c);
	   }
   }
   
   public void hide(){ visible = false; }
   public void show(){ visible = true; }
   
   public static VisConnector getConnector(ArrayList<? extends VisConnector>list, Shape s1, Shape s2){
	   for (VisConnector c : list){
		   if ((c.from == s1)&&(c.to==s2)){
		      return c;
		   }
	   }
	   return null;
   }
   
   
	/**
	 * Calculate Bezier Points for non-directed connectors
	 * @param fromPointX
	 * @param fromPointY
	 * @param toPointX
	 * @param toPointY
	 */
	
	protected void calculateBezierPoints(double fromPointX, double fromPointY, double toPointX,double toPointY){
		

		final int MINDIF = 50;
		double xdiff = toPointX-fromPointX;
		double ydiff = toPointY-toPointY;
		// Destination point is upper and right 
	    if ((toPointY - fromPointY < 0) && (xdiff > 0)){
	    	double c1 = 1.0;
	    	double c2 = 1.0;
	    	double offset = 0.0;
	    	if (Math.abs(xdiff)<MINDIF){
	    		c1 = -3.0;
	    		c2 = -3.0;
	    		offset = 60;
	    	}
	    	controlx1 = fromPointX + 0.1* c1 *(xdiff) + offset;
	    	controly1 = fromPointY + (ydiff);
	    	controlx2 = fromPointX + 0.9* c2 *(xdiff) + offset;
	    	controly2 = fromPointY + (ydiff);	
	
        }
	    
		// Destination point is upper and left
	    else if ((toPointY - fromPointY < 0) && (xdiff <= 0)){
	    	double c1 = 1.0;
	    	double c2 = 1.0;
	    	double offset = 0.0;
	    	if (Math.abs(xdiff)<MINDIF){
	    		c1 = -3.0;
	    		c2 = -3.0;
	    		offset = 60;
	    	}
	    	controlx1 = fromPointX + 0.1* c1 * (xdiff) + offset;
	    	controly1 = fromPointY +     (ydiff);
	    	controlx2 = fromPointX + 0.9* c2 * (xdiff) + offset;
	    	controly2 = fromPointY +     (ydiff);	
        }
	    
		// Destination point is lower and right 
	    else if ((toPointY - fromPointY >= 0) && (xdiff > 0)){
	    	double c1 = 1.0;
	    	double c2 = 1.0;
	    	double offset = 0.0;
	    	if (Math.abs(xdiff)<MINDIF){
	    		c1 = -3.0;
	    		c2 = -3.0;
	    		offset = 60;
	    	}
	    	controlx1 = fromPointX + 0.1* c1 * (xdiff) + offset;
	    	controly1 = fromPointY + (ydiff);
	    	controlx2 = fromPointX + 0.9* c2 * (xdiff) + offset;
	    	controly2 = fromPointY + (ydiff);
        }
	    
		// Destination point is lower and left 
	    else if ((toPointY - fromPointY >= 0) && (xdiff <= 0)){
	    	double c1 = 1.0;
	    	double c2 = 1.0;
	    	double offset = 0.0;
	    	if (Math.abs(xdiff)<MINDIF){
	    		c1     = -3.0;
	    		c2     = -3.0;
	    		offset =  60;
	    	}
	    	controlx1 = fromPointX + 0.1* c1 * (xdiff) + offset;
	    	controly1 = fromPointY + (ydiff);
	    	controlx2 = fromPointX + 0.9* c2 * (xdiff) + offset;
	    	controly2 = fromPointY + (ydiff);	
        }	
	}
	
	
	
	protected void calculateNurbPoints(double fromPointX, double fromPointY, double toPointX,double toPointY){

		double xdiff = toPointX-fromPointX;
	    if  (xdiff > 0) { // Right to left
			controlx1 = fromPointX + 0.1 * (xdiff);
			controly1 = fromPointY;
			controlx2 = fromPointX + 0.4 * (xdiff);
			controly2 = toPointY;
		} else if (xdiff == 0) { // Up to down
			double pseudoXdiff = 50;
			controlx1 = fromPointX - pseudoXdiff;
			controly1 = fromPointY - pseudoXdiff/2;
			controlx2 = toPointX - pseudoXdiff;
			controly2 = toPointY + pseudoXdiff/2;
		} else { // Left to right
	    	controlx1 = toPointX + 0.4 * (-1.0) * (xdiff);
	    	controly1 = fromPointY;
	    	controlx2 = toPointX + 0.1 * (-1.0) * (xdiff);
	    	controly2 = toPointY;
        }


	}

	protected void setPath(Path path, double fromPointX, double fromPointY, double toPointX, double toPointY){
	/*
	 *  adds Points to a path to be drawn
	 */
		path.getElements().clear();
		calculateBezierPoints(fromPointX, fromPointY, toPointX, toPointY);
		path.getElements().add(new MoveTo(fromPointX, fromPointY));
		path.getElements().add(new LineTo(fromPointX, fromPointY));
		path.getElements().add(new CubicCurveTo(
				controlx1, controly1,
				controlx2, controly2,
				toPointX, toPointY
		));
		path.getElements().add(new LineTo(toPointX, toPointY));
	}
	/*protected void setPath2(Path path, double fromPointX, double fromPointY, double toPointX, double toPointY) {
		path.getElements().clear();
		calculateBezierPoints(fromPointX, fromPointY, toPointX, toPointY);

		MoveTo moveTo = new MoveTo(fromPointX, fromPointY);
		path.getElements().add(moveTo);

		CubicCurveTo cubicCurveTo = new CubicCurveTo(
				controlx1, controly1,
				controlx2, controly2,
				toPointX, toPointY
		);
		path.getElements().add(cubicCurveTo);
		path.getElements().add(new MoveTo(toPointX, toPointY));
	}*/



	protected void drawCurve(GraphicsContext g2d, int method) {};


	protected void drawPath(GraphicsContext gc, Path path) {
		gc.beginPath();
		for (PathElement element : path.getElements()) {
			if (element instanceof MoveTo moveTo) {
				gc.moveTo(moveTo.getX(), moveTo.getY());
			} else if (element instanceof LineTo lineTo) {
				gc.lineTo(lineTo.getX(), lineTo.getY());
			} else if (element instanceof CubicCurveTo cubicCurveTo) {
				gc.bezierCurveTo(
						cubicCurveTo.getControlX1(), cubicCurveTo.getControlY1(),
						cubicCurveTo.getControlX2(), cubicCurveTo.getControlY2(),
						cubicCurveTo.getX(), cubicCurveTo.getY()
				);
			} else if (element instanceof QuadCurveTo quadCurveTo) {
				gc.quadraticCurveTo(
						quadCurveTo.getControlX(), quadCurveTo.getControlY(),
						quadCurveTo.getX(), quadCurveTo.getY()
				);
			} else if (element instanceof ClosePath) {
				gc.closePath();
			}
		}
		gc.stroke();
	}
   
}