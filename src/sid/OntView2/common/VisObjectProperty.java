package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import sid.OntView2.utils.ExpressionManager;

import javafx.scene.text.Font;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

public class VisObjectProperty extends VisProperty {

	VisPropertyBox pbox;
	int height,width;
	int voffset;
	OWLObjectPropertyExpression oPropExp;
	String label;
	String qualifiedLabel = "";
	String visibleLabel = "";
	Shape range;
	boolean visible = true;
	VisConnectorPropertyRange rangeConnector;
	ArrayList<VisConnectorHeritance> parentConnectors;
	ArrayList<VisObjectProperty> parents;
	HashSet<Point2D> pointSet;
	VisConnectorPropProp onlyParentConnector;
	ArrayList<Point2D> connectionPoints;
	Font textFont;
	Font circleFont;
	
	boolean qualifiedRendering = false; 
	boolean labelRendering = false; 
	
	
	org.semanticweb.owlapi.reasoner.Node<OWLObjectPropertyExpression> inverseOf;

	boolean isTransitive  =  false;
	boolean isSymmetric   =  false;
	boolean isReflexive   =  false;
	boolean isFunctional  =  false;
	boolean isAsymmetric  =  false;
	boolean hasInverse    =  false;
	boolean isIrreflexive =  false;
	boolean isInverseFunctional = false;
	OWLSubPropertyChainOfAxiom propertyChainAxiom = null;

	public int getPosX(){return getDomain().getPosX()-(getDomain().getWidth()/2)+2;}
	public int getPosY(){return  15+getDomain().getPosY()+(getDomain().getHeight()/2)+getLabelHeight()*voffset;}

	public OWLReasoner getReasoner(){return pbox.vclass.graph.paintframe.getReasoner();}
	
	public boolean onProperty(Point2D p){
		return ((p.getX() >= getPosX()-20)&&(p.getX() <= getPosX())&& (p.getY() >= getPosY()-10)&&(p.getY() <= getPosY()));
	}
	
	public String getTooltipText(){
		StringBuilder description = new StringBuilder();
		if (description.toString().isEmpty()){
			description.append("<html><b>").append(visibleLabel).append("</b><br><br>");
			if ((parents != null)&&(parents.size()>1)){
				description.append("<b>Subproperty of</b><br><ul>");
				for (VisObjectProperty p : parents){
					if (!description.toString().contains("<li>" + p.visibleLabel + " </li>")) {
						description.append("<li>").append(p.visibleLabel).append(" </li>");
					}				}
				description.append("<br></ul>");
			}

			if (!getDomain().getVisibleDefinitionLabels().isEmpty()) {
				for (String defLabel : getDomain().getVisibleDefinitionLabels()) {
					description.append("<b>Domain: </b> ").append(defLabel).append("<br>");
				}
			} else {
				description.append("<b>Domain: </b> ").append(getDomain().visibleLabel).append("<br>");
			}

			description.append("<b>Range: </b>");
			description.append(qualifiedRendering ?
                    ExpressionManager.getReducedQualifiedClassExpression(range.getLinkedClassExpression()) :
                    ExpressionManager.getReducedClassExpression(range.getLinkedClassExpression())).append("<br><br>");
			description.append("<b>Property Description</b><br><ul>");

			if (isTransitive)
				description.append("<li>Transitive</li>");
			if (isFunctional)
				description.append("<li>Functional</li>");
			if (isReflexive)
				description.append("<li>Reflexive</li>");
			if (isSymmetric)
				description.append("<li>Symmetric</li>");
			if (hasInverse){
				description.append("Inverse of <b> <br><ul>");
				for (OWLObjectPropertyExpression inv : inverseOf.getEntitiesMinusTop()){
					if (inv instanceof OWLObjectProperty) {
						description.append("<li>").append(qualifiedRendering ?
		                        ExpressionManager.getReducedQualifiedObjectPropertyExpression(inv) :
		                        ExpressionManager.getReducedObjectPropertyExpression(inv)).append("</li>");
					}
				}
				description.append("</b></ul>");
			}
			if (propertyChainAxiom != null) {
				description.append("<b>Chain Property</b><ul>");
				for (OWLObjectPropertyExpression c: propertyChainAxiom.getPropertyChain()){
					description.append("<li>").append(qualifiedRendering ?
                            ExpressionManager.getReducedQualifiedObjectPropertyExpression(c) :
                            ExpressionManager.getReducedObjectPropertyExpression(c)).append("</li>");
				}
				description.append("</ul>");
			}
			
			description.append("</ul></html>");
		}
		return description.toString();
		
	}
	
	
	
