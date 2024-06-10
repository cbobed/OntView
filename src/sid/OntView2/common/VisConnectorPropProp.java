package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.Path;
import javafx.scene.paint.Color;



public class VisConnectorPropProp extends VisConnector {

	VisObjectProperty fromProp;
	VisObjectProperty toProp;
    int d = 0;
	Point2D fromPoint,toPoint;
	Path path;
	
	public VisConnectorPropProp(VisObjectProperty subProp, VisObjectProperty superProp) {
		
		fromProp = subProp;
		toProp = superProp;
		from = subProp.getDomain();
		to   = superProp.getDomain();
		
		path = new Path();
	}

	private Point2D getFromPoint(){
//		if (fromPoint==null){
			fromPoint = new Point2D(fromProp.getPosX()+fromProp.getLabelWidth()+14, fromProp.getPosY());
//		}
		return fromPoint; 
	}
	
	private Point2D getToPoint() {
		d = (toProp.getPosY()< fromProp.getPosY() ? -5:5);
		if (from == to){
			toPoint = new Point2D(toProp.getPosX()+ (double) (toProp.getLabelWidth() * 3) /4, toProp.getPosY()-d);
		}
		else{
			toPoint = new Point2D(toProp.getPosX()+toProp.getLabelWidth()+10,toProp.getPosY());
		}
		return toPoint; 
	}
	
	
	private boolean drawable (){
		if ((from.visible) &&(to.visible) &&
				(visible)&&(fromProp.visible)&&(toProp.visible)&&
						(from.asVisClass().getPropertyBox().visible)&&
						(to.asVisClass().getPropertyBox().visible)&&(from != null) && (to!= null)){
			return true;
			}
		else {
			return false;
		}
	}
	
	@Override
	public void draw(GraphicsContext g){
		GraphicsContext g2d= (GraphicsContext) g;
		if (drawable()){
			Color prevColor = (Color) g2d.getStroke();
			g2d.setStroke(Color.BLUE);
			g2d.strokeOval(getFromPoint().getX()-8, getFromPoint().getY()-3, 3, 3);
//			VisConnector.drawArrow(g,getFromPoint().x-5,getFromPoint().y, getToPoint().x-2, getToPoint().y);
			setPath(path, getFromPoint().getX()-5,getFromPoint().getY(), getToPoint().getX()-2, getToPoint().getY());
			drawPath(g, path);
		 	g2d.setStroke(prevColor);
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
	
	private void drawNurbs(GraphicsContext g2d, Point2D fromPoint2, Point2D toPoint2) {
		// TODO Auto-generated method stub
		
	}

	protected void drawBezier(GraphicsContext g2d){
	    setPath(path,fromPoint.getX(), fromPoint.getY(), toPoint.getX(),toPoint.getY());
	    drawPath(g2d, path);
	}
	
}
