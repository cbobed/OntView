package sid.OntView2.utils;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import sid.OntView2.main.ClassExpression;

public class ExpressionManager {
    private static final Logger logger = LogManager.getLogger(ExpressionManager.class);

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
		StringBuilder reduced = new StringBuilder();
		int i;
		ClassExpressionType type = o.getClassExpressionType();
		switch (type){
			case OWL_CLASS :
				reduced.append(obtainEntityNameFromIRI(o.asOWLClass().getIRI()));
				break;
			case OBJECT_ONE_OF:
				reduced = new StringBuilder("OneOf(");
				OWLObjectOneOf oneOf = (OWLObjectOneOf) o;
                List<OWLIndividual> individuals = oneOf.individuals().toList();
				i = 1;
				for (OWLIndividual op :individuals) {
					reduced.append(obtainEntityNameFromIRI(op.asOWLNamedIndividual().getIRI()));
					if (i<individuals.size()) {
						reduced.append(",\n");
                        reduced.append("\t".repeat(Math.max(0, level)));
					}
					i++;
				}
				reduced.append(")");
				break;
			case OBJECT_SOME_VALUES_FROM:
				OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) o;
				reduced = new StringBuilder(OntViewConstants.SOME + ".(");
				if (some.getProperty()!= null)
					reduced.append(getReducedObjectPropertyExpression(some.getProperty()));
				if (some.getFiller()!= null) {
					reduced.append(",\n");
                    reduced.append("\t".repeat(Math.max(0, level)));
					reduced.append(getReducedClassExpressionSub(some.getFiller(), level + 1));
				}
				reduced.append(")");
				break;
			case OBJECT_ALL_VALUES_FROM:
				OWLObjectAllValuesFrom all = (OWLObjectAllValuesFrom) o;
				reduced = new StringBuilder(OntViewConstants.FOR_ALL + ".(");
				if (all.getProperty()!=null)
					reduced.append(getReducedObjectPropertyExpression(all.getProperty()));

				if (all.getFiller()!= null) {
					reduced.append(",\n");
                    reduced.append("\t".repeat(Math.max(0, level)));
					reduced.append(getReducedClassExpressionSub(all.getFiller(), level + 1));
				}
				reduced.append(")");
				break;
			case OBJECT_COMPLEMENT_OF:
				OWLObjectComplementOf comp = (OWLObjectComplementOf) o;
				reduced = new StringBuilder(OntViewConstants.COMPLEMENT + "(");
				reduced.append(getReducedClassExpressionSub(comp.getOperand(), level + 1));
				reduced.append(")");
				break;
			case OBJECT_EXACT_CARDINALITY:
				OWLObjectExactCardinality exact = (OWLObjectExactCardinality) o;
				reduced = new StringBuilder("=" + exact.getCardinality() + "(");
				if (exact.getProperty()!=null)
					reduced.append(getReducedObjectPropertyExpression(exact.getProperty()));
				if (exact.getFiller()!= null) {
					reduced.append(",\n");
                    reduced.append("\t".repeat(Math.max(0, level)));
					reduced.append(getReducedClassExpressionSub(exact.getFiller(), level + 1));
				}
				reduced.append(")");
				break;
			case OBJECT_HAS_SELF:
				OWLObjectHasSelf self = (OWLObjectHasSelf) o;
				reduced = new StringBuilder("hasSelf(" + getReducedObjectPropertyExpression(self.getProperty()));
				reduced.append(")");
				break;
			case OBJECT_HAS_VALUE:
				OWLObjectHasValue h = (OWLObjectHasValue) o;
				reduced = new StringBuilder(OntViewConstants.HAS_VALUE + "(");
				reduced.append(getReducedObjectPropertyExpression(h.getProperty()));
				reduced.append(":").append(obtainEntityNameFromIRI(h.getFiller().asOWLNamedIndividual().getIRI())).append(")");
				break;
			case OBJECT_INTERSECTION_OF:
				OWLObjectIntersectionOf inter = (OWLObjectIntersectionOf) o;
				reduced = new StringBuilder(OntViewConstants.AND + ".(");
                List<OWLClassExpression> operandsInter = inter.operands().toList();
				i = 1;
				for (OWLClassExpression op : operandsInter){
					reduced.append(getReducedClassExpressionSub(op, level + 1));
					if (i<operandsInter.size()) {
						reduced.append(",\n");
                        reduced.append("\t".repeat(Math.max(0, level)));
					}
					i++;
				}
				reduced.append(")");
				break;
			case OBJECT_MAX_CARDINALITY:
				OWLObjectMaxCardinality max = (OWLObjectMaxCardinality) o;
				reduced = new StringBuilder(OntViewConstants.LOWER_EQUAL + max.getCardinality() + "(");
				if (max.getProperty()!=null) {
					reduced.append(getReducedObjectPropertyExpression(max.getProperty()));
				}
				if (max.getFiller()!= null) {
					reduced.append(",\n");
                    reduced.append("\t".repeat(Math.max(0, level)));
					reduced.append(getReducedClassExpressionSub(max.getFiller(), level + 1));
				}
				reduced.append(")");
				break;
			case OBJECT_MIN_CARDINALITY:
				OWLObjectMinCardinality min = (OWLObjectMinCardinality) o;
				reduced = new StringBuilder(OntViewConstants.GREATER_EQUAL + min.getCardinality() + "(");
				if (min.getProperty()!=null) {
					reduced.append(getReducedObjectPropertyExpression(min.getProperty()));
				}
				if (min.getFiller()!= null) {
					reduced.append(",\n");
                    reduced.append("\t".repeat(Math.max(0, level)));
					reduced.append(getReducedClassExpressionSub(min.getFiller(), level + 1));
				}
				reduced.append(")");
				break;
			case OBJECT_UNION_OF:
				OWLObjectUnionOf u = (OWLObjectUnionOf) o;
				i=1;
				reduced = new StringBuilder(OntViewConstants.OR + ".(");
                List<OWLClassExpression> operandsU = u.operands().toList();

