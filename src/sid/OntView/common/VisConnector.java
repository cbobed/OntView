package sid.OntView.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;

public abstract class VisConnector {
    Shape from,to;
    Point fromPoint,toPoint;
    public static Color altColor = Color.lightGray;
    public static Color color = Color.BLACK;
    boolean visible = true;
    boolean redundant = false;
	protected double controlx1;
	protected double controly1;
	protected double controlx2;
	protected double controly2;
    static BasicStroke stroke;
    static BasicStroke minStroke;
    static float  width  = 1.0f;
    
    public VisConnector(){}
    
    public VisConnector(Shape par_from, Shape par_to){
    	from = par_from;
    	to = par_to;
    	stroke = new BasicStroke(width);
    	minStroke = new BasicStroke((float) 1.0);
    }
    
    public void setRedundant(){
    	redundant = true;
    }
    
    public boolean isRedundant(){ return redundant;}
    /**
     * graphics.drawLine with an arrow
     */
    public static void drawArrow(Graphics g,int x0,int y0,int x1,int y1){
		double alfa=Math.atan2(y1-y0,x1-x0);
		g.drawLine(x0,y0,x1,y1);
		int k=3;
		int xa=(int)(x1-k*Math.cos(alfa+1));
		int ya=(int)(y1-k*Math.sin(alfa+1));
		// Se dibuja un extremo de la dirección de la flecha.
		g.drawLine(xa,ya,x1,y1); 
		xa=(int)(x1-k*Math.cos(alfa-1));
		ya=(int)(y1-k*Math.sin(alfa-1));
		// Se dibuja el otro extremo de la dirección de la flecha.
		g.drawLine(xa,ya,x1,y1); 
		}
    
    public abstract void draw (Graphics g);

    public static void removeConnector(ArrayList<VisConnector> list ,Shape origin,Shape dst){
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
    
   public static boolean isInArray(ArrayList<VisConnector> list,Shape or, Shape dst){
	   for (VisConnector c : list){
		   if ((c.from == or) && (c.to == dst))
			   return true;
	   }
	   return false;
   }
   
   public void hide (){visible = false;}
   public void show (){visible = true;}
   
   public static VisConnector getConnector(ArrayList<? extends VisConnector>list, Shape s1,Shape s2){
	   for (VisConnector c : list){
		   if ((c.from == s1)&&(c.to==s2)){
		      return c;
		   }
	   }
	   return null;
   }
  

	public static double distance(Point p1,Point p2) {
		return  Math.sqrt(Math.pow((p1.x-p2.x),2) + Math.pow((p1.y-p2.y),2));	
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
		

		final int MINDIF = 50;
		double xdiff = toPointX-fromPointX;
//		double ydiff = toPointY-toPointY;
	    if  (xdiff > 0){
	    	controlx1 = fromPointX + 0.1 * (xdiff);
	    	controly1 = fromPointY;
	    	controlx2 = fromPointX + 0.4 * (xdiff);
	    	controly2 = toPointY;
        }
	    
	    else {

	    	controlx1 = toPointX + 0.4 * (-1.0) * (xdiff);
	    	controly1 = fromPointY;
	    	controlx2 = toPointX + 0.1 * (-1.0) * (xdiff);
	    	controly2 = toPointY;
        }
	    
	
	}
	
	
	
	protected void setPath(GeneralPath path,double fromPointX, double fromPointY, double toPointX,double toPointY){
	/*
	 *  adds Points to a path to be drawn
	 */
		path.reset();
		calculateBezierPoints(fromPointX, fromPointY, toPointX, toPointY);
		path.moveTo(fromPointX, fromPointY);
		path.lineTo(fromPointX, fromPointY);
		path.curveTo(controlx1, 
		   		     controly1,
		   		     controlx2,
		   		     controly2,
                      toPointX, 
                      toPointY
		    		     );
	   path.lineTo(toPointX, toPointY);
		
	}
	

	protected void drawCurve(Graphics2D g2d, int method) {};
   
   
   
}