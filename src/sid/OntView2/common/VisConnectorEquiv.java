package sid.OntView2.common;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;

public class VisConnectorEquiv extends VisConnector {

	Color color= Color.cyan;
	
	public VisConnectorEquiv(Shape par_from, Shape par_to) {
		super(par_from, par_to);
		fromPoint = new Point();
		toPoint =  new Point();
		
	}

	@Override
	public void draw(Graphics g){
		int posy1,posy2;
		Color col = color;
		Graphics2D g2d= (Graphics2D) g;
		
		if ((from.visible) &&(to.visible)){
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
		  	g2d.drawLine(fromPoint.x+5, fromPoint.y, toPoint.x+5, toPoint.y);
		 	g2d.setColor(prevColor);
		}	
	}

	/**
	 * Returns if dst is accesible from origin
	 * @param list
	 * @param or
	 * @param dst
	 * @return boolean
	 */
	public static boolean accesible (Shape or, Shape dst, HashSet<Shape>discarded){
		HashSet<Shape> neighbours = new HashSet<Shape>();
        boolean retVal = false;
		if (discarded==null) {
			discarded = new HashSet<Shape>();
		}
		discarded.add(or);
//		System.out.println(or.asVisClass().label);
		for (VisConnectorEquiv c : or.asVisClass().getEquivConnectors()){
			if ( (c.from== dst) || (c.to==dst)  ) {
					return true;
			}
			else {
					neighbours.add(c.to);
					neighbours.add(c.from);
			}
		}	
		for ( Shape neigh : neighbours){
			if (!discarded.contains(neigh)) {
				if (accesible( neigh,dst, discarded)){
					return true;
				}	
			}
		}	
		return false;
	}
	
		// from Java 1.7 and on they don't allow to override static methods 
	   public static VisConnectorEquiv getConnectorEquiv(ArrayList<VisConnectorEquiv>list, Shape s1, Shape s2){
		   for (VisConnectorEquiv c : list){
			   if ((c.from == s1)&&(c.to==s2)){
			      return c;
			   }
		   }
		   return null;
	   }


}
