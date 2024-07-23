package sid.OntView.common;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.semanticweb.owlapi.model.OWLOntology;

import sid.OntView.utils.JarClassLoader;
import sid.OntView.utils.PageRankScoreComparator;

public abstract class RDFRankConceptExtraction {
		
	public final static boolean RDFRankDebug = true; 
	
	public static void hideNonKeyConcepts(OWLOntology activeOntology,VisGraph graph,int limitResultSize, boolean bidirectional){
		
//		Map<String, Shape> shapeMap = graph.getShapeMap();
//		// first, build the graph to obtain the centrality of the nodes
//		StringBuilder strBuilder = new StringBuilder(); 
//		for (Entry<String, Shape> entry : shapeMap.entrySet()){
//			VisClass node = entry.getValue().asVisClass();
//			String nodeStr = Shape.getKey(node.getLinkedClassExpression()); 
//			for (Shape ch: node.getChildren()) {
//				strBuilder.append("<"+Shape.getKey(ch.getLinkedClassExpression())+"> <rdfs:subClassOf> <"+nodeStr+"> . \n"); 
//				if (bidirectional) {
//					strBuilder.append("<"+nodeStr+"> <rdfs:superClassOf> <"+Shape.getKey(ch.getLinkedClassExpression())+"> . \n");
//				}
//			}
//		}
//		
//	
//		String filename = "tmp-"+limitResultSize+"-"+bidirectional+".nt"; 
//		try (PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(filename)))) {
//			out.println(strBuilder.toString()); 
//		}
//		catch (IOException e) {
//			System.err.println("Debugging the RDF ... not being able to write the file"); 
//			e.printStackTrace();
//		}
//	
//		JarClassLoader classLoader = new 
		

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
