package sid.OntView2.selectionStrategy;

import sid.OntView2.common.Shape;

import java.util.*;

public class RDFRankSelectionStrategyGlobalRL implements SelectionStrategy {
    private final int limit;
    private final Set<Shape> orderedShapesByRDFLevel;
    private final Shape parentShape;


    public RDFRankSelectionStrategyGlobalRL(int limit, Set<Shape> orderedShapesByRDFLevel, Shape parentShape) {
        this.limit = limit;
        this.orderedShapesByRDFLevel = orderedShapesByRDFLevel;
        this.parentShape = parentShape;
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

        Map<Integer, List<Shape>> shapesByLevel = new TreeMap<>(Collections.reverseOrder());
        for (Shape shape : orderedShapesByRDFLevel) {
            shapesByLevel.computeIfAbsent(shape.depthlevel, k -> new ArrayList<>()).add(shape);
        }

        List<Shape> reorderedShapes = new ArrayList<>();
        for (List<Shape> shapes : shapesByLevel.values()) {
            reorderedShapes.addAll(shapes);
        }

        Set<Shape> selectedShapes = new LinkedHashSet<>();
        for (Shape candidate : reorderedShapes) {
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
        List<Shape> reversedShapes = new ArrayList<>(orderedShapesByRDFLevel);
        Collections.reverse(reversedShapes);

        for (Shape candidate : reversedShapes) {
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