	public VisObjectProperty(VisPropertyBox ppbox, OWLObjectPropertyExpression po, int pvoffset, Shape prange, OWLOntology ontology) {
		pbox = ppbox;
		parents  = new ArrayList<VisObjectProperty>();
		label    = ExpressionManager.getReducedObjectPropertyExpression (po);
		visibleLabel = label;
		
		qualifiedLabel = ExpressionManager.getReducedQualifiedObjectPropertyExpression (po); 
		if (qualifiedLabel == null || "null".equalsIgnoreCase(qualifiedLabel)) {
			qualifiedLabel = label; 
		}
		
		oPropExp = po;
		range    = prange;
		voffset  = pvoffset;

		textFont    = Font.font("Arial", FontWeight.NORMAL, FontPosture.REGULAR, 11);
		circleFont  = Font.font("Arial", FontWeight.BOLD, 10);

		connectionPoints = new ArrayList<>();
		if (getDomain() != range) {
			rangeConnector = new VisConnectorPropertyRange(ppbox, pbox.vclass , range, this);
		}
		if (EntitySearcher.isFunctional(oPropExp, ontology)) isFunctional = true;
		if (EntitySearcher.isTransitive(oPropExp, ontology)) isTransitive = true;
		if (EntitySearcher.isSymmetric(oPropExp, ontology)) isSymmetric  = true;
		inverseOf  = getReasoner().getInverseObjectProperties(po);
		hasInverse = ((inverseOf !=null)&&(inverseOf.getEntities().size())>0);
		if (EntitySearcher.isReflexive(oPropExp, ontology)) isReflexive =true;
		if (EntitySearcher.isAsymmetric(oPropExp, ontology)) isAsymmetric = true;
		if (EntitySearcher.isIrreflexive(oPropExp, ontology)) isIrreflexive = true;
		if (EntitySearcher.isInverseFunctional(oPropExp, ontology))	isInverseFunctional = true;
		if (pbox.vclass.graph.chainPropertiesMap!=null)
			propertyChainAxiom = pbox.vclass.graph.chainPropertiesMap.get(getKey (oPropExp));
	}
	
	public void add(VisObjectProperty pparent){
		parents.add(pparent);
		if (parentConnectors == null){
			parentConnectors = new ArrayList<>();
			parentConnectors.add(new VisConnectorHeritance(this, pparent));
		}
		parentConnectors.add(new VisConnectorHeritance(this, pparent));

	}
	
	public int getLabelHeight() {
		if (height ==0) {
			Font font = Font.font("Arial", FontWeight.NORMAL, 9);
			height = VisProperty.stringHeight(font, getDomain().graph.paintframe.getGraphicsContext2D()) + 8;
		}	
		return height;
	}
	
	public int getLabelWidth(){
		if (width ==0){
			Font font = Font.font("Arial", FontWeight.NORMAL, 11);
			width = VisProperty.stringWidth(label, font, getDomain().graph.paintframe.getGraphicsContext2D());
		}
		return width;
	}
	
	public static String getKey (OWLObjectPropertyExpression e){
		if (e instanceof OWLObjectProperty) {
//			return e.asOWLObjectProperty().getIRI().getFragment();
			return e.asOWLObjectProperty().getIRI().toString();
		}
		else 
			return e.toString();
	}
	
