package sid.OntView2.utils;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.OWLOntologyWalker;
import org.semanticweb.owlapi.util.OWLOntologyWalkerVisitor;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;


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

		OWLOntologyFormat format = manager.getOntologyFormat(onto);
		// We can save the ontology in a different format Lets save the ontology
		// in owl/xml format
		OWLXMLOntologyFormat owlxmlFormat = new OWLXMLOntologyFormat();
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

	public void shouldWalkOntology(OWLOntology onto, OWLOntologyManager manager) throws OWLOntologyCreationException {
		// This example shows how to use an ontology walker to walk the asserted
		// structure of an ontology. Suppose we want to find the axioms that use
		// a some values from (existential restriction) we can use the walker to
		// do this. We'll use the pizza ontology as an example. Load the
		// ontology from the web:
		IRI ontoIRI = manager.getOntologyDocumentIRI(onto);

		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology ont = man.loadOntologyFromOntologyDocument(ontoIRI);
		// Create the walker. Pass in the pizza ontology - we need to put it
		// into a set though, so we just create a singleton set in this case.
		OWLOntologyWalker walker = new OWLOntologyWalker(Collections.singleton(ont));
		// Now ask our walker to walk over the ontology. We specify a visitor
		// who gets visited by the various objects as the walker encounters
		// them. We need to create out visitor. This can be any ordinary
		// visitor, but we will extend the OWLOntologyWalkerVisitor because it
		// provides a convenience method to get the current axiom being visited
		// as we go. Create an instance and override the
		// visit(OWLObjectSomeValuesFrom) method, because we are interested in
		// some values from restrictions.
		OWLOntologyWalkerVisitor<Object> visitor = new OWLOntologyWalkerVisitor<Object>(
				walker) {
			@Override
			public Object visit(OWLObjectSomeValuesFrom desc) {
				// Print out the restriction
				System.out.println(desc);
				// Print out the axiom where the restriction is used
				System.out.println("         " + getCurrentAxiom());
				System.out.println();
				// We don't need to return anything here.
				return null;
			}
		};
		// Now ask the walker to walk over the ontology structure using our
		// visitor instance.
		walker.walkStructure(visitor);
	}
	
	static public OWLClass createFreshClass(OWLOntology onto, OWLDataFactory dataFactory, OWLOntologyManager manager){
		OWLClass cls = dataFactory.getOWLClass(
					freshClassPrefix + nextFreshId, 
					new DefaultPrefixManager(onto.getOntologyID().getOntologyIRI().toString())
				);
		
		OWLDeclarationAxiom declAxiom = dataFactory.getOWLDeclarationAxiom(cls, new HashSet<OWLAnnotation>()); 
		manager.applyChange(manager.addAxiom(onto, declAxiom).get(0));
		
		++nextFreshId;
				
		return cls;
	} 
}
