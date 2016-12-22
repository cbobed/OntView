package sid.OntView.common;

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
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import sid.OntView.utils.ExpressionManager;

public class VisDataProperty extends VisProperty {

	VisPropertyBox pbox;
	int height,width;
	int voffset;
	OWLDataPropertyExpression dPropExp;
	String label;
	String qualifiedLabel = ""; 
	String visibleLabel = ""; 
	String range;
	VisConnectorPropertyRange rangeConnector;
	HashSet<Point> pointSet;
	VisConnectorPropProp parent;
	ArrayList<Point> connectionPoints;
	
	boolean visible = true;
	ArrayList<VisConnectorHeritance> parentConnectors;
	ArrayList<VisObjectProperty> parents;
	VisConnectorPropProp onlyParentConnector;
	
	Font textFont;
	Font circleFont;
		
	boolean isFunctional  =  false;
	
	String  description  =  "";
	
	boolean qualifiedRendering = false; 
	boolean labelRendering = false; 

	public int getPosX(){return getDomain().getPosX()-(getDomain().getWidth()/2)+2;}
	public int getPosY(){return 10+getDomain().getPosY()+(getDomain().getHeight())+getLabelHeight()*voffset;}
	
	
	public VisDataProperty( VisPropertyBox ppbox, OWLDataPropertyExpression  dexp,int pvoffset,String prange,OWLOntology ontology) {
		pbox = ppbox;
		label = ExpressionManager.getReducedDataPropertyExpression(dexp);
		qualifiedLabel = ExpressionManager.getReducedQualifiedDataPropertyExpression(dexp); 
		
		if (qualifiedLabel == null || "null".equalsIgnoreCase(qualifiedLabel)) {
			qualifiedLabel = label; 
		}
		
		visibleLabel = label; 
		dPropExp = dexp;
		range = prange;
		voffset = pvoffset;
		connectionPoints = new ArrayList<Point>();
		
		textFont    = new Font(Font.DIALOG,Font.PLAIN,10);
		circleFont  = new Font(Font.DIALOG,Font.BOLD, 10);
		connectionPoints = new ArrayList<Point>();
		if (dPropExp.isFunctional(ontology)) isFunctional = true;
	
	}
	
	public void add(VisConnectorPropProp pparent){
		parent = pparent;
	}
	
	public int getLabelHeight() {
		if (height ==0) {
			height = VisProperty.stringHeight(new Font(Font.DIALOG,Font.PLAIN,9), getDomain().graph.paintframe.getGraphics())+8;
		}	
		return height;
	}
	
	public int getLabelWidth(){
		if (width ==0){
			width = VisProperty.stringWidth(label+": "+range,new Font(Font.DIALOG,Font.PLAIN,9),getDomain().graph.paintframe.getGraphics());
		}
		return width;
	}
	
	public static String getKey(OWLDataPropertyExpression e){
		if (e instanceof OWLDataProperty) {
//			return e.asOWLDataProperty().getIRI().getFragment();
			return e.asOWLDataProperty().getIRI().toString();
		}
		else 
			return e.toString();
	}
	
	public static boolean contains(ArrayList<VisDataProperty>list, OWLDataProperty dprop){
		for (VisDataProperty item : list){
			if (item.dPropExp.toString().equals(dprop.toString()))
				return true;				
		}
		return false;
	}
	
	public VisClass getDomain( ) {
		return pbox.vclass;
	}

	public void draw(Graphics g){
		if ((pbox.visible)&&(visible)&&(pbox.vclass.visible)){
			g.setFont(textFont);
			if ((parents!=null)&&(parents.size() > 0)) {
				g.drawString(label, getPosX(), getPosY());
			}	
			else {
				g.drawString(label+ " : " + range, getPosX(), getPosY());
			}
			Point circlePos = new Point(getPosX()-16, getPosY()-10);
			if (isFunctional){
				g.setFont(circleFont);
				g.drawOval(circlePos.x,circlePos.y, 10,10);			
				g.drawString("+", circlePos.x+1, getPosY()-1);
				g.setFont(textFont);
			}
		}
		
		
	}
	
	public Point getClosePoint(){
		return new Point(getPosX()+pbox.getMaxWidth()+15,getPosY()-5);
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
	}
	
	public boolean onProperty(Point p){
		return ((p.x >= getPosX()-20)&&(p.x <= getPosX())&& (p.y >= getPosY()-10)&&(p.y <= getPosY()));
	}
	
	
	
	public String getTooltipText(){
	
		String description ="";
		if (description.equals("")){
			description +="<html><b>"+(qualifiedRendering?
										ExpressionManager.getReducedQualifiedDataPropertyExpression(dPropExp):
										ExpressionManager.getReducedDataPropertyExpression(dPropExp))+"</b><br><br>";
			if ((parents != null)&&(parents.size()>1)){
				description += "subclass of<ul>";

				description+="</ul>";
			}
			description +="<b>Domain:</b> "+getDomain().visibleLabel+"<br>";
			description +="<b>Range     : </b>"+range+"<br><br>";
			description +="<b>Property Description</b><br><ul>";
			if (isFunctional) {
					description +="<li>Functional</li>";	
			}	
			description += "</ul></html>";
		}
		return description;
		
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
	
	
	// <CBL 25/9/13> 
	// method added to handle the dataProperties in the same way as ObjectProperties
	
	public static void addDomain(VisGraph v, NodeSet<OWLClass> propertyDomainNodeSet,
			OWLDataProperty property,OWLReasoner reasoner,OWLOntology ontology,
			String range){
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
	
}
