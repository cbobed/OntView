package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

public class VisConnectorDashed extends VisConnectorIsA {

	double[] dash = { 4.0f };
	public static Color color = Color.BLACK;
	static double frac = 0.01;

	public VisConnectorDashed(Shape par_from, Shape par_to) {
		super(par_from, par_to);
	}

	public void draw(GraphicsContext g){
		GraphicsContext g2d= (GraphicsContext) g;
		if (visible){
			// Save the previous stroke and fill settings
			Paint prevColor = g2d.getFill();
			Paint prevStroke = g2d.getStroke();
			double prevLineWidth = g2d.getLineWidth();
			StrokeLineCap prevLineCap = g2d.getLineCap();
			double[] prevDashes = g2d.getLineDashes();
			double prevDashOffset = g2d.getLineDashOffset();
			double prevMiterLimit = g2d.getMiterLimit();

			fromPoint = from.getConnectionPoint(new Point2D(from.getPosX(),from.getPosY()),false);
			toPoint   = to.getConnectionPoint(new Point2D(from.getPosX(),from.getPosY()),true);

			// Set the new stroke properties
			g2d.setStroke(color);
			g2d.setFill(color);
			g2d.setLineWidth(width);
			g2d.setLineCap(StrokeLineCap.BUTT);
			g2d.setLineJoin(StrokeLineJoin.MITER);
			g2d.setMiterLimit(5.0f);
			g2d.setLineDashes(dash);
			g2d.setLineDashOffset(0.0);

			drawCurve(g2d, VisConstants.NURB);

			// Restore previous settings
			g2d.setFill(prevColor);
			g2d.setStroke(prevStroke);
			g2d.setLineWidth(prevLineWidth);
			g2d.setLineCap(prevLineCap);
			g2d.setLineJoin(StrokeLineJoin.MITER);
			g2d.setMiterLimit(prevMiterLimit);
			g2d.setLineDashes(prevDashes);
			g2d.setLineDashOffset(prevDashOffset);
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
