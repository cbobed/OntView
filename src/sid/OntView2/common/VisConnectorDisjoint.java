package sid.OntView2.common;

import java.awt.*;

public class VisConnectorDisjoint extends VisConnectorEquiv {

	Color color= Color.GRAY;

	
	public VisConnectorDisjoint(Shape par_from, Shape par_to) {
		super(par_from, par_to);
		
	}
	
	
	@Override
	public void draw(Graphics g){
		int posy1,posy2;
		Color col = color;
		Graphics2D g2d= (Graphics2D) g;
		
		if ((from.visible) &&(to.visible)&&(from.graph.disjoint)){
			if (color==null)
				col = VisConnector.color;
			posy1 = from.getPosY();
			posy2 = to.getPosY();
			fromPoint.x = from.posx;
			toPoint.x = to.posx;
			if (posy1 < posy2) {
				fromPoint.y = posy1+from.getHeight()/2;
				toPoint.y = posy2-to.getHeight()/2;
			}
			else  {
				fromPoint.y = posy1-from.getHeight()/2;
				toPoint.y = posy2+to.getHeight()/2;

			}
		    Color prevColor = g2d.getColor();
		  	g2d.setColor(col);
		  	g2d.drawLine(fromPoint.x, fromPoint.y, toPoint.x, toPoint.y);
		  	g2d.drawLine(fromPoint.x-5, fromPoint.y, toPoint.x-5, toPoint.y);
		 	g2d.setColor(prevColor);
		}	
	}
}
