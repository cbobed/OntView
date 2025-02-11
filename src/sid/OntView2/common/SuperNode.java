package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.semanticweb.owlapi.model.OWLClassExpression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SuperNode extends Shape {
    private final Set<Shape> subNodes;

    public SuperNode() {
        subNodes = new HashSet<>();
    }

    public void addSubNode(Shape node) {
        subNodes.add(node);
    }

    public Set<Shape> getSubNodes() {
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

        double minX = Integer.MAX_VALUE;
        double maxX = Integer.MIN_VALUE;
        double minY = Integer.MAX_VALUE;
        double maxY = Integer.MIN_VALUE;

        for (Shape node : subNodes) {
            if (getIndicatorSize() == -1) setIndicatorSize(node.getIndicatorSize());
            minX = Math.min(minX, node.getPosX() - (double) node.getWidth() / 2);
            maxX = Math.max(maxX, node.getPosX() + (double) node.getWidth() / 2);
            minY = Math.min(minY, node.getPosY() - (double) node.getHeight() / 2);
            maxY = Math.max(maxY, node.getPosY() + (double) node.getHeight() / 2);
        }

        double width = Math.max(maxX - minX + 40, getIndicatorSize() + 40);
        double height = maxY - minY + 40;

        Color lightgray = Color.rgb(234, 234, 234);

        g.setFill(lightgray);
        g.fillRoundRect(minX - 20, minY - 30, width, height, roundCornerValue, roundCornerValue);
        g.setStroke(Color.DARKBLUE);
        g.strokeRoundRect(minX - 20, minY - 30, width, height, roundCornerValue, roundCornerValue);

    }

    @Override
    public Point2D getConnectionPoint(Point2D point, boolean b) {
        return null;
    }
}
