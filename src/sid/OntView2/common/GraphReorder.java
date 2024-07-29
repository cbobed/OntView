package sid.OntView2.common;

import pedviz.algorithms.Sugiyama;
import pedviz.algorithms.sugiyama.SugiyamaNodeView;
import pedviz.graph.Edge;
import pedviz.graph.Graph;
import pedviz.graph.LayoutedGraph;
import pedviz.graph.Node;

import java.util.Map.Entry;


public class GraphReorder {

	VisGraph vgraph;
	int vgraphHeight;
	public GraphReorder(VisGraph v){
		vgraph = v;
	}

	public void visualReorder(){
		Graph graph = new Graph();
		cloneGraph(graph, vgraph);

		float minY = 0;
		float maxY = 0;
		vgraphHeight = vgraph.getHeight();
		//Step 2
		Sugiyama s = new Sugiyama(graph);
		s.getLayoutedGraph();
		s.run();
		@SuppressWarnings("rawtypes")
		LayoutedGraph layoutedGraph = s.getLayoutedGraph();
		SugiyamaNodeView nodeView;
		Object key;
		vgraph.paintframe.stable = true;

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
			if (key instanceof String){
				String key2 = key.toString();
				Shape shape = vgraph.shapeMap.get(key2);
				if (shape !=null && shape.isVisible()){
					shape.setPosY(translateRelativePos(nodeView.getPosX(), minY, maxY));

				}
			}
		}
	}

	/**
	 * gets the relative equivalent point in visgraph
	 */
	public int translateRelativePos(float in, float low,float high){

		float total = high-low;
		float perOne = (in-low)/total;

		return (int) (vgraphHeight * perOne);
	}

	public static void cloneGraph2(Graph graph, VisGraph vgraph){
		for (Entry<String, Shape> entry : vgraph.shapeMap.entrySet()){
			if (!(entry.getValue().outConnectors.isEmpty()) ||(!(entry.getValue().inConnectors.isEmpty()))){
				Node n = new Node(entry.getKey());
				graph.addNode(n);
			}
		}
		for (Entry<String, Shape> entry : vgraph.shapeMap.entrySet()){
			Shape shape = entry.getValue();
			for (VisConnector c : shape.outConnectors){
				if (!c.isRedundant()){
					Node or  = graph.getNode(entry.getKey());
					Node dst = graph.getNode(Shape.getKey(c.to.getLinkedClassExpression()));
					dst.setIdDad(or.getId());
					graph.addEdge(new Edge(or,dst));
				}
			}
		}
	}

	public static void cloneGraph(Graph graph, VisGraph vgraph){
		for (Entry<String, Shape> entry : vgraph.shapeMap.entrySet()){
			if (entry.getValue().isVisible() &&
					(!(entry.getValue().outConnectors.isEmpty()) || !(entry.getValue().inConnectors.isEmpty()))){
				Node n = new Node(entry.getKey());
				graph.addNode(n);
			}
		}
		for (Entry<String, Shape> entry : vgraph.shapeMap.entrySet()){
			Shape shape = entry.getValue();
			if (shape.isVisible()) {
				for (VisConnector c : shape.outConnectors){
					if (!c.isRedundant()){
						Node or  = graph.getNode(entry.getKey());
						Node dst = graph.getNode(Shape.getKey(c.to.getLinkedClassExpression()));
						if (dst != null) {
							dst.setIdDad(or.getId());
							graph.addEdge(new Edge(or, dst));
						}
					}
				}
			}
		}
	}
}