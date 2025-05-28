package sid.OntView2.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pedviz.algorithms.Sugiyama;
import pedviz.algorithms.sugiyama.SugiyamaNodeView;
import pedviz.graph.Edge;
import pedviz.graph.Graph;
import pedviz.graph.LayoutedGraph;
import pedviz.graph.Node;
import sid.OntView2.expressionNaming.SIDClassExpressionNamer;

import java.util.Map.Entry;

public class GraphReorder {
    private static final Logger logger = LogManager.getLogger(GraphReorder.class);
	VisGraph vGraph;
	int vGraphHeight;
	public GraphReorder(VisGraph v){
		vGraph = v;
	}

	public void visualReorder(){
		Graph graph = new Graph();
		cloneGraph(graph, vGraph);
		
		float minY = 0;
		float maxY = 0;
		vGraphHeight = vGraph.getHeight();
		//Step 2
        logger.debug("-->Sugiyama");
		Sugiyama s = new Sugiyama(graph);
		s.getLayoutedGraph();
        logger.debug("-->Sugiyama - run");
		s.run();
        logger.debug("<--Sugiyama");
		@SuppressWarnings("rawtypes")
		LayoutedGraph layoutedGraph = s.getLayoutedGraph();
		SugiyamaNodeView nodeView;
		Object key;
		vGraph.paintframe.stable = true;

		for(Object entry: layoutedGraph.getAllNodes().entrySet()){
			@SuppressWarnings("unchecked")
			Entry<String,SugiyamaNodeView> entryCast = (Entry<String,SugiyamaNodeView>) entry;
			nodeView = entryCast.getValue();
			if (nodeView.getPosX()< minY) {minY = nodeView.getPosX();}
			if (nodeView.getPosX()> maxY) {maxY = nodeView.getPosX();}
		}

		for(Object entry: layoutedGraph.getAllNodes().entrySet()){
			@SuppressWarnings("unchecked")

			Entry<String,SugiyamaNodeView> entryCast = (Entry<String,SugiyamaNodeView>) entry;
			nodeView = entryCast.getValue();
			key = entryCast.getKey();
			if (key != null){
				String key2 = key.toString();
				Shape shape = vGraph.shapeMap.get(key2);
				if (shape !=null && shape.isVisible()){
					shape.setPosY(translateRelativePos(nodeView.getPosX(), minY, maxY));

				}
			}
		}
	}

	/**
	 * gets the relative equivalent point in visGraph
	 */
	public int translateRelativePos(float in, float low,float high){

		float total = high-low;
		float perOne = (in-low)/total;
        float spacingFactor = 2f;

		return (int) (vGraphHeight * perOne * spacingFactor);
	}

	public static void cloneGraph(Graph graph, VisGraph vGraph){
        logger.debug("-->clone graph");
		for (Entry<String, Shape> entry : vGraph.shapeMap.entrySet()){
			Shape shape = entry.getValue();
			if (!(shape.outConnectors.isEmpty()) ||(!(shape.inConnectors.isEmpty()))){
				Node n = new Node(entry.getKey());
				graph.addNode(n);
			}
		}
		for (Entry<String, Shape> entry : vGraph.shapeMap.entrySet()){
			Shape shape = entry.getValue();
			for (VisConnector c : shape.outConnectors){
				if (!c.isRedundant()){
					Node or  = graph.getNode(entry.getKey());
					Node dst = graph.getNode(Shape.getKey(c.to.getLinkedClassExpression()));
                    if (dst == null) {
                        logger.debug("or: {} c.to {} {}",
                                entry.getValue().getLabel(), c.to.getLabel(), c.to.getLinkedClassExpression());
                    }
                    dst.setIdDad(or.getId());
					graph.addEdge(new Edge(or,dst));
				}
			}
		}
        logger.debug("<--clone graph");
	}
}