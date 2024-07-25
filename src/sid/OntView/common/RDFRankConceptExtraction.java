package sid.OntView.common;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;

import sid.OntView.utils.PageRankScoreComparator;

public class RDFRankConceptExtraction {
		
	public final static boolean RDFRankDebug = true; 
	
	
	public static void hideNonKeyConcepts(OWLOntology activeOntology,VisGraph graph,int limitResultSize, boolean bidirectional){
		
		//first retrieve Key Concepts
		Map<String, Shape> shapeMap = graph.getShapeMap();
		Set<String> conceptSet = retrieveKeyConcepts(activeOntology,shapeMap, limitResultSize, bidirectional);
		 
		conceptSet.add(VisConstants.THING_ENTITY);
		conceptSet.add(VisConstants.NOTHING_ENTITY); 
		
		// TODO: we must find the way to force the OWLNothing to appear in the graph 
		for (Entry<String,Shape> entry : shapeMap.entrySet()){
			Shape shape = entry.getValue();
			if (isNonKeyConcept(entry.getKey(),conceptSet,shapeMap)){
				shape.hide();
			}
		}
	    graph.addDashedConnectors();
	}
	
	private static Set<String> retrieveKeyConcepts(OWLOntology activeOntology,
												Map<String, Shape> shapeMap,
												int limitResultSize, 
												boolean bidirectional) {
		
		// Implementation with JGraphT => clearer and cleaner
		
		DefaultDirectedGraph<String, DefaultEdge> pageRankGraph = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class); 
				
		for (Entry<String, Shape> entry : shapeMap.entrySet()){
			VisClass node = entry.getValue().asVisClass();
			String nodeStr = Shape.getKey(node.getLinkedClassExpression());
			pageRankGraph.addVertex(nodeStr); 
			for (Shape ch: node.getChildren()) {
				String childNodeStr = Shape.getKey(ch.getLinkedClassExpression()); 
				pageRankGraph.addVertex(childNodeStr);  
				pageRankGraph.addEdge(childNodeStr, nodeStr); 
				if (bidirectional) {
					pageRankGraph.addEdge(nodeStr, childNodeStr);
				}
			}
		}
		
		Set<String> results = new HashSet<String> (); 		
		PageRank<String, DefaultEdge> pageRankScorer = new PageRank<>(pageRankGraph);
		
		if (RDFRankDebug) {
			System.out.println("Vertices: "+pageRankGraph.vertexSet().size()); 
			System.out.println("Edges: "+pageRankGraph.edgeSet().size()); 
			System.out.println(pageRankScorer.DAMPING_FACTOR_DEFAULT); 
			System.out.println(pageRankScorer.MAX_ITERATIONS_DEFAULT); 
			System.out.println(pageRankScorer.TOLERANCE_DEFAULT); 
		}
		
		Map<String, Double> scoreMap = pageRankScorer.getScores(); 
		ArrayList<Entry<String, Double>> scoreList = new ArrayList<>(); 
		
		scoreMap.entrySet().stream().forEach(entry -> scoreList.add(entry)); 
		Collections.sort(scoreList, Collections.reverseOrder(new PageRankScoreComparator())); 
		
		int added = 0; 
		for (int i=0; added<limitResultSize && i<scoreList.size(); i++) {
			if (RDFRankDebug) {
				System.out.println(scoreList.get(i).getKey()+" :: "+scoreList.get(i).getValue()); 
			}
			OWLClassExpression auxExp = shapeMap.get(scoreList.get(i).getKey()).asVisClass().getLinkedClassExpression(); 
			if (!(auxExp.isOWLThing() || auxExp.isOWLNothing())) {
				added++; 
			}
			results.add(scoreList.get(i).getKey()); 
		}
		return results;
	}
	
		
	/// DUPLICATED CODE ... REFACTOR 
	
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
    private static boolean isKeyConceptDefinition(Shape s, Set<String> keyConcepts,Map<String, Shape> shapeMap){
    	for (Shape childrenCandidate : s.asVisClass().getChildren()){
    		if (keyConcepts.contains(Shape.getKey(childrenCandidate.getLinkedClassExpression()))){
    			return true;
    		}	
    	}
    	return false;
    }
    
    public static void main(String[] args) {
    	
    	try {
    		
    		
    		
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    	
	}
}