	public static boolean contains(ArrayList<VisObjectProperty>list, OWLObjectProperty prop){
		for (VisObjectProperty item : list){
			if (item.oPropExp.toString().equals(prop.toString()))
				return true;				
		}
		return false;
	}
	
	public VisClass getDomain( ) {
		return pbox.vclass;
	}

	public void draw(GraphicsContext g){
		if (g == null){
			return;
		}
		if ((pbox.visible)&&(visible)&&(pbox.vclass.visible)){
			g.setFont(textFont);
			g.setFill(Color.BLACK);

			if ((parents!=null)&&(!parents.isEmpty())) {
				g.fillText(visibleLabel, getPosX(), getPosY());
			}	
			else {
				g.fillText(visibleLabel, getPosX(), getPosY());
			}
			Point2D circlePos = new Point2D(getPosX()-17, getPosY()-11);
			if (isTransitive|| isFunctional || isSymmetric || hasInverse || isReflexive || propertyChainAxiom!=null){
				Color c = (Color) g.getFill();
				g.strokeOval(circlePos.getX(),circlePos.getY()+2, 10,10);
				g.setFill(Color.BLACK);
				g.setStroke(Color.LIGHTGRAY);
				g.fillOval(circlePos.getX(),circlePos.getY()+2, 10,10);
				g.setFill(c);
				g.setStroke(c);
			}
		}
	}

	public Point2D getConnectionPoint(int index) {
		if (connectionPoints ==null)
			getConnectionPoints();
		return connectionPoints.get(index);
	}
	
	public ArrayList<Point2D> getConnectionPoints(){
		if (connectionPoints.size()==0) {
			addPoints(connectionPoints);
		}
		return connectionPoints;
	}

	private void addPoints(ArrayList<Point2D> list ) {
		list.add(new Point2D(getPosX(),getPosY()));
		list.add(new Point2D(getPosX()+getLabelWidth()+2,getPosY()));
		list.add(new Point2D(getPosX()+ (double) getLabelWidth() /2,getPosY()-getLabelHeight()));
		list.add(new Point2D(getPosX()+ (double) getLabelWidth() /2,getPosY()+getLabelHeight()));
	}

	public void drawConnectors(GraphicsContext g) {
		if (g == null){
			return;
		}
		if (visible) {
			if (rangeConnector != null)
				rangeConnector.draw(g);
			if (parents.size()>1){
				g.setFont(circleFont);
				if (pbox.vclass.visible){
					g.strokeOval(getPosX()-17, getPosY()-14, 14,14);
					g.fillText(OntViewConstants.AND, getPosX()-14, getPosY()-2);
				}
				for (VisConnectorHeritance con : parentConnectors) {
					g.setFill(Color.BLACK);
					g.setStroke(Color.BLACK);
					con.draw(g);
				}	
			}
			else if (parents.size()==1){
				if (onlyParentConnector==null){
			    	onlyParentConnector = new VisConnectorPropProp(this, parents.get(0));
			    }
				onlyParentConnector.draw(g);
			}
		   g.setFont(textFont); 	
		}
	}


	public static void addDomain(VisGraph v, NodeSet<OWLClass> propertyDomainNodeSet,
                                 OWLObjectProperty property, OWLReasoner reasoner, OWLOntology ontology,
                                 Shape range){
		// Since property domain returned more than one class, this will have
		// to create  a new class as the intersection of all of them

		OWLDataFactory dFactory = OWLManager.getOWLDataFactory();
		HashSet<OWLClassExpression> terms = new HashSet<OWLClassExpression>();

		for ( org.semanticweb.owlapi.reasoner.Node<OWLClass> node : propertyDomainNodeSet.getNodes()){
			for ( OWLClass entity :node.getEntities()){
				terms.add(entity);
			}
		}
		OWLObjectIntersectionOf result = dFactory.getOWLObjectIntersectionOf(terms);
		VisLevel l = VisLevel.getLevelFromID(v.levelSet,1);
		VisClass intersection = new VisClass(1, result, ExpressionManager.getReducedClassExpression(result), v);

		l.addShape(intersection);
		v.shapeMap.put(result.toString(), intersection);
		intersection.isAnonymous = true;
		intersection.setHeight(intersection.calculateHeight());
		intersection.setWidth(intersection.calculateWidth());
		intersection.setVisLevel(l);
		for (OWLClassExpression term : terms){
			Shape sup = v.lookUpOrCreate(term);
			VisConnectorIsA con = new VisConnectorIsA(sup,intersection);
			v.connectorList.add(con);
			intersection.inConnectors.add(con);
			sup.outConnectors.add(con);
		}
		
		intersection.properties.add(property.getIRI().getFragment());
		if (intersection.getPropertyBox() == null) {
			intersection.createPropertyBox();
		}	
	    intersection.getPropertyBox().add(property,range,ontology);	
		
	}
	
