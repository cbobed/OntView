package sid.OntView.common;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;

import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;



public abstract class VisProperty {

	boolean visible; 
	
	public abstract int getPosX();
	public abstract int getPosY();	
	public abstract boolean onProperty(Point p);
	public abstract String getTooltipText();
	public abstract int getLabelHeight();
	public abstract int getLabelWidth();
    public abstract void draw(Graphics g);
	public void setVisible(boolean f){visible = f;}


	public static int stringWidth(String s,Font f,Graphics g){
		Font prevFont = g.getFont();
	    g.setFont(f);
	    FontMetrics fm = g.getFontMetrics();
	    int width =(int) fm.getStringBounds(s, g).getMaxX();
	    g.setFont(prevFont);
	    return width;		
	}
	
	public static int stringHeight(Font f,Graphics g){
		Font prevFont = g.getFont();
	    g.setFont(f);
	    FontMetrics fm = g.getFontMetrics();
	    int height =(int) fm.getAscent();
	    g.setFont(prevFont);
	    return height;		
	}
    








}
	

