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

    public static String getStrategyOption(){
        return "RDFRankPartial";
    }

    public static String getStrategyOptionSlider(){
        return "RDFRank";
    }
}
