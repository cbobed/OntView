package sid.OntView2.expressionNaming;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import uk.ac.manchester.cs.owlapi.dlsyntax.DLSyntaxObjectRenderer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

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
		
		OWLClassExpressionHarvester axiomVisitor = new OWLClassExpressionHarvester();
		Hashtable<Integer, OWLClassExpression> syntacticalSieve = new Hashtable<Integer, OWLClassExpression>(); 
		
		// we apply the reduction in this module to all the 
		// import closure
		for (OWLAxiom axiom: ontology.getTBoxAxioms(true)) {
			 axiom.accept(axiomVisitor); 
		}
		classesToAdd = axiomVisitor.getHarvestedClasses(); 
		System.out.println("--> "+classesToAdd.size() + " class expressions harvested"); 
		// CBL: due to the large amount of tests in huge ontologies
		// we first perform a syntactic sieve 
		Integer auxInt; 
		for (OWLClassExpression auxClass: classesToAdd) {
			auxInt = auxClass.toString().hashCode(); 
			if (!syntacticalSieve.containsKey(auxInt))
				syntacticalSieve.put(auxInt, auxClass); 
		}
		
		System.out.println("--> "+syntacticalSieve.size()+" class expressions after syntactic sieve"); 
		
		// we now filter the classes to add with the help of the 
		// reasoner before asserting them with the new names 
		
		OWLOntologyManager manager = ontology.getOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory(); 
		OWLEquivalentClassesAxiom equivalentAxiom = null; 
		
		classesFiltered.clear(); 
		
//		for (int i=0; i<classesToAdd.size(); i++) {
//			// System.out.println("comparing "+renderer.render(classesToAdd.get(i))); 
//			for (int j=classesToAdd.size()-1; j>i; j--) {
//				// we go backwards because we remove elements on the fly
//				// System.out.println("  to "+renderer.render(classesToAdd.get(j))); 
//				equivalentAxiom = dataFactory.getOWLEquivalentClassesAxiom(classesToAdd.get(i), classesToAdd.get(j)); 
//				if (reasoner.isEntailed(equivalentAxiom)){
//					// System.out.println("    which is removed due to equivalence"); 
//					classesFiltered.add(new Explanation (classesToAdd.get(i), classesToAdd.get(j))); 
//					classesToAdd.remove(j); 
//				}
//			}			
//		}
		
// 		System.out.println("--> "+classesToAdd.size()+" class expressions after semantic sieve"); 
		
		classesToAdd.clear(); 
		classesToAdd.addAll(syntacticalSieve.values()) ;
		
		// we have the minimum set of class expressions that have to be named
		// it should be enough to assert the new class as part of the equivalence 
		// to be considered
		OWLClass auxiliarClass = null; 
		
		axiomsToAdd.clear();  
		String baseIRI = ontology.getOntologyID().getOntologyIRI().toString(); 
		
		if (baseIRI.endsWith(".owl")) {
			baseIRI += "#"; 
		}
		else {
			baseIRI +="/"; 
		}
		
		for (OWLClassExpression ce: classesToAdd) {
			auxiliarClass = dataFactory.getOWLClass(IRI.create(baseIRI+className+classID)); 
			// System.out.println(auxiliarClass.toString()); 
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

	public ArrayList<OWLClassExpression> getClassesToAdd() {
		return classesToAdd;
	}

	public ArrayList<Explanation> getClassesFiltered() {
		return classesFiltered;
	}

	public Set<OWLEquivalentClassesAxiom> getAxiomsToAdd() {
		return axiomsToAdd;
	}
	
}
