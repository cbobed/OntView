package sid.OntView2.common;

import javafx.scene.canvas.GraphicsContext;

import java.awt.*;
import java.awt.geom.GeneralPath;

public class VisConnectorPropProp extends VisConnector {

	VisObjectProperty fromProp;
	VisObjectProperty toProp;
    int d = 0;
	Point fromPoint,toPoint;
	GeneralPath path;
	
	public VisConnectorPropProp(VisObjectProperty subProp, VisObjectProperty superProp) {
		
		fromProp = subProp;
		toProp = superProp;
		from = subProp.getDomain();
		to   = superProp.getDomain();
		
		path = new GeneralPath();
	}

	private Point getFromPoint(){
//		if (fromPoint==null){
			fromPoint = new Point(fromProp.getPosX()+fromProp.getLabelWidth()+14, fromProp.getPosY());
//		}
		return fromPoint; 
	}
	
	private Point getToPoint() {
		d = (toProp.getPosY()< fromProp.getPosY() ? -5:5);
		if (from == to){
			toPoint = new Point(toProp.getPosX()+toProp.getLabelWidth()*3/4, toProp.getPosY()-d);	
		}
		else{
			toPoint = new Point(toProp.getPosX()+toProp.getLabelWidth()+10,toProp.getPosY());
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
		Graphics2D g2d= (Graphics2D) g;
		if (drawable()){
			Color prevColor = g2d.getColor();					
			g2d.setColor(Color.blue);
			g2d.drawOval(getFromPoint().x-8, getFromPoint().y-3, 3, 3);
//			VisConnector.drawArrow(g,getFromPoint().x-5,getFromPoint().y, getToPoint().x-2, getToPoint().y);
			setPath(path, getFromPoint().x-5,getFromPoint().y, getToPoint().x-2, getToPoint().y);
			g2d.draw(path);
		 	g2d.setColor(prevColor);
		} 	
	}
	
	protected void drawCurve(Graphics2D g2d,int method){
		switch (method){
			case VisConstants.BEZIER:
				drawBezier(g2d);
			    break;
			case VisConstants.NURB:
				drawNurbs(g2d, fromPoint, toPoint);
		
		}
		
	}
	
	private void drawNurbs(Graphics2D g2d, Point fromPoint2, Point toPoint2) {
		// TODO Auto-generated method stub
		
	}

	protected void drawBezier(Graphics2D g2d){
	    setPath(path,fromPoint.getX(), fromPoint.getY(), toPoint.getX(),toPoint.getY());
	    g2d.draw(path);
	}
	
}
