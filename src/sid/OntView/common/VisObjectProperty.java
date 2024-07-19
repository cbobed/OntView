package sid.OntView.common;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.search.EntitySearcher;

import sid.OntView.utils.ExpressionManager;

public class VisObjectProperty extends VisProperty  {

	VisPropertyBox pbox;
	int height,width;
	int voffset;
	OWLObjectPropertyExpression oPropExp;
	String label;
	String qualifiedLabel = "";
	String visibleLabel = "";
	Shape  range;
	boolean visible = true;
	VisConnectorPropertyRange rangeConnector;
	ArrayList<VisConnectorHeritance> parentConnectors;
	ArrayList<VisObjectProperty> parents;
	HashSet<Point> pointSet;
	VisConnectorPropProp onlyParentConnector;
	ArrayList<Point> connectionPoints;
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
	
	public boolean onProperty(Point p){
		return ((p.x >= getPosX()-20)&&(p.x <= getPosX())&& (p.y >= getPosY()-10)&&(p.y <= getPosY()));
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
	
	
	
	public VisObjectProperty(VisPropertyBox ppbox, OWLObjectPropertyExpression po,int pvoffset,Shape prange,OWLOntology ontology) {
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
		textFont    = new Font(Font.DIALOG,Font.PLAIN,10);
		circleFont  = new Font(Font.DIALOG,Font.BOLD, 10);
		connectionPoints = new ArrayList<Point>();
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
			parentConnectors = new ArrayList<VisConnectorHeritance>();
			parentConnectors.add(new VisConnectorHeritance(this, pparent));
		}
		parentConnectors.add(new VisConnectorHeritance(this, pparent));

	}
	
	public int getLabelHeight() {
		if (height == 0) {
			height = VisProperty.stringHeight(new Font(Font.DIALOG,Font.PLAIN,9), getDomain().graph.paintframe.getGraphics())+8;
		}	
		return height;
	}
	
	public int getLabelWidth(){
		if (width == 0){
			width = VisProperty.stringWidth(label,new Font(Font.DIALOG,Font.PLAIN,9),getDomain().graph.paintframe.getGraphics());
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

	public void draw(Graphics g){
		Point p = getClosePoint();
		if ((pbox.visible)&&(visible)&&(pbox.vclass.visible)){
			g.setFont(textFont);
			if ((parents!=null)&&(parents.size() > 0)) {
				g.drawString(visibleLabel, getPosX(), getPosY());
			}	
			else {
				g.drawString(visibleLabel, getPosX(), getPosY());
			}
			Point circlePos = new Point(getPosX()-17, getPosY()-11);
			if (isTransitive|| isFunctional || isSymmetric || hasInverse || isReflexive || propertyChainAxiom!=null){
				Color c = g.getColor();
				g.drawOval(circlePos.x,circlePos.y+2, 9,9);
				g.setColor(Color.lightGray);
				g.fillOval(circlePos.x,circlePos.y+2, 9,9);
				g.setColor(c);
			}
		}
	}
	
	public Point getClosePoint(){
		return new Point(getPosX()+pbox.getMaxWidth()+15,getPosY()-5);
	}
	public Point getOvalCoord(){
		return new Point(getPosX()+pbox.getMaxWidth()+25,getPosY()-5);
	}

	public Point getConnectionPoint(int index) {
		if (connectionPoints ==null)
			getConnectionPoints();
		return connectionPoints.get(index);
	}
	
	public ArrayList<Point> getConnectionPoints(){
		if (connectionPoints.size()==0) {
			addPoints(connectionPoints);
		}
		return connectionPoints;
	}

	private void addPoints(ArrayList<Point> list ) {
		list.add(new Point(getPosX(),getPosY()));
		list.add(new Point(getPosX()+getLabelWidth()+2,getPosY()));
		list.add(new Point(getPosX()+getLabelWidth()/2,getPosY()-getLabelHeight()));
		list.add(new Point(getPosX()+getLabelWidth()/2,getPosY()+getLabelHeight()));
	}

	public void drawConnectors(Graphics g) {
		if (visible) {
			if (rangeConnector != null)
				rangeConnector.draw(g);
			if (parents.size()>1){
				g.setFont(circleFont);
				if (pbox.vclass.visible){
					g.drawOval(getPosX()-17, getPosY()-14, 14,14);
					g.drawString(OntViewConstants.AND, getPosX()-14, getPosY()-2);
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
								OWLObjectProperty property,OWLReasoner reasoner,OWLOntology ontology,
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
			OWLObjectProperty property,OWLReasoner reasoner,OWLOntology ontology){
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
	