	/**
	 * Esta en wip
	 * @param v
	 * @param propertyRangeNodeSet
	 * @param property
	 * @param reasoner
	 * @param ontology
	 */
	
	
	public static Shape addRange(VisGraph v, NodeSet<OWLClass> propertyRangeNodeSet,
                                 OWLObjectProperty property, OWLReasoner reasoner, OWLOntology ontology){
		// Since property range returned more than one class, this will have
		// to create  a new class as the intersection of all of them
		
		OWLDataFactory dFactory = OWLManager.getOWLDataFactory();
		HashSet<OWLClassExpression> terms = new HashSet<OWLClassExpression>();
		
		for ( org.semanticweb.owlapi.reasoner.Node<OWLClass> node : propertyRangeNodeSet.getNodes()){
			for ( OWLClass entity :node.getEntities()){
			terms.add(entity);
			}
		}
		OWLObjectIntersectionOf result = dFactory.getOWLObjectIntersectionOf(terms);
		VisLevel l = VisLevel.getLevelFromID(v.levelSet,1);
		VisClass intersection = new VisClass(1, result, ExpressionManager.getReducedClassExpression(result), v);

		l.addShape(intersection);
		v.shapeMap.put(result.toString(), intersection);
		intersection.isAnonymous = true;
		intersection.setHeight(intersection.calculateHeight());
		intersection.setWidth(intersection.calculateWidth());
		intersection.setVisLevel(l);
		for (OWLClassExpression term : terms){
		Shape sup = v.lookUpOrCreate(term);
		VisConnectorIsA con = new VisConnectorIsA(sup,intersection);
		v.connectorList.add(con);
		intersection.inConnectors.add(con);
		sup.outConnectors.add(con);
		}
		// CBL: the range should not be added to the new shape 
		// it is only the new Shape that is connected
//		
//		intersection.properties.add(property.getIRI().getFragment());
		if (intersection.getPropertyBox() == null) {
			intersection.createPropertyBox();
		}	
//		intersection.getPropertyBox().add(property,range,ontology);	
		
		return intersection; 

}
	
	
	

	public boolean subsumed( ArrayList<VisObjectProperty> list){
	/*
	 * is current property expression is subsumed by others in the list ?
	 */
		OWLDataFactory dFactory = OWLManager.getOWLDataFactory();
		OWLReasoner reasoner = getReasoner();
		for (VisObjectProperty prop : list){
			if (this != prop){
				if (reasoner.isEntailed(dFactory.getOWLSubObjectPropertyOfAxiom(this.oPropExp,prop.oPropExp)))
					return true;
			}
		}
		return false;
	}
	
	public void swapLabel(Boolean qualifiedRendering){
		
		// this is needed for the getTooltipInfo method of the different 
		// elements: as this info is refreshed at a different pace from the 
		// global view refreshment, these methods have to be aware of the type of 
		// rendering that is being used (labelled, qualified). 
		this.qualifiedRendering = qualifiedRendering;
		
		if (qualifiedRendering) 
			visibleLabel = qualifiedLabel; 
		else 
			visibleLabel = label; 
		
	}
}
	

