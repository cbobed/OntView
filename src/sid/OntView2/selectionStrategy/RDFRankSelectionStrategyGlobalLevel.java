package sid.OntView2.selectionStrategy;

import sid.OntView2.common.Shape;

import java.util.*;

public class RDFRankSelectionStrategyGlobalLevel implements SelectionStrategy {
    private final int limit;
    private final Set<Shape> orderedShapesByRDFLevel, orderedDescendantsByLevelLeastImportant,
        orderedDescendantsByLevelBottomTop;
    private final Shape parentShape;
    private final boolean isLR;

    public RDFRankSelectionStrategyGlobalLevel(int limit, Set<Shape> orderedShapesByRDFLevel,
                                               Set<Shape> orderedDescendantsByLevelLeastImportant,
                                               Set<Shape> orderedDescendantsByLevelBottomTop,
                                               Shape parentShape, boolean isLR) {
        this.limit = limit;
        this.orderedShapesByRDFLevel = orderedShapesByRDFLevel;
        this.orderedDescendantsByLevelLeastImportant = orderedDescendantsByLevelLeastImportant;
        this.orderedDescendantsByLevelBottomTop = orderedDescendantsByLevelBottomTop;
        this.parentShape = parentShape;
        this.isLR = isLR;

    }

    /**
     * Returns a set of hidden shapes to be visualized based on the specified percentage limit.
     */
    @Override
    public Set<Shape> getShapesToVisualize() {
        int numberToShow = (int) Math.ceil((limit / 100.0) * orderedShapesByRDFLevel.size());
        int alreadyShown = orderedShapesByRDFLevel.size() - parentShape.getHiddenDescendantsSet();

        if (alreadyShown >= numberToShow){
            return new LinkedHashSet<>();
        }
        numberToShow -= alreadyShown;

        Set<Shape> selectedShapes = new LinkedHashSet<>();
        Set<Shape> iterable = isLR ? orderedShapesByRDFLevel : orderedDescendantsByLevelBottomTop;

        for (Shape candidate : iterable) {
            if (selectedShapes.size() >= numberToShow) {
                break;
            }
            if(!candidate.isVisible()) {
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
        int numberToHide = (int) Math.floor((100 - limit) / 100.0 * orderedShapesByRDFLevel.size());
        int alreadyHidden = parentShape.getHiddenDescendantsSet();

        if (alreadyHidden >= numberToHide){
            return new LinkedHashSet<>();
        }
        numberToHide -= alreadyHidden;

        Set<Shape> selectedShapes = new LinkedHashSet<>();
        Set<Shape> iterable;
        if (isLR){
            iterable = orderedDescendantsByLevelLeastImportant;
        } else {
            List<Shape> reversedShapes = new ArrayList<>(orderedShapesByRDFLevel);
            Collections.reverse(reversedShapes);
            iterable = new LinkedHashSet<>(reversedShapes);
        }

        for (Shape candidate : iterable) {
            if (selectedShapes.size() >= numberToHide) {
                break;
            }
            if (candidate.isVisible()) {
                selectedShapes.add(candidate);
            }
        }

        return selectedShapes;
    }
}
