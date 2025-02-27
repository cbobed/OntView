package sid.OntView2.common;

import it.essepuntato.semanticweb.kce.engine.Engine;
import it.essepuntato.taxonomy.HTaxonomy;
import org.semanticweb.owlapi.model.OWLOntology;
import sid.OntView2.utils.OWLAPITaxonomyMakerExtended;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CustomConceptExtraction extends KConceptExtractor {

    private final Set<Shape> nonHiddenShape = new HashSet<>();
    private final Set<Shape> selectedConcepts;

    public CustomConceptExtraction(Set<Shape> selectedConcepts) {
        this.selectedConcepts = selectedConcepts;
    }

    public void hideNonKeyConcepts(OWLOntology activeOntology, VisGraph graph, int limitResultSize) {
        // Retrieve Key Concepts
        Map<String, Shape> shapeMap = graph.getShapeMap();
        Set<String> conceptSet = retrieveKeyConcepts(activeOntology, shapeMap, limitResultSize);

        conceptSet.add(VisConstants.THING_ENTITY);
        conceptSet.add(VisConstants.NOTHING_ENTITY);

        for (Map.Entry<String, Shape> entry : shapeMap.entrySet()) {
            Shape shape = entry.getValue();
            if (isNonKeyConcept(entry.getKey(), conceptSet, shapeMap)) {
                shape.hide();
            } else {
                if (!(shape.getLabel().matches("Nothing"))) {
                    nonHiddenShape.add(shape);
                }
            }
        }
        for (Shape s: nonHiddenShape){
            if (s.getState()!=Shape.OPEN) {
                s.updateHiddenDescendants();
            }
        }
        graph.addDashedConnectors();
    }

    /**
     * Retrieves a set of Key Concepts to be shown
     */
    @Override
    public Set<String> retrieveKeyConcepts(OWLOntology activeOntology, Map<String, Shape> shapeMap, int limitResultSize) {
        Set<String> keyConcepts = new HashSet<>();

        Map<Shape, String> shapeToKeyMap = new HashMap<>();
        for (Map.Entry<String, Shape> entry : shapeMap.entrySet()) {
            shapeToKeyMap.put(entry.getValue(), entry.getKey());
        }

        for (Shape shape : selectedConcepts) {
            String key = shapeToKeyMap.get(shape);
            if (key != null) {
                keyConcepts.add(key);
            }
        }

        return keyConcepts;

    }
}