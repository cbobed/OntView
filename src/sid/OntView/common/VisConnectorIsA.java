package sid.OntView.common;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;

public class VisConnectorIsA extends VisConnector {
	
	Color isaColor = null;
	
	private static double PATH_OFFSET = 5.0;
	
	GeneralPath path;
	

	public VisConnectorIsA(Shape par_from, Shape par_to) {
		super(par_from, par_to);
		isaColor = VisConnector.color;
		path = new GeneralPath();
	}

	@Override
	public void draw(Graphics g){
		Graphics2D g2d= (Graphics2D) g;
		Stroke prevStroke = g2d.getStroke();
		if (visible){
			if ((from != null) && (to!= null)){
				fromPoint = from.getConnectionPoint(new Point(from.getPosX(),from.getPosY()),false);
				toPoint   = to.getConnectionPoint(new Point(from.getPosX(),from.getPosY()),true);

			    Shape selected = from.graph.paintframe.getPressedShape();

			    Color prevColor = g2d.getColor();
			    if (selected !=null) {
			    		g2d.setStroke(stroke);
			    		if ((from==selected)||(to==selected)){
						   	g2d.setColor(Color.orange);
						}
					    else  {
					    	g2d.setColor(isaColor);
					    	g2d.setStroke(minStroke);
					    }
			    }	
			    else {
			        Stroke str = (redundant ? minStroke : stroke);
			        Color col  = (redundant ? altColor : isaColor);
			    	g2d.setColor(col);
		    		g2d.setStroke(str);
			    }

			    //drawing a curve path
			    drawCurve(g2d, VisConstants.NURB);
			   
			    this.drawArrow(g2d, 5, PATH_OFFSET, fromPoint.getX(), fromPoint.getY());
			    
			    g2d.setColor(prevColor);
			 	g2d.setStroke(prevStroke);
			}
		}	
	}
	
	
	
	
	@Override
	protected void drawCurve(Graphics2D g2d,int method){
		switch (method){
			case VisConstants.BEZIER:
				drawBezier(g2d);
			    break;
			case VisConstants.NURB:
				drawNurbs(g2d, fromPoint, toPoint);
		
		}
		
		
	}
	
	protected void drawBezier(Graphics2D g2d){
	    setPath(path,fromPoint.getX(), fromPoint.getY(), toPoint.getX(),toPoint.getY());
	    g2d.draw(path);
	}
	
	protected void drawNurbs(Graphics2D g2d, Point pfrom, Point pto){
		CubicCurve2D curve2 = null; 
		double auxX1 = 0.2* (pto.getX()-pfrom.getX()) + pfrom.getX();
        double auxX2 = 0.4* (pto.getX()-pfrom.getX()) + pfrom.getX();
		
		curve2 = new CubicCurve2D.Float(); 
		curve2.setCurve(pfrom.getX(),pfrom.getY(), 
						auxX1,pfrom.getY(),
						auxX2,pto.getY(), 
						pto.getX(),pto.getY()); 
		g2d.draw(curve2); 
	}
	
	
	protected void calculateBezierPoints(double fromPointX, double fromPointY, double toPointX,double toPointY){
	/*
	 * calculates intermediate points (x1,y1) (x2,y2) for a Bezier curve
	 */
		double heightDiff = Math.abs(toPointY -fromPointY);
	    if (toPoint.y -fromPoint.y > 0){
	    	controlx1 = fromPointX + 0.2*(toPointX-fromPointX);
	    	controly1 = fromPointY + 0.8*(heightDiff);
	    	controlx2 = fromPointX + 0.8*(toPointX-fromPointX);
	    	controly2 = fromPointY + 0.9*(heightDiff);
        }
	    else {
	    	controlx1 = fromPoint.x + 0.2*(toPointX-fromPointX);
	    	controly1 = fromPoint.y + (-0.8)*(heightDiff);
	    	controlx2 = fromPoint.x + 0.8*(toPointX-fromPointX);
	    	controly2 = fromPoint.y + (-0.9)*(heightDiff);	
	    }
	}
	
	@Override
	protected void setPath(GeneralPath path,double fromPointX, double fromPointY, double toPointX,double toPointY){
	/*
	 *  adds Points to a path to be drawn
	 */
		path.reset();
		calculateBezierPoints(fromPointX, fromPointY, toPointX, toPointY);
		path.moveTo(fromPointX, fromPointY);
		path.lineTo(fromPointX+PATH_OFFSET, fromPointY);
		path.curveTo(controlx1, 
		   		     controly1,
		   		     controlx2,
		   		     controly2,
                      toPointX-PATH_OFFSET, 
                      toPointY
		    		     );
	   path.lineTo(toPointX, toPointY);
		
	}
	
	private void drawArrow(Graphics2D g,int height,double pATH_OFFSET2,double d,double e){
	    int[] xPoints = { (int) (d+pATH_OFFSET2),  (int) (d+pATH_OFFSET2),  (int) d};
	    int[] yPoints = { (int) (e-height), (int) (e+height), (int) e}; 
	    g.fillPolygon(xPoints, yPoints, 3);
		
	}
	
}
