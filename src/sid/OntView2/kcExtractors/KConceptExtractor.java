package sid.OntView2.kcExtractors;

import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;

import sid.OntView2.common.Shape;
import sid.OntView2.common.VisClass;
import sid.OntView2.common.VisConstants;
import sid.OntView2.common.VisGraph;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class KConceptExtractor {

    private final Set<Shape> nonHiddenShape = new HashSet<>();

    /**
     * Hides non-key concepts in the graph
     */
    public void hideNonKeyConcepts(OWLOntology activeOntology, VisGraph graph, int limitResultSize){
        Map<String, Shape> shapeMap = graph.getShapeMap();
        Set<String> conceptSet = retrieveKeyConcepts(activeOntology, shapeMap, limitResultSize);

        conceptSet.add(VisConstants.THING_ENTITY);
        conceptSet.add(VisConstants.NOTHING_ENTITY);

        for (Map.Entry<String, Shape> entry : shapeMap.entrySet()) {
            Shape shape = entry.getValue();
            if (isNonKeyConcept(entry.getKey(), conceptSet, shapeMap)) {
                shape.hide();
            } else {
                if (!shape.asVisClass().isBottom) {
                    nonHiddenShape.add(shape);
                }
            }
        }
        for (Shape s: nonHiddenShape){
            s.updateHiddenDescendants();
        }
        graph.addDashedConnectors();
    }
	
    /**
     * Retrieves the key concepts according to the implemented criteria
     */
    public abstract Set<String> retrieveKeyConcepts(OWLOntology activeOntology, Map<String, Shape> shapeMap,
                                           int limitResultSize);

    /**
     * Expanded condition. "concept" is a key concept if it's contained in keyConcepts,
     * or it's a definition of its contents.
     * @return boolean
     */
    protected boolean isNonKeyConcept(String concept, Set<String> keyConcepts, Map<String, Shape> shapeMap) {
        Shape s = shapeMap.get(concept);
        boolean isNonKeyConcept = true;
        if (keyConcepts.contains(concept)) {
            isNonKeyConcept = false;
        } else {
            if (s instanceof VisClass) {
                if ((s.asVisClass().isAnonymous()) && (isKeyConceptEquivalent(s, keyConcepts))) {
                    isNonKeyConcept = false;
                }
            }
        }
        return isNonKeyConcept;
    }

    /**
     * Returns true if concept s is a definition of the comprised concepts
     * in keyConcepts
     * @return boolean
     */
    protected boolean isKeyConceptEquivalent(Shape s, Set<String> keyConcepts) {
    	
    	Set<String> auxDefinitions = new HashSet<>();  
    	s.asVisClass().getEquivalentClasses().forEach(x -> {auxDefinitions.add(Shape.getKey(x));}); 
    	// intersection of definitions of the shape and the keyConcepts themselves 
    	auxDefinitions.retainAll(keyConcepts); 
    	
    	return !auxDefinitions.isEmpty();  
    	
    }
}