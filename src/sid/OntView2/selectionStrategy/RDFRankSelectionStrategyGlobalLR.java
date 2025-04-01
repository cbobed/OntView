package sid.OntView2.selectionStrategy;

import sid.OntView2.common.Shape;

import java.util.*;

public class RDFRankSelectionStrategyGlobalLR implements SelectionStrategy {
    private final int limit;
    private final Set<Shape> orderedShapesByRDFLevel;

    public RDFRankSelectionStrategyGlobalLR(int limit, Set<Shape> orderedShapesByRDFLevel) {
        this.limit = limit;
        this.orderedShapesByRDFLevel = orderedShapesByRDFLevel;
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
        int numberToShow = (int) Math.ceil((limit / 100.0) * orderedShapesByRDFLevel.size());

        Map<Integer, List<Shape>> shapesByLevel = new HashMap<>();
        for (Shape shape : orderedShapesByRDFLevel) {
            shapesByLevel.computeIfAbsent(shape.depthlevel, k -> new ArrayList<>()).add(shape);
        }

        List<Shape> reversedShapes = new ArrayList<>();
        for (List<Shape> shapes : shapesByLevel.values()) {
            Collections.reverse(shapes);
            reversedShapes.addAll(shapes);
        }

        Set<Shape> selectedShapes = new LinkedHashSet<>();
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