                for (OWLClassExpression op : operandsU){
					reduced.append(getReducedClassExpressionSub(op, level + 1));
					if (i<operandsU.size())  {
						reduced.append(",\n");
                        reduced.append("\t".repeat(Math.max(0, level)));
					}
					i++;
				}
				reduced.append(")");
				break;

			case DATA_HAS_VALUE :
				OWLDataHasValue dHas= (OWLDataHasValue) o;
				reduced = new StringBuilder(OntViewConstants.HAS_VALUE + ".(");
				reduced.append(getReducedDataPropertyExpression(dHas.getProperty())).append(":");
				reduced.append(dHas.getFiller()).append(")");
				reduced.append(")");
				break;
			case DATA_ALL_VALUES_FROM:
				OWLDataAllValuesFrom dAll= (OWLDataAllValuesFrom) o;
				reduced = new StringBuilder(OntViewConstants.FOR_ALL + ".(");
				reduced.append(getReducedDataPropertyExpression(dAll.getProperty())).append(",");
				reduced.append(getReducedDataRange(dAll.getFiller()));
				reduced.append(")");
				break;
			case DATA_EXACT_CARDINALITY:
				OWLDataExactCardinality dExact = (OWLDataExactCardinality) o;
				reduced = new StringBuilder("=" + dExact.getCardinality() + "(");
				reduced.append(getReducedDataPropertyExpression(dExact.getProperty())).append(",");
				reduced.append(getReducedDataRange(dExact.getFiller()));
				reduced.append(")");
				break;
			case DATA_MAX_CARDINALITY:
				OWLDataMaxCardinality dMax = (OWLDataMaxCardinality) o;
				reduced = new StringBuilder(OntViewConstants.LOWER_EQUAL + dMax.getCardinality() + "(");
				reduced.append(getReducedDataPropertyExpression(dMax.getProperty())).append(",");
				reduced.append(getReducedDataRange(dMax.getFiller()));
				reduced.append(")");
				break;
			case DATA_MIN_CARDINALITY:
				OWLDataMinCardinality dMin = (OWLDataMinCardinality) o;
				reduced = new StringBuilder(OntViewConstants.GREATER_EQUAL + dMin.getCardinality() + "(");
				reduced.append(getReducedDataPropertyExpression(dMin.getProperty())).append(",");
				reduced.append(getReducedDataRange(dMin.getFiller()));
				reduced.append(")");
				break;
			case DATA_SOME_VALUES_FROM:
				OWLDataSomeValuesFrom dSome = (OWLDataSomeValuesFrom) o;
				reduced = new StringBuilder(OntViewConstants.SOME + "(");
				reduced.append(getReducedDataPropertyExpression(dSome.getProperty())).append(",");
				reduced.append(getReducedDataRange(dSome.getFiller()));
				reduced.append(")");
				break;
			default :
				reduced.append(o);
		}
        return reduced.toString();
	}

	public static String getReducedQualifiedClassExpression(OWLClassExpression o){
		String s = getReducedQualifiedClassExpressionSub(o, 1);
		return replaceString(s);
	}

	public static String getReducedQualifiedClassExpressionSub(OWLClassExpression o, int level){
		StringBuilder reduced = new StringBuilder();
		int i;
		ClassExpressionType type = o.getClassExpressionType();
		switch (type){
			case OWL_CLASS :
				reduced.append(obtainQualifiedEntityNameFromIRI(o.asOWLClass().getIRI()));
				break;
			case OBJECT_ONE_OF:
				reduced = new StringBuilder("OneOf(");
				OWLObjectOneOf oneOf = (OWLObjectOneOf) o;
                List<OWLIndividual> individuals = oneOf.individuals().toList();
                i = 1;
				for (OWLIndividual op :individuals) {
					reduced.append(obtainQualifiedEntityNameFromIRI(op.asOWLNamedIndividual().getIRI()));
					if (i<individuals.size()) {
						reduced.append(",\n");
                        reduced.append("\t".repeat(Math.max(0, level)));
					}
					i++;
				}
				reduced.append(")");
				break;
			case OBJECT_SOME_VALUES_FROM:
				OWLObjectSomeValuesFrom some = (OWLObjectSomeValuesFrom) o;
				reduced = new StringBuilder(OntViewConstants.SOME + ".(");
				if (some.getProperty()!= null) {
					reduced.append(getReducedQualifiedObjectPropertyExpression(some.getProperty()));
				}
				if (some.getFiller()!= null) {
					reduced.append(",\n");
                    reduced.append("\t".repeat(Math.max(0, level)));
					reduced.append(ExpressionManager.getReducedQualifiedClassExpressionSub(some.getFiller(), level + 1));
				}
				reduced.append(")");
				break;
			case OBJECT_ALL_VALUES_FROM:
				OWLObjectAllValuesFrom all = (OWLObjectAllValuesFrom) o;
				reduced = new StringBuilder(OntViewConstants.FOR_ALL + ".(");
				if (all.getProperty()!=null) {
					reduced.append(getReducedQualifiedObjectPropertyExpression(all.getProperty()));
				}
				if (all.getFiller()!= null) {
					reduced.append(",\n");
                    reduced.append("\t".repeat(Math.max(0, level)));
					reduced.append(ExpressionManager.getReducedQualifiedClassExpressionSub(all.getFiller(), level + 1));
				}
				reduced.append(")");
				break;
			case OBJECT_COMPLEMENT_OF:
				OWLObjectComplementOf comp = (OWLObjectComplementOf) o;
				reduced = new StringBuilder(OntViewConstants.COMPLEMENT + "(");
				reduced.append(ExpressionManager.getReducedQualifiedClassExpressionSub(comp.getOperand(), level + 1));
				reduced.append(")");
				break;
			case OBJECT_EXACT_CARDINALITY:
				OWLObjectExactCardinality exact = (OWLObjectExactCardinality) o;
				reduced = new StringBuilder("=" + exact.getCardinality() + "(");
				if (exact.getProperty()!=null)
					reduced.append(getReducedQualifiedObjectPropertyExpression(exact.getProperty()));
				if (exact.getFiller()!= null) {
					reduced.append(",\n");
                    reduced.append("\t".repeat(Math.max(0, level)));
					reduced.append(ExpressionManager.getReducedQualifiedClassExpressionSub(exact.getFiller(), level + 1));
				}
				reduced.append(")");
				break;
			case OBJECT_HAS_SELF:
				OWLObjectHasSelf self = (OWLObjectHasSelf) o;
				reduced = new StringBuilder("hasSelf(" + getReducedQualifiedObjectPropertyExpression(self.getProperty()));
				reduced.append(")");
				break;
			case OBJECT_HAS_VALUE:
				OWLObjectHasValue h = (OWLObjectHasValue) o;
				reduced = new StringBuilder(OntViewConstants.HAS_VALUE + "(");
				reduced.append(getReducedQualifiedObjectPropertyExpression(h.getProperty()));
				reduced.append(":").append(obtainQualifiedEntityNameFromIRI(h.getFiller().asOWLNamedIndividual().getIRI())).append(")");
				break;
			case OBJECT_INTERSECTION_OF:
				OWLObjectIntersectionOf inter = (OWLObjectIntersectionOf) o;
				reduced = new StringBuilder(OntViewConstants.AND + ".(");
                List<OWLClassExpression> operandsInter = inter.operands().toList();
				i = 1;
				for (OWLClassExpression op : operandsInter){
					reduced.append(ExpressionManager.getReducedQualifiedClassExpressionSub(op, level + 1));
					if (i<operandsInter.size()) {
						reduced.append(",\n");
                        reduced.append("\t".repeat(Math.max(0, level)));
					}
					i++;
				}
				reduced.append(")");
				break;
			case OBJECT_MAX_CARDINALITY:
				OWLObjectMaxCardinality max = (OWLObjectMaxCardinality) o;
				reduced = new StringBuilder(OntViewConstants.LOWER_EQUAL + max.getCardinality() + "(");
				if (max.getProperty()!=null) {
					reduced.append(getReducedQualifiedObjectPropertyExpression(max.getProperty()));
				}
				if (max.getFiller()!= null) {
					reduced.append(",\n");
                    reduced.append("\t".repeat(Math.max(0, level)));
					reduced.append(ExpressionManager.getReducedQualifiedClassExpressionSub(max.getFiller(), level + 1));
				}
				reduced.append(")");
				break;
			case OBJECT_MIN_CARDINALITY:
				OWLObjectMinCardinality min = (OWLObjectMinCardinality) o;
				reduced = new StringBuilder(OntViewConstants.GREATER_EQUAL + min.getCardinality() + "(");
				if (min.getProperty()!=null){
					reduced.append(getReducedQualifiedObjectPropertyExpression(min.getProperty()));
				}
				if (min.getFiller()!= null) {
					reduced.append(",\n");
                    reduced.append("\t".repeat(Math.max(0, level)));
					reduced.append(ExpressionManager.getReducedQualifiedClassExpressionSub(min.getFiller(), level + 1));
				}
				reduced.append(")");
				break;
			case OBJECT_UNION_OF:
				OWLObjectUnionOf u = (OWLObjectUnionOf) o;
				i=1;
				reduced = new StringBuilder(OntViewConstants.OR + ".(");
                List<OWLClassExpression> operandsU = u.operands().toList();
                for (OWLClassExpression op : operandsU){
					reduced.append(ExpressionManager.getReducedQualifiedClassExpressionSub(op, level + 1));
					if (i<operandsU.size()){
						reduced.append(",\n");
                        reduced.append("\t".repeat(Math.max(0, level)));
					}
					i++;
				}
				reduced.append(")");
				break;

			case DATA_HAS_VALUE :
				OWLDataHasValue dHas= (OWLDataHasValue) o;
				reduced = new StringBuilder(OntViewConstants.HAS_VALUE + ".(");
				reduced.append(getReducedQualifiedDataPropertyExpression(dHas.getProperty())).append(":");
				reduced.append(dHas.getFiller()).append(")");
				reduced.append(")");
				break;
			case DATA_ALL_VALUES_FROM:
				OWLDataAllValuesFrom dAll= (OWLDataAllValuesFrom) o;
				reduced = new StringBuilder(OntViewConstants.FOR_ALL + ".(");
				reduced.append(getReducedQualifiedDataPropertyExpression(dAll.getProperty())).append(",");
				reduced.append(getReducedDataRange(dAll.getFiller()));
				reduced.append(")");
				break;
			case DATA_EXACT_CARDINALITY:
				OWLDataExactCardinality dExact = (OWLDataExactCardinality) o;
				reduced = new StringBuilder("=" + dExact.getCardinality() + "(");
				reduced.append(getReducedQualifiedDataPropertyExpression(dExact.getProperty())).append(",");
				reduced.append(getReducedDataRange(dExact.getFiller()));
                reduced.append(")");
				break;
			case DATA_MAX_CARDINALITY:
				OWLDataMaxCardinality dMax = (OWLDataMaxCardinality) o;
				reduced = new StringBuilder(OntViewConstants.LOWER_EQUAL + dMax.getCardinality() + "(");
				reduced.append(getReducedQualifiedDataPropertyExpression(dMax.getProperty())).append(",");
				reduced.append(getReducedDataRange(dMax.getFiller()));
                reduced.append(")");
				break;
			case DATA_MIN_CARDINALITY:
				OWLDataMinCardinality dMin = (OWLDataMinCardinality) o;
				reduced = new StringBuilder(OntViewConstants.GREATER_EQUAL + dMin.getCardinality() + "(");
				reduced.append(getReducedQualifiedDataPropertyExpression(dMin.getProperty())).append(",");
				reduced.append(getReducedDataRange(dMin.getFiller()));
                reduced.append(")");
				break;
			case DATA_SOME_VALUES_FROM:
				OWLDataSomeValuesFrom dSome = (OWLDataSomeValuesFrom) o;
				reduced = new StringBuilder(OntViewConstants.SOME + "(");
				reduced.append(getReducedQualifiedDataPropertyExpression(dSome.getProperty())).append(",");
				reduced.append(getReducedDataRange(dSome.getFiller()));
                reduced.append(")");
				break;
			default :
				reduced.append(o);
		}
        return reduced.toString();
	}


	public static String getReducedDataRange(OWLDataRange o){
		int i;
		StringBuilder reduced= new StringBuilder();
		switch (o.getDataRangeType()){
			case DATA_COMPLEMENT_OF:
				OWLDataComplementOf dComp = (OWLDataComplementOf) o;
				reduced.append(OntViewConstants.COMPLEMENT + ".(").append(getReducedDataRange(dComp.getDataRange())).append(")");
				break;
			case DATA_INTERSECTION_OF:
				OWLDataIntersectionOf dInter = (OWLDataIntersectionOf) o;
				reduced.append((OntViewConstants.AND) + ".(");
                List<OWLDataRange> rangesDI = dInter.operands().toList();
                i=1;
				for ( OWLDataRange op : rangesDI){
					reduced.append(getReducedDataRange(op));
					if (i<rangesDI.size())
						reduced.append(",\n");
					i++;
				}
				break;
			case DATA_ONE_OF:
				OWLDataOneOf dOneOf = (OWLDataOneOf)o;
				reduced.append(("oneOf") + ".(");
                List<OWLLiteral> valuesDOF = dOneOf.values().toList();
				i=1;
				for (OWLLiteral  op : valuesDOF){
					reduced.append(op.getLiteral());
					if (i<valuesDOF.size())
						reduced.append(",");
					i++;
				}
				reduced.append(")");
				break;
			case DATA_UNION_OF:
				OWLDataUnionOf dUnion= (OWLDataUnionOf)o;
				reduced.append((OntViewConstants.OR) + ".(");
                List<OWLDataRange> rangesDU = dUnion.operands().toList();
                i=1;
				for ( OWLDataRange op : rangesDU){
					reduced.append(getReducedDataRange(op));
					if (i<rangesDU.size())
						reduced.append(",");
					i++;
				}
				reduced.append(")");

				break;
			case DATATYPE:
				OWLDatatype dType = (OWLDatatype)o;
				reduced.append(obtainEntityNameFromIRI(dType.getIRI()));
				break;
			case DATATYPE_RESTRICTION:
				OWLDatatypeRestriction dTypeRest = (OWLDatatypeRestriction)o;
                dTypeRest.getDatatype();
                i=1;
                Set<OWLFacetRestriction> facets = dTypeRest.facetRestrictions().collect(Collectors.toCollection(LinkedHashSet::new));
                for (OWLFacetRestriction fRest : facets){
					reduced.append("(").append(obtainEntityNameFromIRI(fRest.getFacet().getIRI())).append(",")
                        .append(fRest.getFacetValue().toString().replaceAll("\"", "")).append(")");
					if (i<facets.size())
						reduced.append(",");
					i++;
				}
				reduced.append(")");
				break;
			default :
				return o.toString();
		}
		return reduced.toString();
	}

	public static String getReducedObjectPropertyExpression(OWLObjectPropertyExpression o){
		if (o instanceof OWLObjectProperty) {
			return obtainEntityNameFromIRI(o.asOWLObjectProperty().getIRI());
		}
		else if (o instanceof OWLObjectInverseOf) {
			OWLObjectPropertyExpression invProp = o.getInverseProperty();
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
			OWLObjectPropertyExpression invProp = e.getInverseProperty();
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
		if (manager == null) logger.warn("manager null");
		else if (iri == null) logger.warn(" iri null");
        assert iri != null;
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

    public static String qualifyLabel(OWLObjectPropertyExpression pe, String label) {
        String result = label;
        String aux = manager.getQName(pe.getNamedProperty().getIRI().toString());
        if (aux != null) {
            if (aux.contains(":")) {
                String prefix = aux.substring(0, aux.indexOf(':'));
                result = prefix + ":" + label;
            }
        }
        result = replaceString(result);
        return result;
    }
}
