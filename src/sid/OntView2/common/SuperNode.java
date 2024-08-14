package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.ArrayList;
import java.util.List;

public class SuperNode extends Shape {
    private List<VisClass> subNodes;

    public SuperNode() {
        subNodes = new ArrayList<>();
    }

    public void addSubNode(VisClass node) {
        subNodes.add(node);
    }

    public List<VisClass> getSubNodes() {
        return subNodes;
    }

    @Override
    public String getLabel() {
        return null;
    }

    @Override
    public OWLClassExpression getLinkedClassExpression() {
        return null;
    }

    @Override
    public String getToolTipInfo() {
        return "SuperNode";
    }

    @Override
    public int getLevelRelativePos() {
        return 0;
    }

    @Override
    public void drawShape(GraphicsContext g) {
        if (subNodes.isEmpty()) {
            return;
        }

        int roundCornerValue = 10;

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (VisClass node : subNodes) {
            minX = Math.min(minX, node.getPosX() - node.getWidth() / 2);
            maxX = Math.max(maxX, node.getPosX() + node.getWidth() / 2);
            minY = Math.min(minY, node.getPosY() - node.getHeight() / 2);
            maxY = Math.max(maxY, node.getPosY() + node.getHeight() / 2);
        }

        int width = maxX - minX + 20;
        int height = maxY - minY + 30;

        Color lightgray = Color.rgb(234, 234, 234);

        g.setFill(lightgray);
        g.fillRoundRect(minX - 10, minY - 10, width, height, roundCornerValue, roundCornerValue);
        g.setStroke(Color.DARKBLUE);
        g.strokeRoundRect(minX - 10, minY - 10, width, height, roundCornerValue, roundCornerValue);

    }

    @Override
    public Point2D getConnectionPoint(Point2D point, boolean b) {
        return null;
    }
}
