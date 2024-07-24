package sid.OntView2.common;

import org.semanticweb.owlapi.model.OWLOntology;
import java.util.Map;
import java.util.Set;

public abstract class AbstractConceptExtractor {

    public abstract Set<String> retrieveKeyConcepts(OWLOntology activeOntology, int limitResultSize);


    public void hideNonKeyConcepts(OWLOntology activeOntology, VisGraph graph, int limitResultSize) {
        // Retrieve Key Concepts
        Map<String, Shape> shapeMap = graph.getShapeMap();
        Set<String> conceptSet = retrieveKeyConcepts(activeOntology, limitResultSize);

        for (Map.Entry<String, Shape> entry : shapeMap.entrySet()) {
            Shape shape = entry.getValue();
            if (isNonKeyConcept(entry.getKey(), conceptSet, shapeMap)) {
                shape.hide();
            }
        }
        graph.addDashedConnectors();
    }

    /**
     * Expanded condition. "concept" is a key concept if it's contained in keyConcepts,
     * or it's a definition of its contents.
     * @param concept
     * @param keyConcepts
     * @param shapeMap
     * @return boolean
     */
    protected boolean isNonKeyConcept(String concept, Set<String> keyConcepts, Map<String, Shape> shapeMap) {
        Shape s = shapeMap.get(concept);
        boolean isNonKeyConcept = true;
        if (keyConcepts.contains(concept)) {
            isNonKeyConcept = false;
        } else {
            if (s instanceof VisClass) {
                if ((s.asVisClass().isAnonymous) && (isKeyConceptDefinition(s, keyConcepts, shapeMap))) {
                    isNonKeyConcept = false;
                }
            }
        }
        return isNonKeyConcept;
    }

    /**
     * Returns true if concept s is a definition of the comprised concepts
     * in keyConcepts
     * @param s
     * @param keyConcepts
     * @return
     */
    protected boolean isKeyConceptDefinition(Shape s, Set<String> keyConcepts, Map<String, Shape> shapeMap) {
        for (Shape childrenCandidate : s.asVisClass().getChildren()) {
            if (keyConcepts.contains(Shape.getKey(childrenCandidate.getLinkedClassExpression()))) {
                return true;
            }
        }
        return false;
    }
}