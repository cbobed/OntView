package reducer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomVisitor;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.SWRLRule;

import sid.OntView2.kcExtractors.RDFRankConceptExtraction;
import sid.OntView2.utils.Params;
import sid.OntView2.utils.Util;


public class OWLAxiomReducerVisitor implements OWLAxiomVisitor {
    private static final Logger logger = LogManager.getLogger(OWLAxiomReducerVisitor.class);

	@Override
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
		OWLAxiom newAxiom = null;
		
		if(Params.verbose)
            logger.debug("{} reduction", axiom.getClass());
		
		OWLClassExpression sub = null, sup = null;
		boolean reduced = true;
		
		if(axiom.getSubClass().isClassExpressionLiteral()){
			sub = axiom.getSubClass();
		}
		else {
			reduced = false;
			if(!StructuralReducer.map.containsKey(axiom.getSubClass())){
				StructuralReducer.map.put(sub, Util.createFreshClass(
						StructuralReducer.onto, 
						StructuralReducer.dataFactory, 
						StructuralReducer.manager));
				sub = StructuralReducer.map.get(sub);
				StructuralReducer.stack.add(sub);
			}
			else
				sub = StructuralReducer.map.get(sub);
		}
		
		if(axiom.getSuperClass().isClassExpressionLiteral()){
			sup = axiom.getSuperClass();
		}
		else {
			reduced = false;
			if(!StructuralReducer.map.containsKey(axiom.getSuperClass())){
				StructuralReducer.map.put(sup, Util.createFreshClass(
						StructuralReducer.onto, 
						StructuralReducer.dataFactory, 
						StructuralReducer.manager));
				sup = StructuralReducer.map.get(sup);
				StructuralReducer.stack.add(sup);
			}
			else
				sup = StructuralReducer.map.get(sup);
		}
		
		if(!reduced){
			StructuralReducer.toDelete.add(axiom);
			StructuralReducer.toAdd.add(newAxiom = 
					StructuralReducer.dataFactory.getOWLSubClassOfAxiom(
							sub, 
							sup, 
							axiom.getAnnotations()));
			if(Params.verbose)
                logger.debug("{}\n", newAxiom);
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
		OWLAxiom newAxiom = null;
		
		if(Params.verbose)
            logger.debug("{} reduction", axiom.getClass());
		
		boolean reduced = true;
		List<OWLClassExpression> operands = axiom.getClassExpressionsAsList();
		OWLClassExpression left = operands.get(0), right = operands.get(1);
		
		if(!left.isClassExpressionLiteral()){
			reduced = false;
			if(!StructuralReducer.map.containsKey(left)){
				StructuralReducer.map.put(left, Util.createFreshClass(
						StructuralReducer.onto, 
						StructuralReducer.dataFactory, 
						StructuralReducer.manager));
				StructuralReducer.stack.add(left);
			}
			else
				left = StructuralReducer.map.get(left);
		}
		
		if(!right.isClassExpressionLiteral()){
			reduced = false;
			if(!StructuralReducer.map.containsKey(right)){
				StructuralReducer.map.put(right, Util.createFreshClass(
						StructuralReducer.onto, 
						StructuralReducer.dataFactory, 
						StructuralReducer.manager));
				StructuralReducer.stack.add(right);
			}
			else
				right = StructuralReducer.map.get(right);
		}
		
		if(!reduced){
			Set<OWLClassExpression> expr = new HashSet<OWLClassExpression>();
			expr.add(left);
			expr.add(right);
			
			StructuralReducer.toDelete.add(axiom);
			StructuralReducer.toAdd.add(newAxiom =
					StructuralReducer.dataFactory.getOWLDisjointClassesAxiom(
							expr,
							axiom.getAnnotations()));
			if(Params.verbose)
                logger.debug("{}\n", newAxiom);
		}
	}
	

	@Override
	public void visit(OWLDisjointUnionAxiom axiom) {
		OWLAxiom newAxiom = null;
		
		if(Params.verbose)
            logger.debug("{} reduction", axiom.getClass());
		
		boolean reduced = true;
		Set<OWLClassExpression> operands = axiom.getClassExpressions();
		Set<OWLClassExpression> reducedOp = new HashSet<OWLClassExpression>();
		OWLClass newClass = null;
		
		for (OWLClassExpression act : operands) {
			if(!act.isClassExpressionLiteral()){
				reduced = false;
				if(!StructuralReducer.map.containsKey(act)){
					StructuralReducer.map.put(act, newClass = Util.createFreshClass(
							StructuralReducer.onto, 
							StructuralReducer.dataFactory, 
							StructuralReducer.manager));
					StructuralReducer.stack.add(act);
					reducedOp.add(newClass);
				}
				else
					reducedOp.add(StructuralReducer.map.get(act));
			}
		}
		
		if(!reduced){			
			StructuralReducer.toDelete.add(axiom);
			StructuralReducer.toAdd.add(newAxiom =
					StructuralReducer.dataFactory.getOWLDisjointUnionAxiom(
							axiom.getOWLClass(),
							reducedOp,
							axiom.getAnnotations()));
			if(Params.verbose)
                logger.debug("{}\n", newAxiom);
		}
	}
	
