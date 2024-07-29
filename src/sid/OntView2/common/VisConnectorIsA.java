package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

public class VisConnectorIsA extends VisConnector {

	Color isaColor = null;

	Color backgroundColor = Color.web("#f9f9f9");
	
	private static double PATH_OFFSET = 5.0;
	
	Path path;
	

	public VisConnectorIsA(Shape par_from, Shape par_to) {
		super(par_from, par_to);
		isaColor = VisConnector.color;
		path = new Path();
	}

	@Override
	public void draw(GraphicsContext g){
		if (g == null){
			return;
		}
        Color prevColor = (Color) g.getStroke();
		double prevLineWidth = g.getLineWidth();
		StrokeLineCap prevCap = g.getLineCap();
		StrokeLineJoin prevJoin = g.getLineJoin();

		if (visible){
			if ((from != null) && (to!= null)){
				fromPoint = from.getConnectionPoint(new Point2D(from.getPosX(),from.getPosY()),false);
				toPoint   = to.getConnectionPoint(new Point2D(from.getPosX(),from.getPosY()),true);

			    Shape selected = from.graph.paintframe.getPressedShape();
				Shape erase = from.graph.paintframe.getEraseConnector();

			    if (selected !=null) {
					g.setLineWidth(width);
					g.setLineCap(StrokeLineCap.SQUARE);
					g.setLineJoin(StrokeLineJoin.MITER);

					if ((from==selected)||(to==selected)){
						g.setStroke(Color.ORANGE);
						g.setFill(Color.ORANGE);
					}
					else  {
						g.setStroke(isaColor);
						g.setFill(isaColor);
						g.setLineWidth(minWidth);
					}
			    } else if (erase != null) {
					g.setStroke(backgroundColor);
					g.setFill(backgroundColor);
				}
			    else {
					double lineWidth = (redundant ? minWidth : width);
					Color col = (redundant ? altColor : isaColor);
					g.setStroke(col);
					g.setLineWidth(lineWidth);
					g.setLineCap(StrokeLineCap.SQUARE);
					g.setLineJoin(StrokeLineJoin.MITER);
			    }

			    //drawing a curve path
			    drawCurve(g, VisConstants.NURB);
			   
			    this.drawArrow(g, 5, PATH_OFFSET, fromPoint.getX(), fromPoint.getY());

				System.out.println("Color used " + g.getFill().toString() + ". Stroke used " + g.getStroke().toString());


				g.setStroke(prevColor);
				g.setFill(prevColor);
				g.setLineWidth(prevLineWidth);
				g.setLineCap(prevCap);
				g.setLineJoin(prevJoin);

				System.out.println("Color used " + prevColor.toString());
				System.out.println(" ");

			}
		}	
	}

	
	@Override
	protected void drawCurve(GraphicsContext g2d,int method){
		switch (method){
			case VisConstants.BEZIER:
				drawBezier(g2d);
			    break;
			case VisConstants.NURB:
				drawNurbs(g2d, fromPoint, toPoint);
		
		}
	}
	
	protected void drawBezier(GraphicsContext g2d){
	    setPath(path,fromPoint.getX(), fromPoint.getY(), toPoint.getX(),toPoint.getY());
		drawPath(g2d, path);
	}

	protected void drawNurbs(GraphicsContext g2d, Point2D pfrom, Point2D pto){
		double auxX1 = 0.2 * (pto.getX()-pfrom.getX()) + pfrom.getX();
        double auxX2 = 0.4 * (pto.getX()-pfrom.getX()) + pfrom.getX();

		g2d.beginPath();
		g2d.moveTo(pfrom.getX(), pfrom.getY());

		g2d.bezierCurveTo(
				auxX1, pfrom.getY(),
				auxX2, pto.getY(),
				pto.getX(), pto.getY()
		);
		g2d.stroke();
	}
	
	
	protected void calculateBezierPoints(double fromPointX, double fromPointY, double toPointX,double toPointY){
	/*
	 * calculates intermediate points (x1,y1) (x2,y2) for a Bezier curve
	 */
		double heightDiff = Math.abs(toPointY -fromPointY);
	    if (toPoint.getY() -fromPoint.getY() > 0){
	    	controlx1 = fromPointX + 0.2*(toPointX-fromPointX);
	    	controly1 = fromPointY + 0.8*(heightDiff);
	    	controlx2 = fromPointX + 0.8*(toPointX-fromPointX);
	    	controly2 = fromPointY + 0.9*(heightDiff);
        }
	    else {
	    	controlx1 = fromPoint.getX() + 0.2*(toPointX-fromPointX);
	    	controly1 = fromPoint.getY() + (-0.8)*(heightDiff);
	    	controlx2 = fromPoint.getX() + 0.8*(toPointX-fromPointX);
	    	controly2 = fromPoint.getY() + (-0.9)*(heightDiff);
	    }
	}
	
	@Override
	protected void setPath(Path path,double fromPointX, double fromPointY, double toPointX,double toPointY){
	/*
	 *  adds Points to a path to be drawn
	 */
		path.getElements().clear();
		calculateBezierPoints(fromPointX, fromPointY, toPointX, toPointY);
		path.getElements().add(new MoveTo(fromPointX, fromPointY));
		path.getElements().add(new LineTo(fromPointX + PATH_OFFSET, fromPointY));
		path.getElements().add(new CubicCurveTo(
				controlx1, controly1, controlx2, controly2, toPointX - PATH_OFFSET, toPointY
		));
		path.getElements().add(new LineTo(toPointX, toPointY));
		
	}
	
	public void drawArrow(GraphicsContext g,int height,double pATH_OFFSET2,double d,double e){
	    double[] xPoints = { (d+pATH_OFFSET2), (d+pATH_OFFSET2),  (int) d};
	    double[] yPoints = { (e-height), (e+height), (int) e};
	    g.fillPolygon(xPoints, yPoints, 3);
		
	}
	
}
