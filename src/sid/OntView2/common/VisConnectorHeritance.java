package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.StrokeLineCap;

public class VisConnectorHeritance extends VisConnectorPropProp {

	public VisConnectorHeritance(VisObjectProperty subProp, VisObjectProperty superProp) {
		super(subProp, superProp);
		
	}

	@Override
	public void draw(GraphicsContext g){
		GraphicsContext g2d= (GraphicsContext) g;
		if ((visible)&&(from.asVisClass().getPropertyBox().visible)){
			if ((from != null) && (to!= null)){
				if ((from.visible) && (to.visible)){
					Point2D fromPoint = new Point2D(fromProp.getPosX()-10, fromProp.getPosY());
					Point2D toPoint   = new Point2D(toProp.getPosX()+toProp.getLabelWidth()+2, toProp.getPosY());

					Color prevColor = (Color) g2d.getStroke();
					double lineWidthStroke = g2d.getLineWidth();
					StrokeLineCap prevCap = g2d.getLineCap();

					g2d.setStroke(Color.BLACK);
					g2d.strokeLine(fromPoint.getX(),fromPoint.getY(), toPoint.getX(), toPoint.getY());

					g2d.setStroke(prevColor);
					g2d.setLineWidth(lineWidthStroke);
					g2d.setLineCap(prevCap);
				}
			}
		}	
	}
	
	
}