	@Override
	public void visit(OWLEquivalentClassesAxiom axiom) {
		OWLAxiom newAxiom = null;
		
		if(Params.verbose)
            logger.debug("{} reduction", axiom.getClass());
		
		boolean reduced = true;
		Set<OWLClassExpression> operands = axiom.getClassExpressions();
		Set<OWLClassExpression> reducedOp = new HashSet<OWLClassExpression>();
		OWLClass newClass = null;
		
		for (OWLClassExpression act : operands) {
			if(!act.isClassExpressionLiteral()){
				reduced = false;
				if(!StructuralReducer.map.containsKey(act)){
					StructuralReducer.map.put(act, newClass = Util.createFreshClass(
							StructuralReducer.onto, 
							StructuralReducer.dataFactory, 
							StructuralReducer.manager));
					StructuralReducer.stack.add(act);
					reducedOp.add(newClass);
				}
				else
					reducedOp.add(StructuralReducer.map.get(act));
			}
			else
				reducedOp.add(act);
		}
		
		if(!reduced){			
			StructuralReducer.toDelete.add(axiom);
			StructuralReducer.toAdd.add(newAxiom =
					StructuralReducer.dataFactory.getOWLEquivalentClassesAxiom(
							reducedOp,
							axiom.getAnnotations()));
			if(Params.verbose)
                logger.debug("{}\n", newAxiom);
		}
	}

	@Override
	public void visit(OWLDataPropertyDomainAxiom axiom) {
		OWLAxiom newAxiom = null;
		
		if(Params.verbose)
            logger.debug("{} reduction", axiom.getClass());
		OWLClass newClass = null;
		OWLClassExpression domain = axiom.getDomain();
		
		if(!domain.isClassExpressionLiteral()){
			if(StructuralReducer.map.containsKey(domain))
				newClass = StructuralReducer.map.get(domain);
			else{				
				StructuralReducer.map.put(domain, newClass = Util.createFreshClass(
						StructuralReducer.onto, 
						StructuralReducer.dataFactory, 
						StructuralReducer.manager));
				StructuralReducer.stack.add(domain);
				
				StructuralReducer.toDelete.add(axiom);
				StructuralReducer.toAdd.add(newAxiom =
						StructuralReducer.dataFactory.getOWLDataPropertyDomainAxiom(
								axiom.getProperty(),
								newClass,
								axiom.getAnnotations()));
				if(Params.verbose)
                    logger.debug("{}\n", newAxiom);
			}
		}
	}

	@Override
	public void visit(OWLObjectPropertyDomainAxiom axiom) {
		OWLAxiom newAxiom = null;
		
		if(Params.verbose)
            logger.debug("{} reduction", axiom.getClass());
		OWLClass newClass = null;
		OWLClassExpression domain = axiom.getDomain();
		
		if(!domain.isClassExpressionLiteral()){
			if(StructuralReducer.map.containsKey(domain))
				newClass = StructuralReducer.map.get(domain);
			else{				
				StructuralReducer.map.put(domain, newClass = Util.createFreshClass(
						StructuralReducer.onto, 
						StructuralReducer.dataFactory, 
						StructuralReducer.manager));
				StructuralReducer.stack.add(domain);
				
				StructuralReducer.toDelete.add(axiom);
				StructuralReducer.toAdd.add(newAxiom =
						StructuralReducer.dataFactory.getOWLObjectPropertyDomainAxiom(
								axiom.getProperty(),
								newClass,
								axiom.getAnnotations()));
				if(Params.verbose)
                    logger.debug("{}\n", newAxiom);
			}
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
		
		if(Params.verbose)
            logger.debug("{} reduction", axiom.getClass());
		OWLClass newClass = null;
		OWLClassExpression range = axiom.getRange();
		
		if(!range.isClassExpressionLiteral()){
			if(StructuralReducer.map.containsKey(range))
				newClass = StructuralReducer.map.get(range);
			else{				
				StructuralReducer.map.put(range, newClass = Util.createFreshClass(
						StructuralReducer.onto, 
						StructuralReducer.dataFactory, 
						StructuralReducer.manager));
				StructuralReducer.stack.add(range);
				
				StructuralReducer.toDelete.add(axiom);
				StructuralReducer.toAdd.add(newAxiom =
						StructuralReducer.dataFactory.getOWLObjectPropertyRangeAxiom(
								axiom.getProperty(),
								newClass,
								axiom.getAnnotations()));
				if(Params.verbose)
                    logger.debug("{}\n", newAxiom);
			}
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
