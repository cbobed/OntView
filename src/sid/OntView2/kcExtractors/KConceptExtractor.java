package sid.OntView2.kcExtractors;

import org.semanticweb.owlapi.model.OWLOntology;

import sid.OntView2.common.Shape;
import sid.OntView2.common.VisClass;
import sid.OntView2.common.VisGraph;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class KConceptExtractor {

	/**
     * Hides non-key concepts in the graph
     * @param activeOntology
     * @param shapeMap
     * @param limitResultSize
     * @return Set<String>
     */
    
    public abstract void hideNonKeyConcepts(OWLOntology activeOntology, VisGraph graph, int limitResultSize); 
	
    /**
     * Retrieves the key concepts according to the implemented criteria
     * @param activeOntology
     * @param shapeMap
     * @param limitResultSize
     * @return Set<String>
     */
	
    public abstract Set<String> retrieveKeyConcepts(OWLOntology activeOntology, Map<String, Shape> shapeMap,
                                           int limitResultSize);

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
                if ((s.asVisClass().isAnonymous()) && (isKeyConceptDefinition(s, keyConcepts, shapeMap))) {
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
    	
    	Set<String> auxDefinitions = new HashSet<>();  
    	s.asVisClass().getEquivalentClasses().forEach(x -> {auxDefinitions.add(Shape.getKey(x));}); 
    	// intersection of definitions of the shape and the keyConcepts themselves 
    	auxDefinitions.retainAll(keyConcepts); 
    	
    	return !auxDefinitions.isEmpty();  
    	
    }
}