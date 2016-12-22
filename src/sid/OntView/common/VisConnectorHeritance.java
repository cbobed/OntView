package sid.OntView.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;

public class VisConnectorHeritance extends VisConnectorPropProp {

	public VisConnectorHeritance(VisObjectProperty subProp, VisObjectProperty superProp) {
		super(subProp, superProp);
		
	}

	@Override
	public void draw(Graphics g){
		Graphics2D g2d= (Graphics2D) g;
		if ((visible)&&(from.asVisClass().getPropertyBox().visible)){
			if ((from != null) && (to!= null)){
				if ((from.visible) && (to.visible)){
					Point fromPoint = new Point(fromProp.getPosX()-10, fromProp.getPosY());
					Point toPoint   = new Point(toProp.getPosX()+toProp.getLabelWidth()+2, toProp.getPosY());
					Color prevColor = g2d.getColor();
					Stroke prevStroke = g2d.getStroke();
					g2d.setColor(Color.BLACK);
					g2d.drawLine(fromPoint.x,fromPoint.y, toPoint.x, toPoint.y);
				 	g2d.setColor(prevColor);
				 	g2d.setStroke(prevStroke);				 	
				} 	
			}
		}	
	}
	
	
}
