package sid.OntView2.common;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.geometry.Point2D;


public abstract class VisProperty {

	boolean visible; 
	
	public abstract int getPosX();
	public abstract int getPosY();	
	public abstract boolean onProperty(Point2D p);
	public abstract String getTooltipText();
	public abstract int getLabelHeight();
	public abstract int getLabelWidth();
    public abstract void draw(GraphicsContext g);
	public void setVisible(boolean f){visible = f;}


	public static int stringWidth(String s, Font f, GraphicsContext g){
		Text text = new Text(s);
		text.setFont(f);
		return (int) Math.ceil(text.getLayoutBounds().getWidth());
	}
	
	public static int stringHeight(Font f, GraphicsContext g){
		Text text = new Text();
		text.setFont(f);
		return (int) Math.ceil(text.getLayoutBounds().getHeight());
	}

}
	

