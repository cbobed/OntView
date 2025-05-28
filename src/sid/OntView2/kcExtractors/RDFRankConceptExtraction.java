package sid.OntView2.kcExtractors;

import java.util.*;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;

import sid.OntView2.common.Shape;
import sid.OntView2.common.VisClass;
import sid.OntView2.common.VisGraph;
import sid.OntView2.utils.PageRankScoreComparator;

public class RDFRankConceptExtraction extends KConceptExtractor { //true
    private static final Logger logger = LogManager.getLogger(RDFRankConceptExtraction.class);
    public final static boolean RDFRankDebug = true;

    @Override
    public Set<String> retrieveKeyConcepts(OWLOntology activeOntology, Map<String, Shape> shapeMap,
                                           int limitResultSize) {
        // Implementation with JGraphT => clearer and cleaner
        DefaultDirectedGraph<String, DefaultEdge> rdfRankGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
    
        for (Shape value: shapeMap.values()) {
        	VisClass node = value.asVisClass();
        	String nodeStr = Shape.getKey(node.getLinkedClassExpression());
            rdfRankGraph.addVertex(nodeStr);
            for (Shape ch: node.getChildren()) {
                String childNodeStr = Shape.getKey(ch.getLinkedClassExpression());
                rdfRankGraph.addVertex(childNodeStr);
                rdfRankGraph.addEdge(childNodeStr, nodeStr);
                rdfRankGraph.addEdge(nodeStr, childNodeStr);

            }
        }

        Set<String> results = new LinkedHashSet<> ();
        PageRank<String, DefaultEdge> pageRankScorer = new PageRank<>(rdfRankGraph);

        if (RDFRankDebug) {
            logger.debug("Vertices: {}", rdfRankGraph.vertexSet().size());
            logger.debug("Edges: {}", rdfRankGraph.edgeSet().size());
            logger.debug(PageRank.DAMPING_FACTOR_DEFAULT);
            logger.debug(PageRank.MAX_ITERATIONS_DEFAULT);
            logger.debug(PageRank.TOLERANCE_DEFAULT);
        }

        Map<String, Double> scoreMap = pageRankScorer.getScores();

        ArrayList<Entry<String, Double>> scoreList = new ArrayList<>(scoreMap.entrySet());
        scoreList.sort(Collections.reverseOrder(new PageRankScoreComparator()));

        int added = 0;
        for (int i=0; added<limitResultSize && i<scoreList.size(); i++) {
            if (RDFRankDebug) {
                logger.debug("{} :: {}", scoreList.get(i).getKey(), scoreList.get(i).getValue());
            }
            OWLClassExpression auxExp = shapeMap.get(scoreList.get(i).getKey()).asVisClass().getLinkedClassExpression();
            if (!(auxExp.isOWLThing() || auxExp.isOWLNothing())) {
                added++;
            }
            results.add(scoreList.get(i).getKey());
        }
        return results;
    }

    public Set<Shape> getShapesOrderedByRDFRank(OWLOntology activeOntology, VisGraph graph, int limitResultSize) {
        Map<String, Shape> shapeMap = graph.getShapeMap();
        Set<String> rankedKeys = retrieveKeyConcepts(activeOntology, shapeMap, limitResultSize);

        Set<Shape> orderedShapes = new LinkedHashSet<>();
        for (String key : rankedKeys) {
            Shape shape = shapeMap.get(key);
            if (shape != null) {
                orderedShapes.add(shape);
            }
        }
        return orderedShapes;
    }
}
