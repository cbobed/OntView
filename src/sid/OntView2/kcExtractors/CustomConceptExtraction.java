package sid.OntView2.kcExtractors;

import org.semanticweb.owlapi.model.OWLOntology;
import sid.OntView2.common.Shape;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CustomConceptExtraction extends KConceptExtractor {
    private final Set<Shape> selectedConcepts;

    public CustomConceptExtraction(Set<Shape> selectedConcepts) {
        this.selectedConcepts = selectedConcepts;
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