package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;


public class VisConnectorPropertyRange extends VisConnectorIsA {

	VisPropertyBox parentBox;
	VisObjectProperty vprop;
    double ARROW_SIZE = 7.0;

    Path path;
	
	public VisConnectorPropertyRange(VisPropertyBox box, Shape par_from, Shape par_to, VisObjectProperty pvprop) {
		super(par_from, par_to);
		vprop = pvprop;
		parentBox = box;
		path = new Path();
		
	}

	@Override
	public void draw(GraphicsContext g){
        boolean globalHide  = parentBox.vClass.graph.paintframe.hideRange;
		if ((visible)&&(parentBox.visible)&&(!globalHide)){
			if ((from != null) && (to!= null)){
				if ((from.visible) && (to.visible)){
					if (vprop.getPosX() > to.getLeftCorner()){
						fromPoint = new Point2D(vprop.getPosX()-1,vprop.getPosY()-5);

					} else if (vprop.getPosX() == to.getLeftCorner()){
						fromPoint = new Point2D(vprop.getPosX(),vprop.getPosY()-5);
					}
					else{
						fromPoint = new Point2D(vprop.getPosX() + vprop.getLabelWidth()+4,vprop.getPosY()-5);
					}

					double toX = (vprop.getPosX() == to.getLeftCorner()) ? to.getLeftCorner() : to.getRightCorner();
					toPoint = (vprop.getPosY() > to.getBottomShapeCorner()) ?
							new Point2D(toX, to.getBottomShapeCorner() - 1) :
							new Point2D(toX, to.getTopCorner() + 1);

					Color prevColor = (Color) g.getStroke();
					double prevLineWidth = g.getLineWidth();

					g.setStroke(Color.DARKTURQUOISE);
					g.setLineWidth(1.7);
			  		drawCurve(g, VisConstants.NURB);

					g.setFill(Color.DARKCYAN);
					g.fillOval(fromPoint.getX()-2, fromPoint.getY()-2, 4, 4);
                    drawArrowProperty(g, ARROW_SIZE, ARROW_SIZE , toPoint.getX(), toPoint.getY());

                    g.setStroke(prevColor);
					g.setLineWidth(prevLineWidth);
				  	g.setFill(prevColor);
				} 	
			}
		}	
	}

    private void drawArrowProperty(GraphicsContext g, double length, double width, double x, double y) {
        double tx = toPoint.getX() - controlX2;
        double ty = toPoint.getY() - fromPoint.getY();
        double angleDeg = Math.toDegrees(Math.atan2(ty, tx));
        g.setFill(Color.DARKCYAN);

        double halfW = width / 2.0;
        g.save();
        g.translate(x, y);
        g.rotate(angleDeg);

        double[] xPoints = {  0, -length, -length };
        double[] yPoints = {  0,  halfW,  -halfW };

        g.fillPolygon(xPoints, yPoints, 3);
        g.restore();
    }


    protected void drawCurve(GraphicsContext g2d,int method){
		switch (method){
			case VisConstants.BEZIER:
				drawBezier(g2d);
			    break;
			case VisConstants.NURB:
				drawNurbs(g2d, fromPoint, toPoint);
		}
	}
	
	protected void drawNurbs(GraphicsContext g2d, Point2D pfrom, Point2D pto){

		calculateNURBPoints(pfrom.getX(), pfrom.getY(), toPoint.getX(), toPoint.getY());
		g2d.beginPath();
		g2d.moveTo(pfrom.getX(), pfrom.getY());
		g2d.bezierCurveTo(controlX1, pfrom.getY(), controlX2, pfrom.getY(), pto.getX(), pto.getY());
		g2d.stroke();
	}
	
	protected void drawBezier(GraphicsContext g2d){
	    setPath(path,fromPoint.getX(), fromPoint.getY(), toPoint.getX(),toPoint.getY());
	    drawPath(g2d, path);
	}
}	
