package sid.OntView2.common;

import java.awt.*;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.GeneralPath;


public class VisConnectorPropertyRange extends VisConnectorIsA {

	VisPropertyBox parentBox;
	VisObjectProperty vprop;
	
	GeneralPath path;
	
	public VisConnectorPropertyRange(VisPropertyBox box, Shape par_from, Shape par_to, VisObjectProperty pvprop) {
		super(par_from, par_to);
		vprop = pvprop;
		parentBox = box;
		path = new GeneralPath();
		
	}

	@Override
	public void draw(Graphics g){
		Graphics2D g2d= (Graphics2D) g;
		boolean globalHide  = parentBox.vclass.graph.paintframe.hideRange;
		if ((visible)&&(parentBox.visible)&&(!globalHide)){
			if ((from != null) && (to!= null)){
				if ((from.visible) && (to.visible)){
					fromPoint = new Point(vprop.getPosX(),vprop.getPosY());
					toPoint = new Point(to.getPosX(),to.getPosY());
			    	fromPoint.x += vprop.getLabelWidth()+15;
					Color prevColor = g2d.getColor();
				  	g2d.setColor(Color.GRAY);
			  		g2d.fillOval(fromPoint.x, fromPoint.y-3, 4, 4);
//				  	g2d.drawLine(fromPoint.x+2, fromPoint.y-2, toPoint.x, toPoint.y);
			  		drawCurve(g2d, VisConstants.NURB);
//			  		setPath(path,fromPoint.x+2, fromPoint.y-2, toPoint.x, toPoint.y);
//			  		g2d.draw(path);
				  	g2d.setColor(prevColor);
				} 	
			}
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
	
	protected void drawNurbs(Graphics2D g2d, Point pfrom, Point pto){
		CubicCurve2D curve2 = null; 

		calculateNurbPoints(pfrom.getX(), pfrom.getY(), toPoint.getX(), toPoint.getY());
		curve2 = new CubicCurve2D.Float(); 
		curve2.setCurve(pfrom.getX(),pfrom.getY(), 
						controlx1,pfrom.getY(),
						controlx2,pfrom.getY(), 
						pto.getX(),pto.getY()); 
		g2d.draw(curve2); 
	}
	
	protected void drawBezier(Graphics2D g2d){
	    setPath(path,fromPoint.getX(), fromPoint.getY(), toPoint.getX(),toPoint.getY());
	    g2d.draw(path);
	}
}	
