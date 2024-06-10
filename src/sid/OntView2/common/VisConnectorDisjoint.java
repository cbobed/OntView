package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class VisConnectorDisjoint extends VisConnectorEquiv {

	Color color= Color.GRAY;
	
	public VisConnectorDisjoint(Shape par_from, Shape par_to) {
		super(par_from, par_to);
		
	}

	@Override
	public void draw(GraphicsContext g){
		int posy1,posy2;
		Color col = color;
		GraphicsContext g2d= (GraphicsContext) g;
		
		if ((from.visible) &&(to.visible)&&(from.graph.disjoint)){
			if (color==null)
				col = VisConnector.color;

			posy1 = from.getPosY();
			posy2 = to.getPosY();
			if (posy1 < posy2) {
				fromPoint = new Point2D(from.posx, posy1 + (double) from.getHeight() / 2);
				toPoint = new Point2D(to.posx, posy2 - (double) to.getHeight() / 2);
			}
			else  {
				fromPoint = new Point2D(from.posx, posy1 - (double) from.getHeight() / 2);
				toPoint = new Point2D(to.posx, posy2 + (double) to.getHeight() / 2);

			}
		    Color prevColor = (Color) g2d.getStroke();
			g2d.setStroke(col);
		  	g2d.strokeLine(fromPoint.getX(), fromPoint.getY(), toPoint.getX(), toPoint.getY());
		  	g2d.strokeLine(fromPoint.getX()-5, fromPoint.getY(), toPoint.getX()-5, toPoint.getY());
			g2d.setStroke(prevColor);
		}	
	}
}
