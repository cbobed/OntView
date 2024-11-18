package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Font;

import org.apache.jena.base.Sys;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import sid.OntView2.utils.ExpressionManager;

import java.util.ArrayList;
import java.util.HashSet;

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
	HashSet<Point2D> pointSet;
	VisConnectorPropProp parent;
	ArrayList<Point2D> connectionPoints;
	
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
	public int getPosY(){return getDomain().getPosY()+(getDomain().getHeight())+getLabelHeight()*voffset;}
	
	
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
		connectionPoints = new ArrayList<Point2D>();

		textFont = Font.font("Arial", FontWeight.NORMAL, 10);
		circleFont  = Font.font("Arial", FontWeight.BOLD, 10);
		connectionPoints = new ArrayList<>();

		if (EntitySearcher.isFunctional(dPropExp.asOWLDataProperty(), ontology)) isFunctional = true;

	}
	
	public void add(VisConnectorPropProp pparent){
		parent = pparent;
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
			Font font = Font.font("Arial", FontWeight.NORMAL, 9);
			width = VisProperty.stringWidth(label+": "+range, font, getDomain().graph.paintframe.getGraphicsContext2D());
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

	public void draw(GraphicsContext g){
		if (g == null){
			return;
		}
		if ((pbox.visible)&&(visible)&&(pbox.vclass.visible)){
			g.setFont(textFont);
			if ((parents!=null)&&(!parents.isEmpty())) {
				g.fillText(label, getPosX(), getPosY());
			}	
			else {
				g.fillText(label+ " : " + range, getPosX(), getPosY());
			}
			Point2D circlePos = new Point2D(getPosX()-16, getPosY()-10);
			if (isFunctional){
				g.setFont(circleFont);
				g.fillOval(circlePos.getX(),circlePos.getY()+5, 5,5);
				//g.fillText("+", circlePos.getX()+1, getPosY()-1);
				g.setFont(textFont);
			}
		}
		
		
	}
	
	public Point2D getClosePoint(){
		return new Point2D(getPosX()+pbox.getMaxWidth()+15,getPosY()-5);
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
		list.add(new Point2D(getPosX()+getLabelWidth()/2,getPosY()-getLabelHeight()));
		list.add(new Point2D(getPosX()+getLabelWidth()/2,getPosY()+getLabelHeight()));
	}
	public void drawConnectors(GraphicsContext g) {
	}
	
	public boolean onProperty(Point2D p){
		return ((p.getX() >= getPosX()-20)&&(p.getX() <= getPosX())&& (p.getY() >= getPosY()-10)&&(p.getY() <= getPosY()));
	}

	public String getTooltipText(){
		StringBuilder description = new StringBuilder();
		if (description.isEmpty()){
			description.append("<html><b>").append(qualifiedRendering ?
                    ExpressionManager.getReducedQualifiedDataPropertyExpression(dPropExp) :
                    ExpressionManager.getReducedDataPropertyExpression(dPropExp)).append("</b><br><br>");
			if ((parents != null)&&(parents.size()>1)){
				description.append("subclass of<ul>");

				description.append("</ul>");
			}

			if (!getDomain().getVisibleDefinitionLabels().isEmpty()) {
				for (String defLabel : getDomain().getVisibleDefinitionLabels()) {
					description.append("<b>Domain:</b> ").append(defLabel).append("<br>");
				}
			} else {
				description.append("<b>Domain:</b> ").append(getDomain().visibleLabel).append("<br>");			}
			description.append("<b>Domain:</b> ").append(getDomain().visibleLabel).append("<br>");
			description.append("<b>Range     : </b>").append(range).append("<br><br>");
			description.append("<b>Property Description</b><br><ul>");
			if (isFunctional) {
					description.append("<li>Functional</li>");
			}
			description.append("</ul></html>");
		}
		return description.toString();
		
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
                                 OWLDataProperty property, OWLReasoner reasoner, OWLOntology ontology,
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
