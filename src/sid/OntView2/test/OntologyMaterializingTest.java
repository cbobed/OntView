package sid.OntView2.test;

import java.util.Set;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.ConsoleProgressMonitor;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;

public class OntologyMaterializingTest {

	public static void main (String args[]) {
			
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology activeOntology = null; 
		try {
			activeOntology = manager.loadOntologyFromOntologyDocument(IRI.create(args[0]));
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			activeOntology = null;
			manager = null;
		}
		
			ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
			OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor);
			OWLReasonerFactory reasonerFactory = new ReasonerFactory(); 
			try {
				// Create a reasoner that will reason over our ontology and its imports closure.  Pass in the configuration.
				OWLReasoner reasoner = reasonerFactory.createReasoner(activeOntology, config);
				
				Set<InferenceType> precomputableInferences = reasoner.getPrecomputableInferenceTypes(); 
				reasoner.precomputeInferences(precomputableInferences.toArray(new InferenceType[precomputableInferences.size()]));
				
			} catch (Exception e) {
				e.printStackTrace();
				
			}

	}
	
}
