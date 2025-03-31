package sid.OntView2.selectionStrategy;

import sid.OntView2.common.Shape;

import java.util.*;

public class RDFRankSelectionStrategyFull implements SelectionStrategy {
    private final int limit;
    private final Set<Shape> orderedShapesByRDF;

    public RDFRankSelectionStrategyFull(int limit, Set<Shape> orderedShapesByRDF) {
        this.limit = limit;
        this.orderedShapesByRDF = orderedShapesByRDF;
    }

    /**
     * Returns a set of hidden shapes to be visualized based on the specified percentage limit.
     * Selects the nodes based on the total number of nodes, regardless of their current visibility.
     * Returns the nodes that are not currently visible.
     */
    @Override
    public Set<Shape> getShapesToVisualize() {
        int numberToShow = (int) Math.floor((limit / 100.0) * orderedShapesByRDF.size());

        Set<Shape> selectedShapes = new LinkedHashSet<>();

        for (Shape candidate : orderedShapesByRDF) {
            if (selectedShapes.size() >= numberToShow) {
                break;
            }
            selectedShapes.add(candidate);
        }

        Set<Shape> hiddenShapes = new LinkedHashSet<>();
        for (Shape shape : selectedShapes) {
            if (!shape.isVisible()) {
                hiddenShapes.add(shape);
            }
        }

        return hiddenShapes;
    }

    /**
     * Returns a set of visual shapes to be hidden based on the specified percentage limit.
     */
    @Override
    public Set<Shape> getShapesToHide() {
        int numberToShow = (int) Math.floor((limit / 100.0) * orderedShapesByRDF.size());
        numberToShow = orderedShapesByRDF.size() - numberToShow;

        Set<Shape> selectedShapes = new LinkedHashSet<>();
        List<Shape> reversedShapes = new ArrayList<>(orderedShapesByRDF);
        Collections.reverse(reversedShapes);

        for (Shape candidate : reversedShapes) {
            if (selectedShapes.size() >= numberToShow) {
                break;
            }
            selectedShapes.add(candidate);
        }

        Set<Shape> visibleShapes = new LinkedHashSet<>();
        for (Shape shape : selectedShapes) {
            if (shape.isVisible()) {
                visibleShapes.add(shape);
            }
        }

        return visibleShapes;
    }
}
