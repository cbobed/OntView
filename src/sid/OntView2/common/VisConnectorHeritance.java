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
		if (g == null){
			return;
		}
        if ((visible)&&(from.asVisClass().getPropertyBox().visible)){
			if ((from != null) && (to!= null)){
				if ((from.visible) && (to.visible)){
					Point2D fromPoint = new Point2D(fromProp.getPosX()-10, fromProp.getPosY());
					Point2D toPoint   = new Point2D(toProp.getPosX()+toProp.getLabelWidth()+2, toProp.getPosY());

					Color prevColor = (Color) g.getStroke();
					double lineWidthStroke = g.getLineWidth();
					StrokeLineCap prevCap = g.getLineCap();

					g.setStroke(Color.BLACK);
					g.strokeLine(fromPoint.getX(),fromPoint.getY(), toPoint.getX(), toPoint.getY());

					g.setStroke(prevColor);
					g.setLineWidth(lineWidthStroke);
					g.setLineCap(prevCap);
				}
			}
		}	
	}
	
	
}
