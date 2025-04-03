package sid.OntView2.selectionStrategy;

import sid.OntView2.common.Shape;

import java.util.*;

public class RDFRankSelectionStrategyStepsLR implements SelectionStrategy {
    private final int limit;
    private final Set<Shape> orderedShapesByRDFLevel, orderedDescendantsByLevelLeastImportant;

    public RDFRankSelectionStrategyStepsLR(int limit, Set<Shape> orderedShapesByRDFLevel,
                                           Set<Shape> orderedDescendantsByLevelLeastImportant) {
        this.limit = limit;
        this.orderedShapesByRDFLevel = orderedShapesByRDFLevel;
        this.orderedDescendantsByLevelLeastImportant = orderedDescendantsByLevelLeastImportant;
    }

    /**
     * Returns a set of hidden shapes to be visualized based on the specified percentage limit.
     */
    @Override
    public Set<Shape> getShapesToVisualize() {
        int numberToShow = (int) Math.ceil((limit / 100.0) * orderedShapesByRDFLevel.size());

        Set<Shape> selectedShapes = new LinkedHashSet<>();

        for (Shape candidate : orderedShapesByRDFLevel) {
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
        int numberToShow = (int) Math.ceil((limit / 100.0) * orderedShapesByRDFLevel.size());

        Set<Shape> selectedShapes = new LinkedHashSet<>();
        for (Shape candidate : orderedDescendantsByLevelLeastImportant) {
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
