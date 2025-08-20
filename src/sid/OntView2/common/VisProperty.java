package sid.OntView2.common;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.geometry.Point2D;

import java.util.Optional;
import java.util.Set;


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

	public static int stringWidth(String s, Font f){
		Text text = new Text(s);
		text.setFont(f);
		return (int) Math.ceil(text.getLayoutBounds().getWidth());
	}
	
	public static int stringHeight(Font f){
		Text text = new Text();
		text.setFont(f);
		return (int) Math.ceil(text.getLayoutBounds().getHeight());
	}

    public abstract void setVisibleLabel(String visibleLabel);

    public void swapLabel(Boolean labelRendering, Boolean qualifiedRendering, String language,
                          String label, String qualifiedLabel,
                          Set<String> explicitLabel, Set<String> explicitQualifiedLabel) {
        // this is needed for the getTooltipInfo method of the different
        // elements: as this info is refreshed at a different pace from the
        // global view refreshment, these methods have to be aware of the type of
        // rendering that is being used (labelled, qualified).

        if (labelRendering){
            Optional<String> candidate;
            if (qualifiedRendering) {
                candidate = explicitQualifiedLabel.stream()
                    .filter(s -> s.contains("@" + language))
                    .findFirst();
            }
            else {
                candidate = explicitLabel.stream()
                    .filter(s -> s.contains("@" + language))
                    .findFirst();
            }
            candidate.ifPresent(this::setVisibleLabel);
        }
        else {
            if (qualifiedRendering) {
                setVisibleLabel(qualifiedLabel);
            } else {
                setVisibleLabel(label);
            }
        }
    }
}
	

