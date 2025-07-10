package sid.OntView2.kcExtractors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;

import sid.OntView2.common.Shape;
import sid.OntView2.common.VisClass;
import sid.OntView2.utils.PageRankScoreComparator;


import java.util.*;
import java.util.Map.Entry;

public class PageRankConceptExtraction extends KConceptExtractor {
    private static final Logger logger = LogManager.getLogger(PageRankConceptExtraction.class);
    public final static boolean PageRankRankGraph = true;

    @Override
    public Set<String> retrieveKeyConcepts(OWLOntology activeOntology, Map<String, Shape> shapeMap,
                                           int limitResultSize) {
        // Implementation with JGraphT => clearer and cleaner
        DefaultDirectedGraph<String, DefaultEdge> pageRankGraph = new DefaultDirectedGraph<>(DefaultEdge.class);

        for (Shape value: shapeMap.values()) {
        	VisClass node = value.asVisClass();
        	String nodeStr = Shape.getKey(node.getLinkedClassExpression());
            pageRankGraph.addVertex(nodeStr);
            for (Shape ch: node.getChildren()) {
                String childNodeStr = Shape.getKey(ch.getLinkedClassExpression());
                pageRankGraph.addVertex(childNodeStr);
                pageRankGraph.addEdge(childNodeStr, nodeStr);
            }
        }

        Set<String> results = new HashSet<> ();
        PageRank<String, DefaultEdge> pageRankScorer = new PageRank<>(pageRankGraph);

        if (PageRankRankGraph) {
            logger.debug("Vertices: {}", pageRankGraph.vertexSet().size());
            logger.debug("Edges: {}", pageRankGraph.edgeSet().size());
            logger.debug(PageRank.DAMPING_FACTOR_DEFAULT);
            logger.debug(PageRank.MAX_ITERATIONS_DEFAULT);
            logger.debug(PageRank.TOLERANCE_DEFAULT);
        }

        Map<String, Double> scoreMap = pageRankScorer.getScores();
        ArrayList<Entry<String, Double>> scoreList = new ArrayList<>();

        scoreMap.entrySet().forEach(scoreList::add);
        scoreList.sort(Collections.reverseOrder(new PageRankScoreComparator()));

        int added = 0;
        for (int i=0; added<limitResultSize && i<scoreList.size(); i++) {
            if (PageRankRankGraph) {
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
}
