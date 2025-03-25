package sid.OntView2.selectionStrategy;

import sid.OntView2.common.Shape;

import java.util.List;
import java.util.Set;

public class RDFRankLevelIdSelectionStrategy implements SelectionStrategy {
    private int limit;
    private List<Shape> orderedShapesByRDF;

    public RDFRankLevelIdSelectionStrategy(int limit, List<Shape> orderedShapesByRDF) {
        this.limit = limit;
        this.orderedShapesByRDF = orderedShapesByRDF;
    }

    @Override
    public Set<Shape> getShapesToVisualize(Set<Shape> shapes) {
        return null;
    }

    @Override
    public Set<Shape> getShapesToHide(Set<Shape> shapes) {
        return null;
    }
}
