package sid.OntView2.expressionNaming;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

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

import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxObjectRenderer;

public class SIDClassExpressionNamer {

	static DLSyntaxObjectRenderer renderer = new DLSyntaxObjectRenderer();


	ArrayList<OWLClassExpression> classesToAdd = null;
	ArrayList<Explanation> classesFiltered = null;
	Set<OWLEquivalentClassesAxiom> axiomsToAdd = null ;

	OWLOntology ontology = null;
	OWLReasoner reasoner = null;

	private int classID = 1;
	public static String className = "SIDClass_";

	public SIDClassExpressionNamer (OWLOntology ont, OWLReasoner reasoner) {
		this.ontology = ont;
		this.reasoner = reasoner;
		this.classesToAdd = null;
		this.classesFiltered = new ArrayList<Explanation>();
		this.axiomsToAdd = new HashSet<OWLEquivalentClassesAxiom> ();
	}
	
	public void applyNaming (boolean refreshReasoner) {

		// First: obtain all the classExpressions that
		// are anonymous (TOP LEVEL)
		// Adapted from the code of Alessandro

		
		try {
			retrieveClassExpressions(); 
			applySyntactiSieve(); 
		}
		catch (NonGatheredClassExpressionsException e) {
			System.err.println("Not possible -- we have just called retrieveClassExpressions()"); 
		}

		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		OWLEquivalentClassesAxiom equivalentAxiom = null;

		// we have the minimum set of class expressions that have to be named
		// it should be enough to assert the new class as part of the equivalence
		// to be considered
		OWLClass auxiliarClass = null;

		axiomsToAdd.clear();
		String baseIRI = ontology.getOntologyID().getOntologyIRI().get().toString();

		if (baseIRI.endsWith(".owl")) {
			baseIRI += "#";
		}
		else {
			baseIRI +="/";
		}

		for (OWLClassExpression ce: classesToAdd) {
			auxiliarClass = dataFactory.getOWLClass(IRI.create(baseIRI+className+classID));
			classID++;
			equivalentAxiom = dataFactory.getOWLEquivalentClassesAxiom(auxiliarClass, ce);
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
		applySyntactiSieve();
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
	
	private void applySyntactiSieve() throws NonGatheredClassExpressionsException {
		
		if (this.classesToAdd == null) throw new NonGatheredClassExpressionsException(); 
		System.out.println("--> "+classesToAdd.size() + " class expressions harvested");
		// CBL: due to the large amount of tests in huge ontologies
		// we first perform a syntactic sieve

		Hashtable<Integer, OWLClassExpression> syntacticalSieve = new Hashtable<Integer, OWLClassExpression>();
		Integer auxInt;
		for (OWLClassExpression auxClass: classesToAdd) {
			auxInt = auxClass.toString().hashCode();
			if (!syntacticalSieve.containsKey(auxInt))
				syntacticalSieve.put(auxInt, auxClass);
		}
		System.out.println("--> "+syntacticalSieve.size()+" class expressions after syntactic sieve");

		classesToAdd.clear();
		classesToAdd.addAll(syntacticalSieve.values()) ;
		
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
