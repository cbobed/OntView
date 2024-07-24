package sid.OntView.common;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.Map.Entry;

import org.apache.jena.riot.RDFDataMgr;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.semanticweb.owlapi.model.OWLOntology;

//import com.hp.hpl.jena.rdf.model.Model;
//import com.hp.hpl.jena.rdf.model.RDFNode;
//import com.hp.hpl.jena.rdf.model.Selector;
//import com.hp.hpl.jena.rdf.model.SimpleSelector;
//import com.hp.hpl.jena.rdf.model.Statement;
//import com.hp.hpl.jena.rdf.model.StmtIterator;

import sid.OntView.utils.JarClassLoader;
import sid.OntView.utils.PageRankScoreComparator;

public abstract class RDFRankConceptExtraction {
		
	public final static boolean RDFRankDebug = true; 
	
	
	public static void hideNonKeyConcepts(OWLOntology activeOntology,VisGraph graph,int limitResultSize, boolean bidirectional){
		
		//first retrieve Key Concepts
		Map<String, Shape> shapeMap = graph.getShapeMap();
		
		Set<String> conceptSet = retrieveKeyConcepts(activeOntology,shapeMap, limitResultSize, bidirectional);
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
		
		// Implementation with JGraphT => clearer and cleaner, but we cannot use it as there is a class clash
		// given the versions of jGraph ... 
		
//		Map<String, Shape> shapeMap = graph.getShapeMap();
//		DefaultDirectedGraph<String, String> pageRankGraph = new DefaultDirectedGraph<String, String>(String.class); 
//				
//		for (Entry<String, Shape> entry : shapeMap.entrySet()){
//			VisClass node = entry.getValue().asVisClass();
//			String nodeStr = Shape.getKey(node.getLinkedClassExpression());
//			pageRankGraph.addVertex(nodeStr); 
//			for (Shape ch: node.getChildren()) {
//				String childNodeStr = Shape.getKey(ch.getLinkedClassExpression()); 
//				pageRankGraph.addVertex(childNodeStr);  
//				pageRankGraph.addEdge(childNodeStr, nodeStr, "<rdfs:subclassOf>"); 
//				if (bidirectional) {
//					pageRankGraph.addEdge(nodeStr, childNodeStr, "<rdfs:superClassOf>");
//				}
//			}
//		}
		
		Set<String> results = new HashSet<String> (); 
		
		// first, build the graph to obtain the centrality of the nodes
		StringBuilder strBuilder = new StringBuilder(); 
		for (Entry<String, Shape> entry : shapeMap.entrySet()){
			VisClass node = entry.getValue().asVisClass();
			String nodeStr = Shape.getKey(node.getLinkedClassExpression()); 
			for (Shape ch: node.getChildren()) {
				strBuilder.append("<"+Shape.getKey(ch.getLinkedClassExpression())+"> <rdfs:subClassOf> <"+nodeStr+"> . \n"); 
				if (bidirectional) {
					strBuilder.append("<"+nodeStr+"> <rdfs:superClassOf> <"+Shape.getKey(ch.getLinkedClassExpression())+"> . \n");
				}
			}
		}
		
	
		String filename = "tmp-"+limitResultSize+"-"+bidirectional+".nt";
		String outputFilename = "tmp-"+limitResultSize+"-"+bidirectional+"-pageRankScores.nt";
		try (PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(filename)))) {
			out.println(strBuilder.toString()); 
		}
		catch (IOException e) {
			System.err.println("Debugging the RDF ... not being able to write the file"); 
			e.printStackTrace();
		}
	
		try {
			JarClassLoader classLoader = new JarClassLoader(Paths.get("./lib/pagerank-0.1.0.jar").toUri().toURL());
			String mainClassName = classLoader.getMainClassName(); 
			String[] jarArgs = {"-in", filename, "-out", outputFilename}; 
			classLoader.invokeClass(mainClassName, jarArgs); 
			
			ArrayList<SimpleEntry<String, Float>> pageRankScoresList = new ArrayList<>(); 
			
			RDFParser rdfParser = Rio.createParser(RDFFormat.NTRIPLES); 
			Model model = new LinkedHashModel(); 
			rdfParser.setRDFHandler(new StatementCollector(model));
			URL documentURL = (Paths.get(outputFilename)).toUri().toURL();
			InputStream inputStream = documentURL.openStream();
			
			rdfParser.parse(inputStream, documentURL.toString());
			
			for (Statement stmt: model) {
				String subj = stmt.getSubject().stringValue(); 
				Float obj = ((Literal)stmt.getObject()).floatValue(); 
				pageRankScoresList.add(new SimpleEntry<String, Float> (subj,obj)); 
			}
			Collections.sort(pageRankScoresList, new PageRankScoreComparator());
			
			for (int i=0; i<limitResultSize && i<pageRankScoresList.size(); i++) {
				// this should be done via the global logging mechanism 
				if (RDFRankDebug ) {
					System.out.println(pageRankScoresList.get(i).getKey()+" :: "+pageRankScoresList.get(i).getValue()); 
				}
				results.add(pageRankScoresList.get(i).getKey()); 
			}
			
		}
		catch (MalformedURLException | InvocationTargetException | ClassNotFoundException | NoSuchMethodException e) {
			System.err.println("Error regading the invocation of the external pageRank algorithm"); 
			System.err.println(e.getCause()); 
			System.err.println(e.getMessage()); 
		}
		catch (IOException e) {
			System.err.println("I/O Error during the invocation of the external pageRank algorithm"); 
			System.err.println(e.getCause()); 
			System.err.println(e.getMessage());
		}
		
//		Code for handling this with Jena ... again, due to old dependencies, this is not to be used 
		
//		Model pageRankScoresRDF = RDFDataMgr.loadModel(outputFilename); 
//		ArrayList<SimpleEntry<String, Float>> pageRankScoresList = new ArrayList<>(); 
//		Selector sel = new SimpleSelector(null, pageRankScoresRDF.getProperty("<http://purl.org/voc/vrank#pagerank>"), (RDFNode) null);  
//		StmtIterator triples = pageRankScoresRDF.listStatements(sel);
//		Statement stmt = null; 
//		while (triples.hasNext()) {
//			stmt = triples.next(); 
//			pageRankScoresList.add(new SimpleEntry<String, Float> (stmt.getSubject().getURI().toString(), 
//																	stmt.getFloat())); 
//		}
//		Collections.sort(pageRankScoresList, new PageRankScoreComparator());
//		
//		for (int i=0; i<limitResultSize && i<pageRankScoresList.size(); i++) {
//			// this should be done via the global logging mechanism 
//			if (RDFRankDebug ) {
//				System.out.println(pageRankScoresList.get(i).getKey()+" :: "+pageRankScoresList.get(i).getValue()); 
//			}
//			results.add(pageRankScoresList.get(i).getKey()); 
//		}
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
