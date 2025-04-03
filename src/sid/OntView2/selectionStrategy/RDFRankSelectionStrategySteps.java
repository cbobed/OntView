package sid.OntView2.selectionStrategy;

import sid.OntView2.common.Shape;

import java.util.*;

public class RDFRankSelectionStrategySteps implements SelectionStrategy {
    private int limit;
    private final Set<Shape> orderedShapesByRDF;
    private final Shape parentShape;

    public RDFRankSelectionStrategySteps(int limit, Set<Shape> orderedShapesByRDF, Shape parentShape) {
        this.limit = limit;
        this.orderedShapesByRDF = orderedShapesByRDF;
        this.parentShape = parentShape;
    }

    /**
     * Returns a set of hidden shapes to be visualized based on the specified percentage limit.
     */
    @Override
    public Set<Shape> getShapesToVisualize() {
        limit = ensureLimitSufficient(limit, parentShape, orderedShapesByRDF.size());
        int numberToShow = (int) Math.floor((limit / 100.0) * orderedShapesByRDF.size());

        Set<Shape> selectedShapes = new LinkedHashSet<>();

        for (Shape candidate : orderedShapesByRDF) {
            if (selectedShapes.size() >= numberToShow) {
                break;
            }
            if (!candidate.isVisible()) {
                selectedShapes.add(candidate);
            }
        }
        return selectedShapes;
    }

    /**
     * Returns a set of visual shapes to be hidden based on the specified percentage limit.
     */
    @Override
    public Set<Shape> getShapesToHide() {
        limit = ensureLimitSufficient(limit, parentShape, orderedShapesByRDF.size());
        int numberToShow = (int) Math.floor((limit / 100.0) * orderedShapesByRDF.size());

        Set<Shape> selectedShapes = new LinkedHashSet<>();
        List<Shape> reversedShapes = new ArrayList<>(orderedShapesByRDF);
        Collections.reverse(reversedShapes);

        for (Shape candidate : reversedShapes) {
            if (selectedShapes.size() >= numberToShow) {
                break;
            }
            if (candidate.isVisible()) {
                selectedShapes.add(candidate);
            }
        }
        return selectedShapes;
    }
}
