package sid.OntView2.selectionStrategy;

import sid.OntView2.common.Shape;

import java.util.Set;

public interface SelectionStrategy {
    /**
     * Given a set of shapes, returns a subset of shapes that should be visualized,
     * according to the specific strategy.
     */
    Set<Shape> getShapesToVisualize();

    /**
     * Given a set of shapes, returns a subset of shapes that should be hidden,
     * according to the specific strategy.
     */
    Set<Shape> getShapesToHide();

    default int ensureLimitSufficient(int limit, Shape parentShape, int numShapes) {
        int minimumRequired = (int) Math.ceil(100.0 / numShapes);
        minimumRequired = Math.min(minimumRequired, 100);
        if (limit < minimumRequired) {
            parentShape.notEnoughLimit();
            return minimumRequired;
        }
        return limit;
    }
}
