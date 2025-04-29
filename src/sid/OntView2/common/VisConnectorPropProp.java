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
		if(from.getPosX() > to.getPosX()){
			fromPoint = new Point2D(fromProp.getPosX(), fromProp.getPosY()-1);
		} /*else if (from.getPosX() == to.getPosX()){
			fromPoint = new Point2D(fromProp.getPosX(), fromProp.getPosY());
		}*/ else {
			fromPoint = new Point2D(fromProp.getPosX()+fromProp.getLabelWidth()+5, fromProp.getPosY());
		}
		//fromPoint = new Point2D(fromProp.getPosX()+fromProp.getLabelWidth()+5, fromProp.getPosY());
		return fromPoint;
	}
	
	private Point2D getToPoint() {
		d = (toProp.getPosY()<fromProp.getPosY() ? -5:5);
		if (from == to){
			toPoint = new Point2D(toProp.getPosX() + toProp.getLabelWidth()-3, toProp.getPosY()- d);
		}
		else{
			toPoint = new Point2D(toProp.getPosX()+toProp.getLabelWidth()+10,toProp.getPosY());
		}
		return toPoint; 
	}
	
	
	private boolean drawable (){
        return (from.visible) && (to.visible) &&
            (visible) && (fromProp.visible) && (toProp.visible) &&
            (from.asVisClass().getPropertyBox().visible) &&
            (to.asVisClass().getPropertyBox().visible) && (from != null) && (to != null);
	}
	
	@Override
	public void draw(GraphicsContext g){
		if (g == null){
			return;
		}
        if (drawable()){
			Color prevColor = (Color) g.getStroke();
			double prevLineWidth = g.getLineWidth();

			g.setStroke(Color.HOTPINK);
			g.setLineWidth(1.7);
			setPath(path, getFromPoint().getX(),getFromPoint().getY(), getToPoint().getX(), getToPoint().getY());
			drawPath(g, path);

			g.setFill(Color.DEEPPINK);
			g.fillOval(getFromPoint().getX()-3, getFromPoint().getY()-2, 4, 4);

			g.setLineWidth(prevLineWidth);
			g.setFill(prevColor);
		 	g.setStroke(prevColor);
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
