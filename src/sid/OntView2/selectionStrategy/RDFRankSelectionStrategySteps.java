package sid.OntView2.selectionStrategy;

import sid.OntView2.common.Shape;

import java.util.*;

public class RDFRankSelectionStrategySteps implements SelectionStrategy {
    private final int limit;
    private final Set<Shape> orderedShapesByRDF;

    public RDFRankSelectionStrategySteps(int limit, Set<Shape> orderedShapesByRDF) {
        this.limit = limit;
        this.orderedShapesByRDF = orderedShapesByRDF;
    }

    /**
     * Returns a set of hidden shapes to be visualized based on the specified percentage limit.
     */
    @Override
    public Set<Shape> getShapesToVisualize() {
        int numberToShow = (int) Math.ceil((limit / 100.0) * orderedShapesByRDF.size());

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
        int numberToShow = (int) Math.ceil((limit / 100.0) * orderedShapesByRDF.size());

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
