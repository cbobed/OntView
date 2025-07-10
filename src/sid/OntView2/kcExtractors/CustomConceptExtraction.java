package sid.OntView2.kcExtractors;

import org.semanticweb.owlapi.model.OWLOntology;
import sid.OntView2.common.CustomKCEModal;
import sid.OntView2.common.Shape;

import java.util.*;
import java.util.stream.Collectors;

public class CustomConceptExtraction extends KConceptExtractor {
    private Set<Shape> selectedConcepts;
    private final Map<String, Shape> shapeMap;
    public boolean isClassExpressionUsed = false;

    public CustomConceptExtraction(Map<String, Shape> shapeMap) {
        this.selectedConcepts = new HashSet<>();
        this.shapeMap = shapeMap;
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

    /**
     * Shows a popup for selecting concepts or find the selected nodes in the visual graph.
     */
    CustomKCEModal modal;
    public void showConceptSelectionPopup() {
        if (isClassExpressionUsed) {
            findSelectedNodesVisGraph(selectedConcepts);
            return;
        }

        if(selectedConcepts.isEmpty()) {
            modal = new CustomKCEModal(shapeMap, this);
        }
        modal.showConceptSelectionPopup();
    }

    public Set<Shape> getSelectedConcepts() { return selectedConcepts; }

    /**
     * Finds the selected nodes in the visual graph and adds them to the selectedConcepts set.
     */
    public void findSelectedNodesVisGraph(Set<Shape> selectedConcepts) {
        if (!selectedConcepts.isEmpty()) {
            Set<Shape> conceptsToFind = new HashSet<>(selectedConcepts);
            this.selectedConcepts.clear();

            for (Shape s : conceptsToFind) {
                if (s != null)
                    this.selectedConcepts.add(s.getGraph().getVisualExtension(s.getLinkedClassExpression()));
            }
        }

    }
}