package sid.OntView.utils;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;
import uk.ac.manchester.cs.owl.owlapi.OWLDeclarationAxiomImpl;


public class Util {
	
	static final String freshClassPrefix = "#Class_";
	static int nextFreshId = 0;
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
	private static Calendar cal = Calendar.getInstance();
	
	static public OWLOntology load(String iriString, boolean local, OWLOntologyManager manager) 
			throws OWLOntologyCreationException {
		OWLOntology onto = null;

		if(!local){
			IRI iri = IRI.create(iriString);
			onto = manager.loadOntologyFromOntologyDocument(iri);
		}
		else {
			File file = new File(iriString);
			onto = manager.loadOntologyFromOntologyDocument(file);  
		}

		return onto;
	}
	
	public static String getCurrTime(){
		return dateFormat.format(cal.getTime());
	}

	static public void save(OWLOntology onto, String destFile, OWLOntologyManager manager) 
			throws OWLOntologyStorageException, OWLOntologyCreationException, IOException {

		//File file = File.createTempFile("owlapiexamples", "saving");
		File file = new File(destFile);
		manager.saveOntology(onto, IRI.create(file.toURI()));
		// By default ontologies are saved in the format from which they were
		// loaded. In this case the ontology was loaded from an rdf/xml file We
		// can get information about the format of an ontology from its manager

		OWLDocumentFormat format = manager.getOntologyFormat(onto);
		// We can save the ontology in a different format Lets save the ontology
		// in owl/xml format
		OWLXMLDocumentFormat owlxmlFormat = new OWLXMLDocumentFormat();
		// Some ontology formats support prefix names and prefix IRIs. In our
		// case we loaded the pizza ontology from an rdf/xml format, which
		// supports prefixes. When we save the ontology in the new format we
		// will copy the prefixes over so that we have nicely abbreviated IRIs
		// in the new ontology document
		if (format.isPrefixOWLOntologyFormat()) {
			owlxmlFormat.copyPrefixesFrom(format.asPrefixOWLOntologyFormat());
		}
		manager.saveOntology(onto, owlxmlFormat, IRI.create(file.toURI()));
	}
	
	public static void printExplanation(OWLOntology ontology, OWLReasonerFactory factory, 
			OWLDataFactory dataFactory, OWLReasoner reasoner){

	    System.out.println("Computing explanations for the inconsistency...");
	    for (OWLClass cls : reasoner.getUnsatisfiableClasses()) {
	    	System.out.println(cls);
	    }
	    /*
		    BlackBoxExplanation exp=new BlackBoxExplanation(ontology, factory, reasoner);
		    HSTExplanationGenerator multExplanator=new HSTExplanationGenerator(exp);
		    // Now we can get explanations for the inconsistency 
		    Set<Set<OWLAxiom>> explanations = multExplanator.getExplanations(cls);
		    // Let us print them. Each explanation is one possible set of axioms that cause the 
		    // unsatisfiability. 
		    for (Set<OWLAxiom> explanation : explanations) {
		        System.out.println("------------------");
		        System.out.println("Axioms causing the inconsistency: ");
		        for (OWLAxiom causingAxiom : explanation) {
		            System.out.println(causingAxiom);
		        }
		        System.out.println("------------------");
		    }
		}*/
    }


	
	static public OWLClass createFreshClass(OWLOntology onto, OWLDataFactory dataFactory, OWLOntologyManager manager){
		OWLClass cls = dataFactory.getOWLClass(
					freshClassPrefix + nextFreshId, 
					new DefaultPrefixManager(onto.getOntologyID().getOntologyIRI().get().toString())
				);
		
		OWLDeclarationAxiom declAxiom = dataFactory.getOWLDeclarationAxiom(cls, new HashSet<OWLAnnotation>()); 
		manager.addAxiom(onto, declAxiom); 
		
		++nextFreshId;
				
		return cls;
	} 
}
