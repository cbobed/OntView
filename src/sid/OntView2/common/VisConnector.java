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
	protected double controlX1;
	protected double controlY1;
	protected double controlX2;
	protected double controlY2;
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
	 */
	protected void calculateBezierPoints(double fromPointX, double fromPointY, double toPointX,double toPointY){

        double xDiff = toPointX - fromPointX;
		double yDiff = 0.0;

		double c1 = 1.0;
		double c2 = 1.0;
		double offset = 0.0;

		// Destination point is upper and right 
	    if (((toPointY - fromPointY < 0) && (xDiff > 0)) ||
				((toPointY - fromPointY < 0) && (xDiff <= 0)) ||
				((toPointY - fromPointY >= 0) && (xDiff > 0)) ||
				((toPointY - fromPointY >= 0) && (xDiff <= 0))){

	    	controlX1 = fromPointX + 0.1 * c1 * (xDiff) + offset;
	    	controlY1 = fromPointY + (yDiff);
	    	controlX2 = fromPointX + 0.9 * c2 * (xDiff) + offset;
	    	controlY2 = fromPointY + (yDiff);
	
        }
	}
	
	protected void calculateNURBPoints(double fromPointX, double fromPointY, double toPointX, double toPointY){

		double xDiff = toPointX-fromPointX;
	    if  (xDiff > 0) { // Right to left
			controlX1 = fromPointX + 0.1 * (xDiff);
			controlY1 = fromPointY;
			controlX2 = fromPointX + 0.4 * (xDiff);
			controlY2 = toPointY;
		} else if (xDiff == 0) { // Up to down
			double pseudoXDiff = 50;
			controlX1 = fromPointX - pseudoXDiff;
			controlY1 = fromPointY + 0.5 * pseudoXDiff;
			controlX2 = toPointX - pseudoXDiff;
			controlY2 = toPointY - 0.5 * pseudoXDiff;
		} else { // Left to right
	    	controlX1 = toPointX + 0.4 * (-1.0) * (xDiff);
	    	controlY1 = fromPointY;
	    	controlX2 = toPointX + 0.1 * (-1.0) * (xDiff);
	    	controlY2 = toPointY;
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
            controlX1, controlY1,
            controlX2, controlY2,
				toPointX, toPointY
		));
		path.getElements().add(new LineTo(toPointX, toPointY));
	}

	protected void drawCurve(GraphicsContext g2d, int method) {}


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