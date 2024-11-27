package sid.OntView2.utils;

import java.util.Objects;
import java.util.Set;

import org.semanticweb.owlapi.rdf.rdfxml.renderer.OWLOntologyXMLNamespaceManager;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataComplementOf;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataIntersectionOf;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataOneOf;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDataUnionOf;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeRestriction;
import org.semanticweb.owlapi.model.OWLFacetRestriction;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import sid.OntView2.common.OntViewConstants;

public class ExpressionManager {

	public static OWLOntologyXMLNamespaceManager manager = null;
	public static String currentOntologyIRI = null;

	public static void setNamespaceManager (OWLOntologyManager om, OWLOntology o) {
		manager = new OWLOntologyXMLNamespaceManager(o, Objects.requireNonNull(om.getOntologyFormat(o)));
		currentOntologyIRI = o.getOntologyID().getOntologyIRI().toString();
	}

	public static OWLOntologyXMLNamespaceManager getNamespaceManager () {
		return manager;
	}

	public static String getReducedClassExpression(OWLClassExpression o){
		String s = getReducedClassExpressionSub(o, 1);
		return replaceString(s);
	}

	public static String getReducedClassExpressionSub(OWLClassExpression o, int level){
		String reduced ="";
		int i;
		ClassExpressionType type = o.getClassExpressionType();
		switch (type){
			case OWL_CLASS :
				reduced += obtainEntityNameFromIRI(o.asOWLClass().getIRI());
				break;
			case OBJECT_ONE_OF:
				reduced = "OneOf(";
				OWLObjectOneOf oneOf = (OWLObjectOneOf) o;
				i = 1;
				for (OWLIndividual op :oneOf.getIndividuals()) {
					reduced += obtainEntityNameFromIRI(op.asOWLNamedIndividual().getIRI());
					if (i<oneOf.getIndividuals().size()) {
						reduced+=",\n";
						for (int j=0; j<level; j++) {
							reduced+="\t";
						}
					}
					i++;
				}
				reduced+=")";
				break;
			case OBJECT_SOME_VALUES_FROM:
				OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) o;
				reduced = OntViewConstants.SOME+".(";
				if (some.getProperty()!= null)
					reduced+= getReducedObjectPropertyExpression(some.getProperty());
				if (some.getFiller()!= null) {
					reduced +=",\n";
					for (int j=0; j<level; j++) reduced+="\t";
					reduced+= getReducedClassExpressionSub((OWLClassExpression) some.getFiller(), level+1);
				}
				reduced +=")";
				break;
			case OBJECT_ALL_VALUES_FROM:
				OWLObjectAllValuesFrom all = (OWLObjectAllValuesFrom) o;
				reduced = OntViewConstants.FOR_ALL+".(";
				if (all.getProperty()!=null)
					reduced+= getReducedObjectPropertyExpression(all.getProperty());

				if (all.getFiller()!= null) {
					reduced +=",\n";
					for (int j=0; j<level; j++) reduced+="\t";
					reduced+= getReducedClassExpressionSub(all.getFiller(), level+1);
				}
				reduced+=")";
				break;
			case OBJECT_COMPLEMENT_OF:
				OWLObjectComplementOf comp = (OWLObjectComplementOf) o;
				reduced = OntViewConstants.COMPLEMENT+"(";
				reduced += getReducedClassExpressionSub(comp.getOperand(), level+1);
				reduced+=")";
				break;
			case OBJECT_EXACT_CARDINALITY:
				OWLObjectExactCardinality exact = (OWLObjectExactCardinality) o;
				reduced = "="+exact.getCardinality()+ "(";
				if (exact.getProperty()!=null)
					reduced+= getReducedObjectPropertyExpression(exact.getProperty());
				if (exact.getFiller()!= null) {
					reduced += ",\n";
					for (int j=0; j<level; j++) reduced+="\t";
					reduced+= getReducedClassExpressionSub(exact.getFiller(), level+1);
				}
				reduced+=")";
				break;
			case OBJECT_HAS_SELF:
				OWLObjectHasSelf self = (OWLObjectHasSelf) o;
				reduced = "hasSelf("+getReducedObjectPropertyExpression(self.getProperty());
				reduced+= ")";
				break;
			case OBJECT_HAS_VALUE:
				OWLObjectHasValue h = (OWLObjectHasValue) o;
				reduced = OntViewConstants.HASVALUE + "(";
				reduced += getReducedObjectPropertyExpression(h.getProperty());
				reduced += ":"+obtainEntityNameFromIRI(h.getValue().asOWLNamedIndividual().getIRI()) +")";
				break;
			case OBJECT_INTERSECTION_OF:
				OWLObjectIntersectionOf inter = (OWLObjectIntersectionOf) o;
				reduced = OntViewConstants.AND + ".(";
				i = 1;
				for (OWLClassExpression op : inter.getOperands()){
					reduced += getReducedClassExpressionSub(op, level+1);
					if (i<inter.getOperands().size()) {
						reduced+=",\n";
						for (int j=0; j<level; j++) {
							reduced+="\t";
						}
					}
					i++;
				}
				reduced+=")";
				break;
			case OBJECT_MAX_CARDINALITY:
				OWLObjectMaxCardinality max = (OWLObjectMaxCardinality) o;
				reduced = OntViewConstants.LOWER_EQUAL+max.getCardinality()+ "(";
				if (max.getProperty()!=null) {
					reduced+= getReducedObjectPropertyExpression(max.getProperty()) ;
				}
				if (max.getFiller()!= null) {
					reduced += ",\n";
					for (int j=0; j<level; j++) reduced+="\t";
					reduced+= getReducedClassExpressionSub(max.getFiller(), level+1);
				}
				reduced+=")";
				break;
			case OBJECT_MIN_CARDINALITY:
				OWLObjectMinCardinality min = (OWLObjectMinCardinality) o;
				reduced = OntViewConstants.GREATER_EQUAL+min.getCardinality()+ "(";
				if (min.getProperty()!=null) {
					reduced+= getReducedObjectPropertyExpression(min.getProperty());
				}
				if (min.getFiller()!= null) {
					reduced+=",\n";
					for (int j=0; j<level; j++) reduced+="\t";
					reduced+= getReducedClassExpressionSub(min.getFiller(), level+1);
				}
				reduced+=")";
				break;
			case OBJECT_UNION_OF:
				OWLObjectUnionOf u = (OWLObjectUnionOf) o;
				i=1;
				reduced = OntViewConstants.OR + ".(";
				for (OWLClassExpression op : u.getOperands()){
					reduced += getReducedClassExpressionSub(op, level+1);
					if (i<u.getOperands().size())  {
						reduced+=",\n";
						for (int j=0; j<level; j++) {
							reduced+="\t";
						}
					}
					i++;
				}
				reduced+=")";
				break;

			case DATA_HAS_VALUE :
				OWLDataHasValue dhas= (OWLDataHasValue) o;
				reduced = OntViewConstants.HASVALUE + ".(";
				reduced += getReducedDataPropertyExpression(dhas.getProperty())+":";
				reduced += dhas.getValue()+")";
				reduced+=")";
				break;
			case DATA_ALL_VALUES_FROM:
				OWLDataAllValuesFrom dall= (OWLDataAllValuesFrom) o;
				reduced = OntViewConstants.FOR_ALL + ".(";
				reduced += getReducedDataPropertyExpression(dall.getProperty())+",";
				reduced += getReducedDataRange(dall.getFiller());
				reduced+=")";
				break;
			case DATA_EXACT_CARDINALITY:
				OWLDataExactCardinality dexact = (OWLDataExactCardinality) o;
				reduced = "="+ dexact.getCardinality() + "(";
				reduced += getReducedDataPropertyExpression(dexact.getProperty())+","; ;
				reduced += getReducedDataRange(dexact.getFiller());
				reduced+=")";
				break;
			case DATA_MAX_CARDINALITY:
				OWLDataMaxCardinality dmax = (OWLDataMaxCardinality) o;
				reduced = OntViewConstants.LOWER_EQUAL + dmax.getCardinality() + "(";
				reduced += getReducedDataPropertyExpression(dmax.getProperty())+",";
				reduced += getReducedDataRange(dmax.getFiller());
				reduced+=")";
				break;
			case DATA_MIN_CARDINALITY:

				OWLDataMinCardinality dmin = (OWLDataMinCardinality) o;
				reduced = OntViewConstants.GREATER_EQUAL+ dmin.getCardinality() + "(";
				reduced += getReducedDataPropertyExpression(dmin.getProperty())+",";
				reduced += getReducedDataRange(dmin.getFiller());
				reduced+=")";
				break;

			case DATA_SOME_VALUES_FROM:

				OWLDataSomeValuesFrom dsome = (OWLDataSomeValuesFrom) o;
				reduced = OntViewConstants.SOME + "(";
				reduced += getReducedDataPropertyExpression(dsome.getProperty())+",";
				reduced += getReducedDataRange(dsome.getFiller());
				reduced+=")";
				break;

			default :
				reduced += o.toString();
		}

