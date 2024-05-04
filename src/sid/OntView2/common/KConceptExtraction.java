package sid.OntView2.common;

import it.essepuntato.semanticweb.kce.engine.Engine;
import it.essepuntato.taxonomy.HTaxonomy;
import it.essepuntato.taxonomy.maker.OWLAPITaxonomyMaker;
import org.semanticweb.owlapi.model.OWLOntology;
import sid.OntView2.utils.OWLAPITaxonomyMakerExtended;

import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class KConceptExtraction {
	
	
	/**
	 * 
	 * @param activeOntology
	 * @param graph
	 * @param limitResultSize
	 */

	public static void hideNonKeyConcepts(OWLOntology activeOntology, VisGraph graph, int limitResultSize){
        //first retrieve Key Concepts
		Map<String, Shape> shapeMap = graph.getShapeMap();
		
		Set<String> conceptSet = retrieveKeyConcepts(activeOntology,limitResultSize);
		for (Entry<String, Shape> entry : shapeMap.entrySet()){
			Shape shape = entry.getValue();
			if (isNonKeyConcept(entry.getKey(),conceptSet,shapeMap)){
				shape.hide();
			}
		}
	    graph.addDashedConnectors();

	}
		 
	/**
	 * Replaced by overloaded version
	 * Retrieves a set of Key Concepts to be shown by using KCE Api
 	 * @param activeOntology
	 * @param graph
	 * @return
	 */
    private static Set<String> retrieveKeyConcepts(OWLOntology activeOntology, VisGraph graph, int limitResultSize){
			 boolean considerImportedOntologies = true; /* True if you want t*/
			 String source = graph.getPaintFrame().getActiveOntologySource();
			 if (source != null) {
				 HTaxonomy ht = new OWLAPITaxonomyMaker(URI.create(source).toString(), considerImportedOntologies).makeTaxonomy();
				 Engine e = new Engine(ht);
				 e.setNumberOfKeyConceptsToExtract(limitResultSize);
				 e.run();
				 Set<String> conceptSet = e.getKeyConcepts();
				 conceptSet.add(VisConstants.THING_ENTITY);
				 return conceptSet;
			    
			 }
			 else {
				 System.err.println("hideNonKeyConcepts is WIP in protege version. Check source string");
				 return null;
			 }
	}

    
    private static Set<String> retrieveKeyConcepts(OWLOntology activeOntology,int limitResultSize){
			 boolean considerImportedOntologies = true; /* True if you want t*/
			
			 if (activeOntology != null) {
				 HTaxonomy ht = new OWLAPITaxonomyMakerExtended(activeOntology, considerImportedOntologies).makeTaxonomy();
				 Engine e = new Engine(ht);
				 e.setNumberOfKeyConceptsToExtract(limitResultSize);
				 e.run();
				 Set<String> conceptSet = e.getKeyConcepts();
				 conceptSet.add(VisConstants.THING_ENTITY);
				 return conceptSet;
			    
			 }
			 else {
				 System.err.println("hideNonKeyConcepts is WIP in protege version. Check source string");
				 return null;
			 }
	}

    
    
    
    /**
     * Expanded condition. "concept" is a key concept if its contained in keyConcepts
     * or it's a definition of any of its contents.
     * @param concept
     * @param keyConcepts
     * @param shapeMap
     * @return boolean
     */
    private static boolean isNonKeyConcept(String concept,Set<String> keyConcepts, Map<String, Shape> shapeMap){
    	Shape s = shapeMap.get(concept);
    	boolean isNonKeyConcept = true;
    	if (keyConcepts.contains(concept)){
    		isNonKeyConcept = false;
    	}
    	else { 
    		if (s instanceof VisClass){
    		   if ((s.asVisClass().isAnonymous) && ( isKeyConceptDefinition(s, keyConcepts,shapeMap))){
    			   isNonKeyConcept = false;
    		   }
    		}
    		
    	}
    	
    	return isNonKeyConcept;
    }
    /**
     * Returns true if concept s is a definition of any of the comprised concepts
     * in keyConcepts
     * @param s
     * @param keyConcepts
     * @return
     */
    private static boolean isKeyConceptDefinition(Shape s, Set<String> keyConcepts, Map<String, Shape> shapeMap){
    	for (Shape childrenCandidate : s.asVisClass().getChildren()){
    		if (keyConcepts.contains(Shape.getKey(childrenCandidate.getLinkedClassExpression()))){
    			return true;
    		}	
    	}
    	return false;
    }
}
