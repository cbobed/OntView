package sid.OntView2.utils;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;


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
	}

	public static void printExplanation(OWLOntology ontology, OWLReasonerFactory factory,
										OWLDataFactory dataFactory, OWLReasoner reasoner){

		System.out.println("Computing explanations for the inconsistency...");
		for (OWLClass cls : reasoner.getUnsatisfiableClasses()) {
			System.out.println(cls);
		}
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
