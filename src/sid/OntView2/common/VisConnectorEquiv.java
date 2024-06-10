package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.HashSet;

public class VisConnectorEquiv extends VisConnector {

	Color color= Color.CYAN;
	
	public VisConnectorEquiv(Shape par_from, Shape par_to) {
		super(par_from, par_to);
		fromPoint = new Point2D(0,0);
		toPoint =  new Point2D(0,0);
		
	}

	@Override
	public void draw(GraphicsContext g){
		int posy1,posy2;
		Color col = color;
		GraphicsContext g2d= (GraphicsContext) g;
		
		if ((from.visible) &&(to.visible)){
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
		  	g2d.strokeLine(fromPoint.getX()+5, fromPoint.getY(), toPoint.getX()+5, toPoint.getY());
		 	g2d.setStroke(prevColor);
		}	
	}

	/**
	 * Returns if dst is accesible from origin
	 * @param discarded
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
