package sid.OntView2.common;

import pedviz.algorithms.Sugiyama;
import pedviz.algorithms.sugiyama.SugiyamaNodeView;
import pedviz.graph.Edge;
import pedviz.graph.Graph;
import pedviz.graph.LayoutedGraph;
import pedviz.graph.Node;

import java.util.Map.Entry;

public class GraphReorderBackUp {

	VisGraph vgraph;
	int vgraphWidth;
	int vgraphHeight;

	public GraphReorderBackUp(VisGraph v) {
		vgraph = v;
	}

	public void visualReorder() {
		Graph graph = new Graph();
		cloneGraph(graph, vgraph);

		//
		float minX = Float.MAX_VALUE;
		float maxX = Float.MIN_VALUE;
		float minY = Float.MAX_VALUE;
		float maxY = Float.MIN_VALUE;

		vgraphWidth = vgraph.getWidth();
		vgraphHeight = vgraph.getHeight();  //

		Sugiyama s = new Sugiyama(graph);
		s.getLayoutedGraph();
		s.run();
		LayoutedGraph layoutedGraph = s.getLayoutedGraph();
		SugiyamaNodeView nodeView;
		Object key;
		vgraph.paintframe.stable = true;

		for (Object entry : layoutedGraph.getAllNodes().entrySet()) {
			Entry<String, SugiyamaNodeView> entryCast = (Entry<String, SugiyamaNodeView>) entry;
			nodeView = entryCast.getValue();
			if (nodeView.getPosX() < minX) {
				minX = nodeView.getPosX();
			}
			if (nodeView.getPosX() > maxX) {
				maxX = nodeView.getPosX();
			}
			if (nodeView.getPosY() < minY) {
				minY = nodeView.getPosY();
			}
			if (nodeView.getPosY() > maxY) {
				maxY = nodeView.getPosY();
			}
		}

		for (Object entry : layoutedGraph.getAllNodes().entrySet()) {
			Entry<String, SugiyamaNodeView> entryCast = (Entry<String, SugiyamaNodeView>) entry;
			nodeView = entryCast.getValue();
			key = entryCast.getKey();
			if (key instanceof String) {
				String key2 = key.toString();
				Shape shape = vgraph.shapeMap.get(key2);
				if (shape != null) {
					shape.setPosX(translateRelativePosX(nodeView.getPosX(), minX, maxX));
					shape.setPosY(translateRelativePosY(nodeView.getPosY(), minY, maxY));
				}
			}
		}
	}

	public int translateRelativePosX(float in, float low, float high) {
		float total = high - low;
		float perOne = (in - low) / total;
		return (int) (vgraphWidth * perOne);
	}

	public int translateRelativePosY(float in, float low, float high) {
		float total = high - low;
		float perOne = (in - low) / total;
		return (int) (vgraphHeight * perOne);
	}

	public static void cloneGraph(Graph graph, VisGraph vgraph) {
		for (Entry<String, Shape> entry : vgraph.shapeMap.entrySet()) {
			if (!(entry.getValue().outConnectors.isEmpty()) || (!(entry.getValue().inConnectors.isEmpty()))) {
				Node n = new Node(entry.getKey());
				graph.addNode(n);
			}
		}
		for (Entry<String, Shape> entry : vgraph.shapeMap.entrySet()) {
			Shape shape = entry.getValue();
			for (VisConnector c : shape.outConnectors) {
				if (!c.isRedundant()) {
					Node or = graph.getNode(entry.getKey());
					Node dst = graph.getNode(Shape.getKey(c.to.getLinkedClassExpression()));
					dst.setIdDad(or.getId());
					graph.addEdge(new Edge(or, dst));
				}
			}
		}

	}
}
