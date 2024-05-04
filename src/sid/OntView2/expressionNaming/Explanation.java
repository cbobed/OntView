package sid.OntView2.expressionNaming;

import org.semanticweb.owlapi.model.OWLClassExpression;
import uk.ac.manchester.cs.owlapi.dlsyntax.DLSyntaxObjectRenderer;



public class Explanation {
	
	public static DLSyntaxObjectRenderer renderer = new DLSyntaxObjectRenderer(); 
	
	OWLClassExpression ce1; 
	OWLClassExpression ce2; 
	
	public Explanation (OWLClassExpression ce1, OWLClassExpression ce2) {
		this.ce1 = ce1; 
		this.ce2 = ce2; 
	}
	
	public String toString (){
		return renderer.render(ce1) + " \n EquivalentTo \n "+renderer.render(ce2); 
	}
	
}
