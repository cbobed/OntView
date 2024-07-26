package sid.OntView2.common;

/**
 * Thread that watches over graph state
 * 
 * @author bob
 *
 */
public class VisGraphObserver {
	VisGraph graph;

	public VisGraphObserver(VisGraph g){
		graph = g;
		graph.addGeneralObserver((observable, oldValue, newValue) -> update());
	}

	public synchronized void update() {
		VisLevel.adjustWidthAndPos(graph.getLevelSet());
	}

}
