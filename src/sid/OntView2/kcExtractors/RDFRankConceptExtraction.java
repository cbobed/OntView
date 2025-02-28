package sid.OntView2.kcExtractors;

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
import org.semanticweb.owlapi.model.OWLOntology;

import sid.OntView2.common.Shape;
import sid.OntView2.common.VisClass;
import sid.OntView2.common.VisConstants;
import sid.OntView2.common.VisGraph;
import sid.OntView2.utils.PageRankScoreComparator;

public class RDFRankConceptExtraction extends KConceptExtractor { //true
    private final Set<Shape> nonHiddenShape = new HashSet<>();

    public final static boolean RDFRankDebug = true;
    public void hideNonKeyConcepts(OWLOntology activeOntology, VisGraph graph, int limitResultSize){

        //first retrieve Key Concepts
        Map<String, Shape> shapeMap = graph.getShapeMap();
        Set<String> conceptSet = retrieveKeyConcepts(activeOntology,shapeMap, limitResultSize);

        conceptSet.add(VisConstants.THING_ENTITY);
        conceptSet.add(VisConstants.NOTHING_ENTITY);

        // TODO: we must find the way to force the OWLNothing to appear in the graph
        for (Entry<String, Shape> entry : shapeMap.entrySet()){
            Shape shape = entry.getValue();
            if (isNonKeyConcept(entry.getKey(),conceptSet,shapeMap)){
                shape.hide();
            } else {
                if (!(shape.getLabel().matches("Nothing"))) {
                    nonHiddenShape.add(shape);
                }
            }
        }
        for (Shape s: nonHiddenShape){
            if (s.getState()!=Shape.OPEN) {
                s.updateHiddenDescendants();
            }
        }
        graph.addDashedConnectors();
    }

    @Override
    public Set<String> retrieveKeyConcepts(OWLOntology activeOntology, Map<String, Shape> shapeMap,
                                           int limitResultSize) {
        // Implementation with JGraphT => clearer and cleaner
        DefaultDirectedGraph<String, DefaultEdge> rdfRankGraph = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);

        for (Entry<String, Shape> entry : shapeMap.entrySet()){
            VisClass node = entry.getValue().asVisClass();
            String nodeStr = Shape.getKey(node.getLinkedClassExpression());
            rdfRankGraph.addVertex(nodeStr);
            for (Shape ch: node.getChildren()) {
                String childNodeStr = Shape.getKey(ch.getLinkedClassExpression());
                rdfRankGraph.addVertex(childNodeStr);
                rdfRankGraph.addEdge(childNodeStr, nodeStr);
                rdfRankGraph.addEdge(nodeStr, childNodeStr);

            }
        }

        Set<String> results = new HashSet<> ();
        PageRank<String, DefaultEdge> pageRankScorer = new PageRank<>(rdfRankGraph);

        if (RDFRankDebug) {
            System.out.println("Vertices: "+rdfRankGraph.vertexSet().size());
            System.out.println("Edges: "+rdfRankGraph.edgeSet().size());
            System.out.println(PageRank.DAMPING_FACTOR_DEFAULT);
            System.out.println(PageRank.MAX_ITERATIONS_DEFAULT);
            System.out.println(PageRank.TOLERANCE_DEFAULT);
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
}
