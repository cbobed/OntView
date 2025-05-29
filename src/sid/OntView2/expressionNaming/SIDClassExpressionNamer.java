package sid.OntView2.expressionNaming;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import sid.OntView2.utils.ImageMerger;

public class SIDClassExpressionNamer {
    private static final Logger logger = LogManager.getLogger(SIDClassExpressionNamer.class);
    ArrayList<OWLClassExpression> classesToAdd;
	ArrayList<Explanation> classesFiltered;
	Set<OWLEquivalentClassesAxiom> axiomsToAdd;

	OWLOntology ontology;
	OWLReasoner reasoner;

	private int classID = 1;
	public static String className = "SIDClass_";

	public SIDClassExpressionNamer (OWLOntology ont, OWLReasoner reasoner) {
		this.ontology = ont;
		this.reasoner = reasoner;
		this.classesToAdd = null;
		this.classesFiltered = new ArrayList<>();
		this.axiomsToAdd = new HashSet<> ();
	}
	
	public void applyNaming (boolean refreshReasoner) {

		// First: obtain all the classExpressions that
		// are anonymous (TOP LEVEL)
		// Adapted from the code of Alessandro

		try {
			retrieveClassExpressions(); 
			applySyntacticSieve();
		}
		catch (NonGatheredClassExpressionsException e) {
            logger.error("Not possible -- we have just called retrieveClassExpressions()");
		}

		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		OWLEquivalentClassesAxiom equivalentAxiom;

		// we have the minimum set of class expressions that have to be named
		// it should be enough to assert the new class as part of the equivalence
		// to be considered
		OWLClass auxiliaryClass;

		axiomsToAdd.clear();
		String baseIRI = ontology.getOntologyID().getOntologyIRI().get().toString();

		if (baseIRI.endsWith(".owl")) {
			baseIRI += "#";
		}
		else {
			baseIRI +="/";
		}

		for (OWLClassExpression ce: classesToAdd) {
			auxiliaryClass = dataFactory.getOWLClass(IRI.create(baseIRI+className+classID));
			classID++;
			equivalentAxiom = dataFactory.getOWLEquivalentClassesAxiom(auxiliaryClass, ce);
			axiomsToAdd.add(equivalentAxiom);
		}

		manager.addAxioms(ontology, axiomsToAdd);

		// we refresh the reasoner with the set of newly added classes
		if (refreshReasoner) {
			reasoner.flush();
		}
	}
	
	public void gatherAllExpressionsFiltering() throws NonGatheredClassExpressionsException {
		retrieveClassExpressions();
		applySyntacticSieve();
	}
	
	private void retrieveClassExpressions () {
		OWLClassExpressionHarvester axiomVisitor = new OWLClassExpressionHarvester();
		// we apply the reduction in this module to all the
		// import closure
		for (OWLAxiom axiom: ontology.getTBoxAxioms(Imports.INCLUDED)) {
			axiom.accept(axiomVisitor);
		}
		classesToAdd = axiomVisitor.getHarvestedClasses();		
	}
	
	private void applySyntacticSieve() throws NonGatheredClassExpressionsException {
		
		if (this.classesToAdd == null) throw new NonGatheredClassExpressionsException();
        logger.debug("--> {} class expressions harvested", classesToAdd.size());
		// CBL: due to the large amount of tests in huge ontologies
		// we first perform a syntactic sieve

		Hashtable<Integer, OWLClassExpression> syntacticalSieve = new Hashtable<>();
		Integer auxInt;
		for (OWLClassExpression auxClass: classesToAdd) {
			auxInt = auxClass.toString().hashCode();
			if (!syntacticalSieve.containsKey(auxInt))
				syntacticalSieve.put(auxInt, auxClass);
		}
        logger.debug("--> {} class expressions after syntactic sieve", syntacticalSieve.size());
		classesToAdd.clear();
		classesToAdd.addAll(syntacticalSieve.values());
	}

	public ArrayList<OWLClassExpression> getClassesToAdd() {
		return classesToAdd;
	}
	
	public void nullifyClassesToAdd() {
		classesToAdd = null; 
	}

	public ArrayList<Explanation> getClassesFiltered() {
		return classesFiltered;
	}

	public Set<OWLEquivalentClassesAxiom> getAxiomsToAdd() {
		return axiomsToAdd;
	}
}
