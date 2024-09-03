package reducer;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;

import sid.OntView2.utils.Params;
import sid.OntView2.utils.Util;


public class OWLClassExpressioneReducerVisitor implements
		OWLClassExpressionVisitor {

	@Override
	public void visit(OWLClass ce) {
		return;
	}

	@Override
	public void visit(OWLObjectIntersectionOf ce) {
		if(Params.verbose)
			System.out.println(ce.getClass() + " reduction");
		
		Set<OWLClassExpression> reducedOp = new HashSet<OWLClassExpression>();
		OWLClass newClass = null;
		
		for (OWLClassExpression op : ce.getOperandsAsList()) {
			if(op.isClassExpressionLiteral())
				reducedOp.add(ce);
			else {
				if(StructuralReducer.map.containsKey(op))
					reducedOp.add(StructuralReducer.map.get(op));						
				else {
					reducedOp.add(newClass = Util.createFreshClass(
							StructuralReducer.onto, 
							StructuralReducer.dataFactory, 
							StructuralReducer.manager));
					StructuralReducer.map.put(op, newClass);
					StructuralReducer.stack.push(op);
				}
			}
		}
		
		StructuralReducer.toAdd.add(
				StructuralReducer.dataFactory.getOWLEquivalentClassesAxiom(
						StructuralReducer.map.get(ce), 
						StructuralReducer.dataFactory.getOWLObjectIntersectionOf(reducedOp)
				)
		);
	}

	@Override
	public void visit(OWLObjectUnionOf ce) {
		if(Params.verbose)
			System.out.println(ce.getClass() + " reduction");

		Set<OWLClassExpression> reducedOp = new HashSet<OWLClassExpression>();
		OWLClass newClass = null;
		
		for (OWLClassExpression op : ce.getOperandsAsList()) {
			if(op.isClassExpressionLiteral())
				reducedOp.add(ce);
			else {
				if(StructuralReducer.map.containsKey(op))
					reducedOp.add(StructuralReducer.map.get(op));						
				else {
					reducedOp.add(newClass = Util.createFreshClass(
							StructuralReducer.onto, 
							StructuralReducer.dataFactory, 
							StructuralReducer.manager));
					StructuralReducer.map.put(op, newClass);
					StructuralReducer.stack.push(op);
				}
			}
		}
		
		StructuralReducer.toAdd.add(
				StructuralReducer.dataFactory.getOWLEquivalentClassesAxiom(
						StructuralReducer.map.get(ce), 
						StructuralReducer.dataFactory.getOWLObjectUnionOf(reducedOp)
				)
		);
	}

	@Override
	public void visit(OWLObjectComplementOf ce) {
		if(Params.verbose)
			System.out.println(ce.getClass() + " reduction");

		OWLClassExpression reducedOp = null,
				op = ce.getOperand();
		
		if(!op.isClassExpressionLiteral()){
			if(StructuralReducer.map.containsKey(op))
					reducedOp= StructuralReducer.map.get(op);
			else {
				reducedOp = Util.createFreshClass(
						StructuralReducer.onto, 
						StructuralReducer.dataFactory, 
						StructuralReducer.manager);
				
				StructuralReducer.map.put(op, reducedOp.asOWLClass());
				StructuralReducer.stack.push(op);
			}
		}
		else
			reducedOp = op;
		
		StructuralReducer.toAdd.add(
				StructuralReducer.dataFactory.getOWLEquivalentClassesAxiom(
						StructuralReducer.map.get(ce), 
						StructuralReducer.dataFactory.getOWLObjectComplementOf(reducedOp)
				)
		);
	}

	@Override
	public void visit(OWLObjectSomeValuesFrom ce) {
		if(Params.verbose)
			System.out.println(ce.getClass() + " reduction");

		OWLClassExpression reducedOp = null,
				op = ce.getFiller();
		
		if(!op.isClassExpressionLiteral()){
			if(StructuralReducer.map.containsKey(op))
					reducedOp= StructuralReducer.map.get(op);
			else {
				reducedOp = Util.createFreshClass(
						StructuralReducer.onto, 
						StructuralReducer.dataFactory, 
						StructuralReducer.manager);
				
				StructuralReducer.map.put(op, reducedOp.asOWLClass());
				StructuralReducer.stack.push(op);
			}
		}
		else
			reducedOp = op;
		
		StructuralReducer.toAdd.add(
				StructuralReducer.dataFactory.getOWLEquivalentClassesAxiom(
						StructuralReducer.map.get(ce), 
						StructuralReducer.dataFactory.getOWLObjectComplementOf(reducedOp)
				)
		);
	}

	@Override
	public void visit(OWLObjectAllValuesFrom ce) {
		if(Params.verbose)
			System.out.println(ce.getClass() + " reduction");

		OWLClassExpression reducedOp = null,
				op = ce.getFiller();
		
		if(!op.isClassExpressionLiteral()){
			if(StructuralReducer.map.containsKey(op))
					reducedOp= StructuralReducer.map.get(op);
			else {
				reducedOp = Util.createFreshClass(
						StructuralReducer.onto, 
						StructuralReducer.dataFactory, 
						StructuralReducer.manager);
				
				StructuralReducer.map.put(op, reducedOp.asOWLClass());
				StructuralReducer.stack.push(op);
			}
		}
		else
			reducedOp = op;
		
		StructuralReducer.toAdd.add(
				StructuralReducer.dataFactory.getOWLEquivalentClassesAxiom(
						StructuralReducer.map.get(ce), 
						StructuralReducer.dataFactory.getOWLObjectComplementOf(reducedOp)
				)
		);
	}

	@Override
	public void visit(OWLObjectMinCardinality ce) {
		if(Params.verbose)
			System.out.println(ce.getClass() + " reduction");

		OWLClassExpression reducedOp = null,
				op = ce.getFiller();
		
		if(!op.isClassExpressionLiteral()){
			if(StructuralReducer.map.containsKey(op))
					reducedOp= StructuralReducer.map.get(op);
			else {
				reducedOp = Util.createFreshClass(
						StructuralReducer.onto, 
						StructuralReducer.dataFactory, 
						StructuralReducer.manager);
				
				StructuralReducer.map.put(op, reducedOp.asOWLClass());
				StructuralReducer.stack.push(op);
			}
		}
		else
			reducedOp = op;
		
		StructuralReducer.toAdd.add(
				StructuralReducer.dataFactory.getOWLEquivalentClassesAxiom(
						StructuralReducer.map.get(ce), 
						StructuralReducer.dataFactory.getOWLObjectMinCardinality(ce.getCardinality(),ce.getProperty(),reducedOp)
				)
		);
	}

	@Override
	public void visit(OWLObjectExactCardinality ce) {
		if(Params.verbose)
			System.out.println(ce.getClass() + " reduction");

		OWLClassExpression reducedOp = null,
				op = ce.getFiller();
		
		if(!op.isClassExpressionLiteral()){
			if(StructuralReducer.map.containsKey(op))
					reducedOp= StructuralReducer.map.get(op);
			else {
				reducedOp = Util.createFreshClass(
						StructuralReducer.onto, 
						StructuralReducer.dataFactory, 
						StructuralReducer.manager);
				
				StructuralReducer.map.put(op, reducedOp.asOWLClass());
				StructuralReducer.stack.push(op);
			}
		}
		else
			reducedOp = op;
		
		StructuralReducer.toAdd.add(
				StructuralReducer.dataFactory.getOWLEquivalentClassesAxiom(
						StructuralReducer.map.get(ce), 
						StructuralReducer.dataFactory.getOWLObjectExactCardinality(ce.getCardinality(),ce.getProperty(),reducedOp)
				)
		);
	}

	@Override
	public void visit(OWLObjectMaxCardinality ce) {
		if(Params.verbose)
			System.out.println(ce.getClass() + " reduction");

		OWLClassExpression reducedOp = null,
				op = ce.getFiller();
		
		if(!op.isClassExpressionLiteral()){
			if(StructuralReducer.map.containsKey(op))
					reducedOp= StructuralReducer.map.get(op);
			else {
				reducedOp = Util.createFreshClass(
						StructuralReducer.onto, 
						StructuralReducer.dataFactory, 
						StructuralReducer.manager);
				
				StructuralReducer.map.put(op, reducedOp.asOWLClass());
				StructuralReducer.stack.push(op);
			}
		}
		else
			reducedOp = op;
		
		StructuralReducer.toAdd.add(
				StructuralReducer.dataFactory.getOWLEquivalentClassesAxiom(
						StructuralReducer.map.get(ce), 
						StructuralReducer.dataFactory.getOWLObjectMaxCardinality(ce.getCardinality(),ce.getProperty(),reducedOp)
				)
		);
	}


	@Override
	public void visit(OWLObjectHasValue ce) {
		return;
	}

	@Override
	public void visit(OWLObjectHasSelf ce) {
		return;
	}

	@Override
	public void visit(OWLObjectOneOf ce) {
		return;
	}

	@Override
	public void visit(OWLDataSomeValuesFrom ce) {
		return;
	}

	@Override
	public void visit(OWLDataAllValuesFrom ce) {
		return;
	}

	@Override
	public void visit(OWLDataHasValue ce) {
		return;
	}

	@Override
	public void visit(OWLDataMinCardinality ce) {
		return;
	}

	@Override
	public void visit(OWLDataExactCardinality ce) {
		return;
	}

	@Override
	public void visit(OWLDataMaxCardinality ce) {
		return;
	}

}
