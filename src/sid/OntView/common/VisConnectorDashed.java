package sid.OntView.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.GeneralPath;

public class VisConnectorDashed extends VisConnectorIsA {

	float dash[] = { 4.0f };
    public static Color color = Color.BLACK;
    static double frac = 0.01;

	public VisConnectorDashed(Shape par_from, Shape par_to) {
		super(par_from, par_to);
	}
	
	public void draw(Graphics g){
		Graphics2D g2d= (Graphics2D) g;
		if (visible){
			BasicStroke prevStroke = (BasicStroke) g2d.getStroke();
			fromPoint = from.getConnectionPoint(new Point(from.getPosX(),from.getPosY()),false);
			toPoint   = to.getConnectionPoint(new Point(from.getPosX(),from.getPosY()),true);
		    g2d.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER, 5.0f, dash, 0.0f));
		    Color prevColor = g2d.getColor();
		  	g2d.setColor(color);
		  	drawCurve(g2d, VisConstants.NURB);
		  	g2d.setStroke(prevStroke);
		 	g2d.setColor(prevColor);
		}	
	}
	
//	protected void setPath(GeneralPath path,double fromPointX, double fromPointY, double toPointX,double toPointY){
//	/*
//	 *  adds Points to a path to be drawn
//	 */
//		path.reset();
//		calculateBezierPoints(fromPointX, fromPointY, toPointX, toPointY);
//		path.moveTo(fromPointX, fromPointY);
//		path.lineTo(fromPointX+VisConstants.CONNECTOROFFSET, fromPointY);
//		path.curveTo(controlx1, 
//		   		     controly1,
//		   		     controlx2,
//		   		     controly2,
//                      toPointX-VisConstants.CONNECTOROFFSET, 
//                      toPointY
//		    		     );
//	   path.lineTo(toPointX, toPointY);
//		
//	}
//	
//	private void drawArrow(Graphics2D g,int height,double pATH_OFFSET2,double d,double e){
//	    int[] xPoints = { (int) (d+pATH_OFFSET2),  (int) (d+pATH_OFFSET2),  (int) d};
//	    int[] yPoints = { (int) (e-height), (int) (e+height), (int) e}; 
//	    g.fillPolygon(xPoints, yPoints, 3);
//		
//	}
//	
//	protected void calculateBezierPoints(double fromPointX, double fromPointY, double toPointX,double toPointY){
//	/*
//	 * calculates intermediate points (x1,y1) (x2,y2) for a Bezier curve
//	 */
//		double heightDiff = Math.abs(toPointY -fromPointY);
//	    if (toPoint.y -fromPoint.y > 0){
//	    	controlx1 = fromPointX + 0.2*(toPointX-fromPointX);
//	    	controly1 = fromPointY + 0.8*(heightDiff);
//	    	controlx2 = fromPointX + 0.8*(toPointX-fromPointX);
//	    	controly2 = fromPointY + 0.9*(heightDiff);
//        }
//	    else {
//	    	controlx1 = fromPoint.x + 0.2*(toPointX-fromPointX);
//	    	controly1 = fromPoint.y + (-0.8)*(heightDiff);
//	    	controlx2 = fromPoint.x + 0.8*(toPointX-fromPointX);
//	    	controly2 = fromPoint.y + (-0.9)*(heightDiff);	
//	    }
//	}
	
}
