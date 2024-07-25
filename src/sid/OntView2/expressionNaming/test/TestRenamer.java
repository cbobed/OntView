package sid.OntView2.expressionNaming.test;

import java.io.File;
import java.io.IOException;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import openllet.owlapi.OpenlletReasonerFactory;
import sid.OntView2.expressionNaming.Explanation;
import sid.OntView2.expressionNaming.SIDClassExpressionNamer;


public class TestRenamer {

	public static OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	public static OWLDataFactory dataFactory = OWLManager.getOWLDataFactory();
	public static OWLOntology onto = null;
	
	public static OWLReasoner reasoner = null; 
	
	public static void main(String[] args) throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {

		String ontoFile = "d:\\eclipseWorkspaces\\workspaceHelios\\OntView-Applet\\tmp\\p4.owl";
		boolean direct = true;
		
		OWLOntology onto = load(ontoFile, direct, manager);
		
		reasoner = (new OpenlletReasonerFactory()).createReasoner(onto); 
		
		
		SIDClassExpressionNamer renamer = new SIDClassExpressionNamer(onto, reasoner); 
		
		long start = System.nanoTime();
		renamer.applyNaming(true); 
		System.out.println("Took " + (System.nanoTime() - start) / 1000000 + "ms");
		save(onto, ontoFile.replace("/", "/reduced/").replace(".","_red."), manager);
		
		System.out.println("Added "+renamer.getClassesToAdd().size()+" new concepts");
		System.out.println("in the form of "+renamer.getAxiomsToAdd().size()+" new axioms"); 
		System.out.println("The reasoner filtered "+renamer.getClassesFiltered().size()+" classes to be added"); 
		
		for (Explanation exp: renamer.getClassesFiltered()) {
			System.out.println(exp); 
		}
	}
	
	
	
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
	
	static public void save(OWLOntology onto, String destFile, OWLOntologyManager manager) 
	throws OWLOntologyStorageException, OWLOntologyCreationException, IOException {

		//File file = File.createTempFile("owlapiexamples", "saving");
		File file = new File(destFile);
		manager.saveOntology(onto, IRI.create(file.toURI()));
		// By default ontologies are saved in the format from which they were
		// loaded. In this case the ontology was loaded from an rdf/xml file We
		// can get information about the format of an ontology from its manager

//		OWLDocumentFormat format = manager.getOntologyFormat(onto);
//		// We can save the ontology in a different format Lets save the ontology
//		// in owl/xml format
//		OWLXMLDocumentFormat owlxmlFormat = new OWLXMLDocumentFormat();
//		// Some ontology formats support prefix names and prefix IRIs. In our
//		// case we loaded the pizza ontology from an rdf/xml format, which
//		// supports prefixes. When we save the ontology in the new format we
//		// will copy the prefixes over so that we have nicely abbreviated IRIs
//		// in the new ontology document
//		if (format.isPrefixOWLOntologyFormat()) {
//			owlxmlFormat.copyPrefixesFrom(format.asPrefixOWLOntologyFormat());
//		}
//		manager.saveOntology(onto, owlxmlFormat, IRI.create(file.toURI()));
	}
	
	
}
