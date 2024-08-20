package sid.OntView2.common;

import pedviz.algorithms.Sugiyama;
import pedviz.algorithms.sugiyama.SugiyamaNodeView;
import pedviz.graph.Edge;
import pedviz.graph.Graph;
import pedviz.graph.LayoutedGraph;
import pedviz.graph.Node;

import java.util.HashMap;
import java.util.Map.Entry;

public class GraphReorder {

	VisGraph vgraph;
	int vgraphHeight;

	public GraphReorder(VisGraph v) {
		vgraph = v;
	}

	public void visualReorder() {
		Graph graph = new Graph();
		cloneGraph(graph, vgraph);

		// Ajustar posiciones Y de subnodos de supernodos para mantenerlos juntos
		adjustSuperNodeSubNodesPositions(vgraph);

		float minY = 0;
		float maxY = 0;
		vgraphHeight = vgraph.getHeight();

		// Step 2
		Sugiyama s = new Sugiyama(graph);
		s.getLayoutedGraph();
		s.run();
		@SuppressWarnings("rawtypes")
		LayoutedGraph layoutedGraph = s.getLayoutedGraph();
		SugiyamaNodeView nodeView;
		Object key;
		vgraph.paintframe.stable = true;

		for (Object entry : layoutedGraph.getAllNodes().entrySet()) {
			@SuppressWarnings("unchecked")
			Entry<String, SugiyamaNodeView> entryCast = (Entry<String, SugiyamaNodeView>) entry;
			nodeView = entryCast.getValue();
			if (nodeView.getPosX() < minY) {
				minY = nodeView.getPosX();
			}
			if (nodeView.getPosX() > maxY) {
				maxY = nodeView.getPosX();
			}
		}

		for (Object entry : layoutedGraph.getAllNodes().entrySet()) {
			@SuppressWarnings("unchecked")
			Entry<String, SugiyamaNodeView> entryCast = (Entry<String, SugiyamaNodeView>) entry;
			nodeView = entryCast.getValue();
			key = entryCast.getKey();
			if (key instanceof String) {
				String key2 = key.toString();
				Shape shape = vgraph.shapeMap.get(key2);
				if (shape != null && shape.isVisible()) {
					shape.setPosY(translateRelativePos(nodeView.getPosX(), minY, maxY));
				}
			}
		}

		repositionParents();
	}

	/**
	 * Ajusta las posiciones Y de los subnodos de los supernodos para mantenerlos juntos.
	 */
	private void adjustSuperNodeSubNodesPositions(VisGraph vgraph) {
		for (Entry<String, Shape> entry : vgraph.shapeMap.entrySet()) {
			Shape shape = entry.getValue();
			if (shape instanceof SuperNode superNode) {
				int minY = Integer.MAX_VALUE;
				int maxY = Integer.MIN_VALUE;

				for (VisClass subNode : superNode.getSubNodes()) {
					int posY = subNode.getPosY();
					if (posY < minY) {
						minY = posY;
					}
					if (posY > maxY) {
						maxY = posY;
					}
				}

				int middleY = (minY + maxY) / 2;
				int index = 0;
				int spacing = 20;

				for (VisClass subNode : superNode.getSubNodes()) {
					subNode.setPosY(middleY + (index - superNode.getSubNodes().size() / 2) * spacing);
					index++;
				}
			}
		}
	}

	/**
	 * Gets the relative equivalent point in visgraph.
	 */
	public int translateRelativePos(float in, float low, float high) {
		float total = high - low;
		float perOne = (in - low) / total;

		return (int) (vgraphHeight * perOne);
	}

	public static void cloneGraph(Graph graph, VisGraph vgraph) {
		for (Entry<String, Shape> entry : vgraph.shapeMap.entrySet()) {
			Shape shape = entry.getValue();

			if (!(shape.outConnectors.isEmpty()) || !(shape.inConnectors.isEmpty())) {
				if (!(shape instanceof VisClass) || !isPartOfSuperNode(shape, vgraph)) {
					Node n = new Node(entry.getKey());
					graph.addNode(n);

					if (shape instanceof SuperNode superNode) {
						for (VisClass subNode : superNode.getSubNodes()) {
							Node subNodeGraph = new Node(subNode.getKey(subNode.getLinkedClassExpression()));
							graph.addNode(subNodeGraph);
							graph.addEdge(new Edge(n, subNodeGraph));
						}
					}
				}
			}
		}

		for (Entry<String, Shape> entry : vgraph.shapeMap.entrySet()) {
			Shape shape = entry.getValue();
			for (VisConnector c : shape.outConnectors) {
				if (!c.isRedundant()) {
					Node or = graph.getNode(entry.getKey());
					if (or == null) {
						or = new Node(entry.getKey());
						graph.addNode(or);
					}
					Node dst = graph.getNode(Shape.getKey(c.to.getLinkedClassExpression()));
					if (dst == null) {
						dst = new Node(Shape.getKey(c.to.getLinkedClassExpression()));
						graph.addNode(dst);
					}
					dst.setIdDad(or.getId());
					graph.addEdge(new Edge(or, dst));
				}
			}
		}
	}


	private static boolean isPartOfSuperNode(Shape shape, VisGraph vgraph) {
		for (Entry<String, Shape> entry : vgraph.shapeMap.entrySet()) {
			if (entry.getValue() instanceof SuperNode superNode) {
				if (superNode.getSubNodes().contains(shape)) {
					return true;
				}
			}
		}
		return false;
	}

	private void repositionParents() {
		for (Entry<String, Shape> entry : vgraph.shapeMap.entrySet()) {
			Shape shape = entry.getValue();
			if (shape.isVisible() && shape instanceof VisClass) {
				VisClass visClass = (VisClass) shape;
				if (!visClass.children.isEmpty()) {
					int sumY = 0;
					int count = 0;
					for (Shape child : visClass.children) {
						if (child.isVisible()) {
							sumY += child.getPosY();
							count++;
						}
					}
					if (count > 0) {
						int averageY = sumY / count;
						visClass.setPosY(averageY);
					}
				}
			}
		}
	}
}
