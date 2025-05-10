package sid.OntView2.expressionNaming;

import org.semanticweb.owlapi.model.*;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class OWLClassExpressionHarvester implements OWLAxiomVisitor {

	ArrayList<OWLClassExpression> harvestedClasses;
	ArrayList<OWLClassExpression> domainClasses;
	ArrayList<OWLClassExpression> rangeClasses;
	
	public OWLClassExpressionHarvester () {
		this.harvestedClasses = new ArrayList<>();
		this.domainClasses = new ArrayList<>(); 
		this.rangeClasses = new ArrayList<>(); 
	}
	
	public ArrayList<OWLClassExpression> getHarvestedClasses() {
		return harvestedClasses;
	}

    //// VISITOR PATTERN AXIOMS
	
	public void visit(@Nonnull OWLAnnotationAssertionAxiom axiom) {}

	@Override
	public void visit(@Nonnull OWLSubAnnotationPropertyOfAxiom axiom) {}

	@Override
	public void visit(@Nonnull OWLAnnotationPropertyDomainAxiom axiom) {}

	@Override
	public void visit(@Nonnull OWLAnnotationPropertyRangeAxiom axiom) {}
	
	@Override
	public void visit(@Nonnull OWLDifferentIndividualsAxiom axiom) {}

	@Override
	public void visit(@Nonnull OWLObjectPropertyAssertionAxiom axiom) {}

	@Override
	public void visit(@Nonnull SWRLRule rule) {}
	
	@Override
	public void visit(@Nonnull OWLSameIndividualAxiom axiom) {}
	
	@Override
	public void visit(@Nonnull OWLSubPropertyChainOfAxiom axiom) {}
	
	@Override
	public void visit(@Nonnull OWLDeclarationAxiom axiom) {
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
	public void visit(@Nonnull OWLNegativeObjectPropertyAssertionAxiom axiom) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(@Nonnull OWLAsymmetricObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(@Nonnull OWLReflexiveObjectPropertyAxiom axiom) {
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
        Set<OWLClassExpression> operands = axiom.getClassExpressions();
		for (OWLClassExpression ce: operands) {
			if (ce.isAnonymous()) {
				harvestedClasses.add(ce); 
			}
		}
	}
	
	@Override
	public void visit(OWLEquivalentClassesAxiom axiom) {
		Set<OWLClassExpression> operands = axiom.getClassExpressions(); 
		
		Iterator<OWLClassExpression> it = operands.iterator(); 
		ArrayList<OWLClassExpression> auxArray = new ArrayList<>();
		OWLClassExpression auxCE;
		while (it.hasNext()) {
			auxCE = it.next(); 
			if (auxCE.isAnonymous()) {
				auxArray.add(auxCE); 
			}
		}
        harvestedClasses.addAll(auxArray);
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
	public void visit(@Nonnull OWLEquivalentObjectPropertiesAxiom axiom) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(@Nonnull OWLNegativeDataPropertyAssertionAxiom axiom) {
		// TODO Auto-generated method stub
	}


	@Override
	public void visit(@Nonnull OWLDisjointDataPropertiesAxiom axiom) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(@Nonnull OWLDisjointObjectPropertiesAxiom axiom) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(OWLObjectPropertyRangeAxiom axiom) {
        OWLClassExpression range = axiom.getRange();
		
		if (range.isAnonymous()) {
			harvestedClasses.add(range);
			rangeClasses.add(range); 
		}
	}

	@Override
	public void visit(@Nonnull OWLFunctionalObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(@Nonnull OWLSubObjectPropertyOfAxiom axiom) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(@Nonnull OWLSymmetricObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(@Nonnull OWLDataPropertyRangeAxiom axiom) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(@Nonnull OWLFunctionalDataPropertyAxiom axiom) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(@Nonnull OWLEquivalentDataPropertiesAxiom axiom) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(@Nonnull OWLClassAssertionAxiom axiom) {}

	@Override
	public void visit(@Nonnull OWLDataPropertyAssertionAxiom axiom) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(@Nonnull OWLTransitiveObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(@Nonnull OWLIrreflexiveObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(@Nonnull OWLSubDataPropertyOfAxiom axiom) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(@Nonnull OWLInverseFunctionalObjectPropertyAxiom axiom) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(@Nonnull OWLInverseObjectPropertiesAxiom axiom) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(@Nonnull OWLHasKeyAxiom axiom) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(@Nonnull OWLDatatypeDefinitionAxiom axiom) {}

}