		//		replaceString(reduce);
		return reduced;
	}



	public static String getReducedQualifiedClassExpression(OWLClassExpression o){
		String s = getReducedQualifiedClassExpressionSub(o, 1);
		return replaceString(s);
	}

	public static String getReducedQualifiedClassExpressionSub(OWLClassExpression o, int level){
		String reduced ="";
		int i;
		ClassExpressionType type = o.getClassExpressionType();
		switch (type){
			case OWL_CLASS :
				reduced += obtainQualifiedEntityNameFromIRI(o.asOWLClass().getIRI());
				break;
			case OBJECT_ONE_OF:
				reduced = "OneOf(";
				OWLObjectOneOf oneOf = (OWLObjectOneOf) o;
				i = 1;
				for (OWLIndividual op :oneOf.getIndividuals()) {
					reduced += obtainQualifiedEntityNameFromIRI(op.asOWLNamedIndividual().getIRI());
					if (i<oneOf.getIndividuals().size()) {
						reduced+=",\n";
						for (int j=0; j<level; j++) {
							reduced+="\t";
						}
					}
					i++;
				}
				reduced+=")";
				break;
			case OBJECT_SOME_VALUES_FROM:
				OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) o;
				reduced = OntViewConstants.SOME+".(";
				if (some.getProperty()!= null) {
					reduced+= getReducedQualifiedObjectPropertyExpression(some.getProperty());
				}
				if (some.getFiller()!= null) {
					reduced+=",\n";
					for (int j=0; j<level; j++) {
						reduced+="\t";
					}
					reduced+= ExpressionManager.getReducedQualifiedClassExpressionSub((OWLClassExpression) some.getFiller(), level+1);
				}
				reduced +=")";
				break;
			case OBJECT_ALL_VALUES_FROM:
				OWLObjectAllValuesFrom all = (OWLObjectAllValuesFrom) o;
				reduced = OntViewConstants.FOR_ALL+".(";
				if (all.getProperty()!=null) {
					reduced+= getReducedQualifiedObjectPropertyExpression(all.getProperty());
				}
				if (all.getFiller()!= null) {
					reduced+=",\n";
					for (int j=0; j<level; j++) {
						reduced+="\t";
					}
					reduced+= ExpressionManager.getReducedQualifiedClassExpressionSub(all.getFiller(), level+1);
				}
				reduced+=")";
				break;
			case OBJECT_COMPLEMENT_OF:
				OWLObjectComplementOf comp = (OWLObjectComplementOf) o;
				reduced = OntViewConstants.COMPLEMENT+"(";
				reduced += ExpressionManager.getReducedQualifiedClassExpressionSub(comp.getOperand(), level+1);
				reduced+=")";
				break;
			case OBJECT_EXACT_CARDINALITY:
				OWLObjectExactCardinality exact = (OWLObjectExactCardinality) o;
				reduced = "="+exact.getCardinality()+ "(";
				if (exact.getProperty()!=null)
					reduced+= getReducedQualifiedObjectPropertyExpression(exact.getProperty());
				if (exact.getFiller()!= null) {
					reduced+=",\n";
					for (int j=0; j<level; j++) {
						reduced+="\t";
					}
					reduced+= ExpressionManager.getReducedQualifiedClassExpressionSub(exact.getFiller(), level+1);
				}
				reduced+=")";
				break;
			case OBJECT_HAS_SELF:
				OWLObjectHasSelf self = (OWLObjectHasSelf) o;
				reduced = "hasSelf("+getReducedQualifiedObjectPropertyExpression(self.getProperty());
				reduced+= ")";
				break;
			case OBJECT_HAS_VALUE:
				OWLObjectHasValue h = (OWLObjectHasValue) o;
				reduced = OntViewConstants.HASVALUE + "(";
				reduced += getReducedQualifiedObjectPropertyExpression(h.getProperty());
				reduced += ":"+obtainQualifiedEntityNameFromIRI(h.getValue().asOWLNamedIndividual().getIRI()) +")";
				break;
			case OBJECT_INTERSECTION_OF:
				OWLObjectIntersectionOf inter = (OWLObjectIntersectionOf) o;
				reduced = OntViewConstants.AND + ".(";
				i = 1;
				for (OWLClassExpression op : inter.getOperands()){
					reduced += ExpressionManager.getReducedQualifiedClassExpressionSub(op, level+1);
					if (i<inter.getOperands().size()) {
						reduced+=",\n";
						for (int j=0; j<level; j++) {
							reduced+="\t";
						}
					}
					i++;
				}
				reduced+=")";
				break;
			case OBJECT_MAX_CARDINALITY:
				OWLObjectMaxCardinality max = (OWLObjectMaxCardinality) o;
				reduced = OntViewConstants.LOWER_EQUAL+max.getCardinality()+ "(";
				if (max.getProperty()!=null) {
					reduced+= getReducedQualifiedObjectPropertyExpression(max.getProperty());
				}
				if (max.getFiller()!= null) {
					reduced+=",\n";
					for (int j=0; j<level; j++) {
						reduced+="\t";
					}
					reduced+= ExpressionManager.getReducedQualifiedClassExpressionSub(max.getFiller(), level+1);
				}
				reduced+=")";
				break;
			case OBJECT_MIN_CARDINALITY:
				OWLObjectMinCardinality min = (OWLObjectMinCardinality) o;
				reduced = OntViewConstants.GREATER_EQUAL+min.getCardinality()+ "(";
				if (min.getProperty()!=null){
					reduced+= getReducedQualifiedObjectPropertyExpression(min.getProperty());
				}
				if (min.getFiller()!= null) {
					reduced+=",\n";
					for (int j=0; j<level; j++) {
						reduced+="\t";
					}
					reduced+= ExpressionManager.getReducedQualifiedClassExpressionSub(min.getFiller(),level+1);
				}
				reduced+=")";
				break;
			case OBJECT_UNION_OF:
				OWLObjectUnionOf u = (OWLObjectUnionOf) o;
				i=1;
				reduced = OntViewConstants.OR + ".(";
				for (OWLClassExpression op : u.getOperands()){
					reduced += ExpressionManager.getReducedQualifiedClassExpressionSub(op,level+1);
					if (i<u.getOperands().size()){
						reduced+=",";
						for (int j=0; j<level; j++) {
							reduced+="\t";
						}
					}
					i++;
				}
				reduced+=")";
				break;

			case DATA_HAS_VALUE :
				OWLDataHasValue dhas= (OWLDataHasValue) o;
				reduced = OntViewConstants.HASVALUE + ".(";
				reduced += getReducedQualifiedDataPropertyExpression(dhas.getProperty())+":";
				reduced += dhas.getValue()+")";
				reduced+=")";
				break;
			case DATA_ALL_VALUES_FROM:
				OWLDataAllValuesFrom dall= (OWLDataAllValuesFrom) o;
				reduced = OntViewConstants.FOR_ALL + ".(";
				reduced += getReducedQualifiedDataPropertyExpression(dall.getProperty());
				reduced += getReducedDataRange(dall.getFiller());

				reduced+=")";
				break;
			case DATA_EXACT_CARDINALITY:
				OWLDataExactCardinality dexact = (OWLDataExactCardinality) o;
				reduced = "="+ dexact.getCardinality() + "(";
				reduced += getReducedQualifiedDataPropertyExpression(dexact.getProperty());
				reduced += getReducedDataRange(dexact.getFiller());
				break;
			case DATA_MAX_CARDINALITY:
				OWLDataMaxCardinality dmax = (OWLDataMaxCardinality) o;
				reduced = OntViewConstants.LOWER_EQUAL + dmax.getCardinality() + "(";
				reduced += getReducedQualifiedDataPropertyExpression(dmax.getProperty());
				reduced += getReducedDataRange(dmax.getFiller());
				break;
			case DATA_MIN_CARDINALITY:

				OWLDataMinCardinality dmin = (OWLDataMinCardinality) o;
				reduced = OntViewConstants.GREATER_EQUAL+ dmin.getCardinality() + "(";
				reduced += getReducedQualifiedDataPropertyExpression(dmin.getProperty());
				reduced += getReducedDataRange(dmin.getFiller());
				break;

			case DATA_SOME_VALUES_FROM:

				OWLDataSomeValuesFrom dsome = (OWLDataSomeValuesFrom) o;
				reduced = OntViewConstants.SOME + "(";
				reduced += getReducedQualifiedDataPropertyExpression(dsome.getProperty());
				reduced += getReducedDataRange(dsome.getFiller());
				break;



			default :
				reduced += o.toString();
		}

		//		replaceString(reduce);
		return reduced;
	}


	public static String getReducedDataRange(OWLDataRange o){
		int i =1;
		String reduced="";
		switch (o.getDataRangeType()){
			case DATA_COMPLEMENT_OF:
				OWLDataComplementOf dComp = (OWLDataComplementOf) o;
				reduced+=OntViewConstants.COMPLEMENT+".("+ getReducedDataRange(dComp.getDataRange())+")";
				break;
			case DATA_INTERSECTION_OF:
				OWLDataIntersectionOf dInter = (OWLDataIntersectionOf) o;
				reduced+=(OntViewConstants.AND)+".(";
				i=1;
				for ( OWLDataRange op : dInter.getOperands()){
					reduced += getReducedDataRange(op);
					if (i<dInter.getOperands().size())
						reduced+=",";
					i++;
				}
				break;
			case DATA_ONE_OF:
				OWLDataOneOf dOneof = (OWLDataOneOf)o;
				reduced+=("oneOf")+".(";
				i=1;
				for (OWLLiteral  op : dOneof.getValues()){
					reduced += op.getLiteral();
					if (i<dOneof.getValues().size())
						reduced+=",";
					i++;
				}
				reduced +=")";
				break;
			case DATA_UNION_OF:
				OWLDataUnionOf dUnion= (OWLDataUnionOf)o;
				reduced+=(OntViewConstants.OR)+".(";
				i=1;
				for ( OWLDataRange op : dUnion.getOperands()){
					reduced += getReducedDataRange(op);
					if (i<dUnion.getOperands().size())
						reduced+=",";
					i++;
				}
				reduced +=")";

				break;
			case DATATYPE:
				OWLDatatype dType = (OWLDatatype)o;
				reduced+= obtainEntityNameFromIRI(dType.getIRI());
				break;
			case DATATYPE_RESTRICTION:
				OWLDatatypeRestriction dTypeRest = (OWLDatatypeRestriction)o;
				OWLDatatype a = dTypeRest.getDatatype();
//				reduced += "(facet)"+getReducedDataRange(a)+"(";
				i=1;
				Set<OWLFacetRestriction> facets = dTypeRest.getFacetRestrictions();
				for (OWLFacetRestriction fRest : facets){
					reduced+= "("+obtainEntityNameFromIRI(fRest.getFacet().getIRI())+","+fRest.getFacetValue().toString().replaceAll("\"","")+")";
					if (i<facets.size())
						reduced+=",";
					i++;
				}
				reduced +=")";
				break;
			default :
				return o.toString();
		}
		return reduced;
	}

	public static String getReducedObjectPropertyExpression(OWLObjectPropertyExpression o){
		
		if (o instanceof OWLObjectProperty) {
			return obtainEntityNameFromIRI(o.asOWLObjectProperty().getIRI());
		}
		else if (o instanceof OWLObjectInverseOf) {
			OWLObjectPropertyExpression invProp = ((OWLObjectInverseOf) o).getInverseProperty(); 
			return "inverseOf ("+getReducedObjectPropertyExpression(invProp)+")"; 
		}
		else 
		{
			return o.toString();
		}
	}

	public static String getReducedQualifiedObjectPropertyExpression (OWLObjectPropertyExpression e){
		if (e instanceof OWLObjectProperty) {
			return obtainQualifiedEntityNameFromIRI(e.asOWLObjectProperty().getIRI()); 
		}
		else if (e instanceof OWLObjectInverseOf) {
			OWLObjectPropertyExpression invProp = ((OWLObjectInverseOf) e).getInverseProperty(); 
			return "inverseOf ("+getReducedQualifiedObjectPropertyExpression(invProp)+")"; 
		}
		else
			return e.toString();
	}


	public static String getReducedDataPropertyExpression(OWLDataPropertyExpression o){
		if (o instanceof OWLDataProperty) {
			return obtainEntityNameFromIRI(o.asOWLDataProperty().getIRI());
		}
		else {
			return o.toString();
		}
	}


	public static String getReducedQualifiedDataPropertyExpression(OWLDataPropertyExpression e){
		if (e instanceof OWLDataProperty) {
			return obtainQualifiedEntityNameFromIRI(e.asOWLDataProperty().getIRI());
		}
		else {
			return e.toString();
		}
	}



	public static String replaceString(String in){
		String rep;
		rep = in.replaceAll("\\^\\^xsd:boolean","");
		rep = rep.replaceAll("\\^\\^xsd:decimal","");
		rep = rep.replaceAll("\\^\\^xsd:string","");
		rep = rep.replaceAll("\\^\\^xsd:strrepg","");
		rep = rep.replaceAll("\\^\\^xsd:float","");
		rep = rep.replaceAll("\\^\\^xsd:double","");
		rep = rep.replaceAll("\\^\\^xsd:duration","");

		rep = rep.replaceAll("\\^\\^xsd:integer","");
		rep = rep.replaceAll("\\^\\^xsd:dateTime","");
		rep = rep.replaceAll("\\^\\^xsd:time","");

		rep = rep.replaceAll("\\^\\^integer","");
		rep = rep.replaceAll("\\(int ","(");

		rep = rep.replaceAll("minInclusive", OntViewConstants.GREATER_EQUAL);
		rep = rep.replaceAll("maxInclusive", OntViewConstants.LOWER_EQUAL);
		rep = rep.replaceAll("xsd:float","");
		rep = rep.replaceAll("xsd:double","");
		rep = rep.replaceAll("xsd:duration","");
		rep = rep.replaceAll("xsd:dateTime","");
		rep = rep.replaceAll("xsd:time","");
		rep = rep.replaceAll("xsd:","");
		return rep;
	}

	public static String obtainEntityNameFromIRI (IRI iri) {
		String result = iri.getFragment();
		if ( result == null) {
			try {
				result = iri.toString().substring(iri.toString().lastIndexOf('/'));
			}
			catch (Exception e) {
				result = "WrongFormat";
			}
		}
		return result;
	}

	public static String obtainQualifiedEntityNameFromIRI(IRI iri) {
		if (manager == null) System.out.println("manager null");
		else if (iri == null) System.out.println(" iri null");
		return manager.getQName(iri.toString());
	}


	public static String qualifyLabel(OWLClass c, String label) {

		String result = label;

		String aux = manager.getQName(c.getIRI().toString());

		if (aux!= null) {
			if (aux.contains(":")) {
				String prefix = aux.substring(0, aux.indexOf(':'));
				result = prefix+":"+label;
			}
		}
		result = replaceString(result);
		return result;
	}


}
