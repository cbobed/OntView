package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import sid.OntView.utils.ExpressionManager;

import javafx.scene.text.Font;
import javafx.scene.paint.Color;
import javafx.geometry.Point2D;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;

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
	
	String  description  =  "";
	public int getPosX(){return getDomain().getPosX()-(getDomain().getWidth()/2)+2;}
	public int getPosY(){return 10+getDomain().getPosY()+(getDomain().getHeight())+getLabelHeight()*voffset;}
	private OWLReasoner getReasoner(){return pbox.vclass.graph.paintframe.getReasoner();}
	
	public boolean onProperty(Point2D p){
		return ((p.getX() >= getPosX()-20)&&(p.getX() <= getPosX())&& (p.getY() >= getPosY()-10)&&(p.getY() <= getPosY()));
	}
	
	public String getTooltipText(){
	
		String description ="";
		if (description.equals("")){
			description +="<html><b>"+visibleLabel+"</b><br><br>";
			if ((parents != null)&&(parents.size()>1)){
				description += "<b>Subproperty of</b><ul>";
				for (VisObjectProperty p : parents){
					description += "<li>"+p.visibleLabel+" </li>";
				}
				description+="</ul>";
			}
			description +="<b>Domain:</b> "+getDomain().visibleLabel+"<br>";
			description +="<b>Range     : </b>"; 
			description += (qualifiedRendering?
								ExpressionManager.getReducedQualifiedClassExpression(range.getLinkedClassExpression()):
								ExpressionManager.getReducedClassExpression(range.getLinkedClassExpression()))+"<br><br>";
			description += "<b>Property Description</b><br>";

			if (isTransitive)
				description +="<li>Transitive</li>";	
			if (isFunctional)
				description +="<li>Functional</li>";	
			if (isReflexive)
				description +="<li>Reflexive</li>";	
			if (isSymmetric)
				description +="<li>Symmetric</li>";
			if (hasInverse){
				description+="<li> inverse of <b>";
				for (OWLObjectPropertyExpression inv : inverseOf.getEntitiesMinusTop()){
					description+= (qualifiedRendering?
										ExpressionManager.getReducedQualifiedObjectPropertyExpression(inv):
										ExpressionManager.getReducedObjectPropertyExpression (inv))+" ";
				}
				description+="</b></li>";
			}
			if (propertyChainAxiom != null) {
				description+= "<b>Chain Property</b><ul>";
				for (OWLObjectPropertyExpression c: propertyChainAxiom.getPropertyChain()){
					description+="<li>"+(qualifiedRendering?
											ExpressionManager.getReducedQualifiedObjectPropertyExpression(c):
											ExpressionManager.getReducedObjectPropertyExpression(c))+"</li>";
				}
				description+="</ul>";
			}
			
			description += "</html>";
		}
		return description;
		
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

		textFont    = Font.font("Dialog", FontWeight.NORMAL, FontPosture.REGULAR, 12);
		circleFont  = Font.font("Dialog", FontWeight.BOLD, 10);

		connectionPoints = new ArrayList<Point2D>();
		if (getDomain() != range) {
			rangeConnector = new VisConnectorPropertyRange(ppbox, pbox.vclass , range, this);
		}	
		if (oPropExp.isFunctional(ontology)) isFunctional = true;
		if (oPropExp.isTransitive(ontology)) isTransitive = true;
		if (oPropExp.isSymmetric(ontology )) isSymmetric  = true;
		inverseOf  = getReasoner().getInverseObjectProperties(po);
		hasInverse = ((inverseOf !=null)&&(inverseOf.getEntities().size())>0);
		if (oPropExp.isReflexive(ontology)) isReflexive =true;
		if (oPropExp.isAsymmetric(ontology)) isAsymmetric = true;
		if (oPropExp.isIrreflexive(ontology)) isIrreflexive = true;
		if (oPropExp.isInverseFunctional(ontology))	isInverseFunctional = true;
		if (pbox.vclass.graph.chainPropertiesMap!=null)
			propertyChainAxiom = pbox.vclass.graph.chainPropertiesMap.get(getKey (oPropExp));
	}
	
	public void add(VisObjectProperty pparent){
		parents.add(pparent);
		if (parentConnectors == null){
			parentConnectors = new ArrayList<VisConnectorHeritance>();
			parentConnectors.add(new VisConnectorHeritance(this, pparent));
		}
		parentConnectors.add(new VisConnectorHeritance(this, pparent));

	}
	
	public int getLabelHeight() {
		if (height ==0) {
			Font font = Font.font("Dialog", FontWeight.NORMAL, 9);
			height = VisProperty.stringHeight(font, getDomain().graph.paintframe.getGraphicsContext2D()) + 8;
		}	
		return height;
	}
	
	public int getLabelWidth(){
		if (width ==0){
			Font font = Font.font("Dialog", FontWeight.NORMAL, 9);
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
		Point2D p = getClosePoint();
		if ((pbox.visible)&&(visible)&&(pbox.vclass.visible)){
			g.setFont(textFont);
			if ((parents!=null)&&(parents.size() > 0)) {
				g.fillText(visibleLabel, getPosX(), getPosY());
			}	
			else {
				g.fillText(visibleLabel, getPosX(), getPosY());
			}
			Point2D circlePos = new Point2D(getPosX()-17, getPosY()-11);
			if (isTransitive|| isFunctional || isSymmetric || hasInverse || isReflexive || propertyChainAxiom!=null){
				Color c = (Color) g.getFill();
				g.strokeOval(circlePos.getX(),circlePos.getY()+2, 9,9);
				g.setFill(Color.LIGHTGRAY);
				g.fillOval(circlePos.getX(),circlePos.getY()+2, 9,9);
				g.setFill(c);
			}
		}
	}
	
	public Point2D getClosePoint(){
		return new Point2D(getPosX()+pbox.getMaxWidth()+15,getPosY()-5);
	}
	public Point2D getOvalCoord(){
		return new Point2D(getPosX()+pbox.getMaxWidth()+25,getPosY()-5);
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
	

