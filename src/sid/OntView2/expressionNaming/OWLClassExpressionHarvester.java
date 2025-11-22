package sid.OntView2.expressionNaming;

import org.semanticweb.owlapi.model.*;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class OWLClassExpressionHarvester implements OWLAxiomVisitor {

	ArrayList<OWLClassExpression> harvestedClasses;
	Hashtable<OWLPropertyExpression, OWLClassExpression> domainClasses;
	Hashtable<OWLPropertyExpression, OWLClassExpression> rangeClasses;
	
	Hashtable<OWLDataProperty, Set<OWLClassExpression>> domainIntersectionsDataProp;
	Hashtable<OWLObjectProperty, Set<OWLClassExpression>> domainIntersectionsObjProp;
	Hashtable<OWLObjectProperty, Set<OWLClassExpression>> rangeIntersectionsObjProp;
	
	
	public OWLClassExpressionHarvester () {
		this.harvestedClasses = new ArrayList<>();
		this.domainClasses = new Hashtable<>(); 
		this.rangeClasses = new Hashtable<>(); 
		
		this.domainIntersectionsDataProp = new Hashtable<>(); 
		this.domainIntersectionsObjProp = new Hashtable<>(); 
		this.rangeIntersectionsObjProp = new Hashtable<>();
	}
	
	public Hashtable<OWLPropertyExpression, OWLClassExpression> getDomainClasses() {
		return domainClasses; 
	}
	
	public Hashtable<OWLPropertyExpression, OWLClassExpression> getRangeClasses() {
		return rangeClasses; 
	}
	
	public ArrayList<OWLClassExpression> getHarvestedClasses(OWLDataFactory dataFactory) {
		System.out.println("--> Harvested without consolidating: "+harvestedClasses.size()); 
		int consolidated = 0; 
		if (domainClasses.isEmpty() || rangeClasses.isEmpty()) {
			// we consolidate the domains and the ranges and 
			// add them to the harvestedClasses list 
			for (Entry<OWLDataProperty, Set<OWLClassExpression>> ent: domainIntersectionsDataProp.entrySet()) {
				if (ent.getValue().size()>1) {
					domainClasses.put(ent.getKey(), dataFactory.getOWLObjectIntersectionOf(ent.getValue())); 
					consolidated++; 
				}
				else if (ent.getValue().size() == 1) {
					domainClasses.put(ent.getKey(), ent.getValue().iterator().next()); 
				}
			}
			
			for (Entry<OWLObjectProperty, Set<OWLClassExpression>> ent: domainIntersectionsObjProp.entrySet()) {
				if (ent.getValue().size()>1) {
					domainClasses.put(ent.getKey(), dataFactory.getOWLObjectIntersectionOf(ent.getValue())); 
					consolidated++; 
				}
				else if (ent.getValue().size() == 1) {
					domainClasses.put(ent.getKey(), ent.getValue().iterator().next()); 
				}
			}
			for (Entry<OWLObjectProperty, Set<OWLClassExpression>> ent: rangeIntersectionsObjProp.entrySet()) {
				if (ent.getValue().size()>1) {
					rangeClasses.put(ent.getKey(), dataFactory.getOWLObjectIntersectionOf(ent.getValue())); 
					consolidated++; 
				}
				else if (ent.getValue().size() == 1) {
					rangeClasses.put(ent.getKey(), ent.getValue().iterator().next()); 
				}
			}
			
			harvestedClasses.addAll(domainClasses.values()); 
			harvestedClasses.addAll(rangeClasses.values()); 
		}
		System.out.println("--> Harvested after consolidating: "+harvestedClasses.size() + " of which: "+consolidated+" are AND-built"); 
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
		if (!domainIntersectionsDataProp.containsKey(axiom.getProperty())) {
			domainIntersectionsDataProp.put(axiom.getProperty().asOWLDataProperty(), new HashSet<>()); 
		}	
		domainIntersectionsDataProp.get(axiom.getProperty()).add(domain); 
	}

	@Override
	public void visit(OWLObjectPropertyDomainAxiom axiom) {
	
		OWLClassExpression domain = axiom.getDomain();
		OWLObjectProperty objProp = null; 
		
		if (axiom.getProperty() instanceof OWLObjectInverseOf) {
			objProp = ((OWLObjectInverseOf)axiom.getProperty()).getNamedProperty(); 
			if (!rangeIntersectionsObjProp.containsKey(objProp)) {
				rangeIntersectionsObjProp.put(objProp, new HashSet<>()); 
			}	
			rangeIntersectionsObjProp.get(objProp).add(domain);
		}
		else {
			if (!domainIntersectionsObjProp.containsKey(axiom.getProperty())) {
				domainIntersectionsObjProp.put(axiom.getProperty().asOWLObjectProperty(), new HashSet<>()); 
			}	
			domainIntersectionsObjProp.get(axiom.getProperty()).add(domain);
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
		OWLObjectProperty objProp = null; 
		
		if (axiom.getProperty() instanceof OWLObjectInverseOf) {
			objProp = ((OWLObjectInverseOf)axiom.getProperty()).getNamedProperty(); 
			if (!domainIntersectionsObjProp.containsKey(objProp)) {
				domainIntersectionsObjProp.put(objProp, new HashSet<>()); 
			}	
			domainIntersectionsObjProp.get(objProp).add(range);
		}
		else {
			if (!rangeIntersectionsObjProp.containsKey(axiom.getProperty())) {
				rangeIntersectionsObjProp.put(axiom.getProperty().asOWLObjectProperty(), new HashSet<>()); 
			}	
			rangeIntersectionsObjProp.get(axiom.getProperty()).add(range);
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
