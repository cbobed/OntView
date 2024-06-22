package sid.OntView2.common;

import java.util.Observable;
import java.util.Observer;

/**
 * Thread that watches over graph state
 * 
 * @author bob
 *
 */
public class VisGraphObserver extends Thread implements Observer {
	
	// check every MILLIS
	public static final int MILLIS_DEFAULT = 30000;
	private int milliseconds;
	boolean cancel = false;
	VisGraph graph;
	public VisGraphObserver(VisGraph g, int pmilliseconds){
		graph = g;
		milliseconds = pmilliseconds;
	}

	public VisGraphObserver(VisGraph g){
		this(g, MILLIS_DEFAULT);
	}
	
	public void run(){
		while (!cancel){
		   try {
			this.wait(milliseconds);
		   } catch (InterruptedException e) {
			   	e.printStackTrace();
		   }
		   if (!cancel) {
		      update();
		   }   
		   else {
			   cancel = false;
		   }   
		}
	}

	public synchronized void update() {
		VisLevel.adjustWidthAndPos(graph.getLevelSet());
	}

	@Override
	public void update(Observable o, Object arg) {
		this.update();
		cancel = true;	
	}

}
