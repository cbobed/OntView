package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;


public class VisConnectorPropertyRange extends VisConnectorIsA {

	VisPropertyBox parentBox;
	VisObjectProperty vprop;
	
	Path path;
	
	public VisConnectorPropertyRange(VisPropertyBox box, Shape par_from, Shape par_to, VisObjectProperty pvprop) {
		super(par_from, par_to);
		vprop = pvprop;
		parentBox = box;
		path = new Path();
		
	}

	@Override
	public void draw(GraphicsContext g){
		GraphicsContext g2d= (GraphicsContext) g;
		boolean globalHide  = parentBox.vclass.graph.paintframe.hideRange;
		if ((visible)&&(parentBox.visible)&&(!globalHide)){
			if ((from != null) && (to!= null)){
				if ((from.visible) && (to.visible)){
					fromPoint = new Point2D(vprop.getPosX() + vprop.getLabelWidth()+15,vprop.getPosY());
					toPoint = new Point2D(to.getPosX(),to.getPosY());
					Color prevColor = (Color) g2d.getStroke();
				  	g2d.setStroke(Color.GRAY);
			  		g2d.fillOval(fromPoint.getX(), fromPoint.getY()-3, 4, 4);
//				  	g2d.drawLine(fromPoint.x+2, fromPoint.y-2, toPoint.x, toPoint.y);
			  		drawCurve(g2d, VisConstants.NURB);
//			  		setPath(path,fromPoint.x+2, fromPoint.y-2, toPoint.x, toPoint.y);
//			  		g2d.draw(path);
				  	g2d.setStroke(prevColor);
				} 	
			}
		}	
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

		calculateNurbPoints(pfrom.getX(), pfrom.getY(), toPoint.getX(), toPoint.getY());
		g2d.beginPath();
		g2d.moveTo(pfrom.getX(), pfrom.getY());
		g2d.bezierCurveTo(controlx1, pfrom.getY(), controlx2, pfrom.getY(), pto.getX(), pto.getY());
		g2d.stroke();
	}
	
	protected void drawBezier(GraphicsContext g2d){
	    setPath(path,fromPoint.getX(), fromPoint.getY(), toPoint.getX(),toPoint.getY());
	    drawPath(g2d, path);
	}
}	
