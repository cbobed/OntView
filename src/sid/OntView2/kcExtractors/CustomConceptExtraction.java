package sid.OntView2.kcExtractors;

import org.semanticweb.owlapi.model.OWLOntology;
import sid.OntView2.common.Shape;

import java.util.*;
import java.util.stream.Collectors;

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

        Map<Shape, List<String>> shapeToKeyMap = shapeMap.entrySet().stream()
            .collect(Collectors.groupingBy(
                Map.Entry::getValue,
                Collectors.mapping(
                    Map.Entry::getKey,
                    Collectors.toList()
                )
            ));

        for (Shape shape : selectedConcepts) {
            List<String> key = shapeToKeyMap.get(shape);
            if (key != null) {
                keyConcepts.addAll(key);
            }
        }
        
        return keyConcepts;

    }
}