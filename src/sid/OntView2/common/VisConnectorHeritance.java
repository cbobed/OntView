package sid.OntView2.common;

import javafx.scene.canvas.GraphicsContext;

import java.awt.*;

public class VisConnectorHeritance extends VisConnectorPropProp {

	public VisConnectorHeritance(VisObjectProperty subProp, VisObjectProperty superProp) {
		super(subProp, superProp);
		
	}

	@Override
	public void draw(GraphicsContext g){
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
