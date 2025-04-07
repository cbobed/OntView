package sid.OntView2.expressionNaming;

import org.semanticweb.owlapi.model.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class OWLClassExpressionHarvester implements OWLAxiomVisitor {

	ArrayList<OWLClassExpression> harvestedClasses = null;
	ArrayList<OWLClassExpression> domainClasses = null; 
	ArrayList<OWLClassExpression> rangeClasses = null; 
	
	public OWLClassExpressionHarvester () {
		this.harvestedClasses = new ArrayList<OWLClassExpression>(); 
		this.domainClasses = new ArrayList<>(); 
		this.rangeClasses = new ArrayList<>(); 
	}
	
	public ArrayList<OWLClassExpression> getHarvestedClasses() {
		return harvestedClasses;
	}
	
	public ArrayList<OWLClassExpression> getDomainClasses() {
		return domainClasses;
	}

	public ArrayList<OWLClassExpression> getRangeClasses() {
		return rangeClasses;
	}
	
	//// VISITOR PATTERN AXIOMS 
	
	public void visit(OWLAnnotationAssertionAxiom axiom) {
		return;
	}

	@Override
	public void visit(OWLSubAnnotationPropertyOfAxiom axiom) {
		return;
	}

	@Override
	public void visit(OWLAnnotationPropertyDomainAxiom axiom) {
		return;
	}

	@Override
	public void visit(OWLAnnotationPropertyRangeAxiom axiom) {
		return;
	}
	
	@Override
	public void visit(OWLDifferentIndividualsAxiom axiom) {
		return;
	}

	@Override
	public void visit(OWLObjectPropertyAssertionAxiom axiom) {
		return;
	}

	@Override
	public void visit(SWRLRule rule) {
		return;
	}
	
	@Override
	public void visit(OWLSameIndividualAxiom axiom) {
		return;
	}
	
	@Override
	public void visit(OWLSubPropertyChainOfAxiom axiom) {
		return;
	}
	
	@Override
	public void visit(OWLDeclarationAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLSubClassOfAxiom axiom) {
	
		if (axiom.getSubClass().isAnonymous()) {
			harvestedClasses.add(axiom.getSubClass()); 
		}
		
		if (axiom.getSuperClass().isAnonymous()) {
			harvestedClasses.add(axiom.getSuperClass()); 
		}

	}

	@Override
	public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(OWLAsymmetricObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLReflexiveObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLDisjointClassesAxiom axiom) {
		
		List<OWLClassExpression> operands = axiom.getClassExpressionsAsList();
		
		for (OWLClassExpression ce: operands) {
			if (ce.isAnonymous()) {
				harvestedClasses.add(ce);
			}
		}
	}
	

	@Override
	public void visit(OWLDisjointUnionAxiom axiom) {
		OWLAxiom newAxiom = null;
		Set<OWLClassExpression> operands = axiom.getClassExpressions();
		for (OWLClassExpression ce: operands) {
			if (ce.isAnonymous()) {
				harvestedClasses.add(ce); 
			}
		}
	}
	
	@Override
	public void visit(OWLEquivalentClassesAxiom axiom) {
		
		// this is a special case: 
		// the defined concepts are treated using the reasoner 
		// not in this process
		// 
		// here, we only consider C1 == .. == Cn, with all being 
		// anonymous classExpressions
		
		Set<OWLClassExpression> operands = axiom.getClassExpressions(); 
		
		Iterator<OWLClassExpression> it = operands.iterator(); 
		boolean anyNamed = false;
		ArrayList<OWLClassExpression> auxArray = new ArrayList<OWLClassExpression>(); 
		OWLClassExpression auxCE = null; 
		while (it.hasNext() && !anyNamed) {
			auxCE = it.next(); 
			if (auxCE.isAnonymous()) {
				auxArray.add(auxCE); 
			}
			else {
				anyNamed = true; 
			}
		}
		
		if (!anyNamed) {
			// if everyone is anonymous, then we consider them
			// otherwise, they will appear as the definition of 
			// a defined Concept
			harvestedClasses.addAll(auxArray); 
		}		
	}

	@Override
	public void visit(OWLDataPropertyDomainAxiom axiom) {
		OWLClassExpression domain = axiom.getDomain();
		if (domain.isAnonymous()) {
			harvestedClasses.add(domain); 
			domainClasses.add(domain); 
		}
	}

	@Override
	public void visit(OWLObjectPropertyDomainAxiom axiom) {
	
		OWLClassExpression domain = axiom.getDomain();
		
		if (domain.isAnonymous()) {
			harvestedClasses.add(domain);
			domainClasses.add(domain); 
		}
	}

	@Override
	public void visit(OWLEquivalentObjectPropertiesAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
		// TODO Auto-generated method stub

	}


	@Override
	public void visit(OWLDisjointDataPropertiesAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLDisjointObjectPropertiesAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLObjectPropertyRangeAxiom axiom) {
		OWLAxiom newAxiom = null;
		
		OWLClassExpression range = axiom.getRange();
		
		if (range.isAnonymous()) {
			harvestedClasses.add(range);
			rangeClasses.add(range); 
		}
	}

	@Override
	public void visit(OWLFunctionalObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLSubObjectPropertyOfAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLSymmetricObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLDataPropertyRangeAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLFunctionalDataPropertyAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLEquivalentDataPropertiesAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLClassAssertionAxiom axiom) {
		return;
	}

	@Override
	public void visit(OWLDataPropertyAssertionAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLTransitiveObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLSubDataPropertyOfAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLInverseObjectPropertiesAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLHasKeyAxiom axiom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visit(OWLDatatypeDefinitionAxiom axiom) {
		return;
	}

}
