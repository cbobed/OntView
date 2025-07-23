package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Font;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.search.EntitySearcher;
import sid.OntView2.utils.ExpressionManager;

import java.util.*;

import static sid.OntView2.utils.ExpressionManager.qualifyLabel;
import static sid.OntView2.utils.ExpressionManager.replaceString;

public class VisDataProperty extends VisProperty {

	VisPropertyBox pBox;
	int height,width;
	int vOffset;
	OWLDataPropertyExpression dPropExp;
	String label;
	String qualifiedLabel;
    Set<String> explicitLabel = new HashSet<>();
    Set<String> explicitQualifiedLabel = new HashSet<>();
    String visibleLabel;
	String range;
	VisConnectorPropProp parent;
	ArrayList<Point2D> connectionPoints;
	boolean visible = true;
	ArrayList<VisObjectProperty> parents;
	Font textFont;
	Font circleFont;
	boolean isFunctional  =  false;
	boolean qualifiedRendering = false, labelRendering = false, isKorean = false;
	public int getPosX(){return getDomain().getLeftCorner() + 2;}
	public int getPosY(){return 15 + getDomain().getBottomShapeCorner() + getLabelHeight() * vOffset;}

    public VisDataProperty( VisPropertyBox ppbox, OWLDataPropertyExpression dExp, int pvoffset,String prange,OWLOntology ontology) {
		pBox = ppbox;
		label = ExpressionManager.getReducedDataPropertyExpression(dExp);
		qualifiedLabel = ExpressionManager.getReducedQualifiedDataPropertyExpression(dExp);

        OWLDataProperty namedProp = dExp.asOWLDataProperty();
        List<OWLAnnotation> annotations = EntitySearcher.getAnnotations(namedProp, ontology).toList();
        for (OWLAnnotation ann : annotations) {
            if (ann.getProperty().isLabel()) {
                String auxLabel = replaceString(ann.getValue().toString());
                explicitLabel.add(auxLabel);
                String auxQLabel = qualifyLabel(namedProp, auxLabel);
                explicitQualifiedLabel.add(!"null".equalsIgnoreCase(auxQLabel) ? auxQLabel : auxLabel);
            }
        }
        if (explicitLabel.isEmpty()) explicitLabel.add(label);

		if (qualifiedLabel == null || "null".equalsIgnoreCase(qualifiedLabel)) {
			qualifiedLabel = label; 
		}
		
		visibleLabel = label; 
		dPropExp = dExp;
		range = prange;
		vOffset = pvoffset;
		connectionPoints = new ArrayList<>();

		textFont = Font.font("DejaVu Sans", FontWeight.NORMAL, 10);
		circleFont  = Font.font("DejaVu Sans", FontWeight.BOLD, 10);
		connectionPoints = new ArrayList<>();

		if (EntitySearcher.isFunctional(dPropExp.asOWLDataProperty(), ontology)) isFunctional = true;
	}
	
	public void add(VisConnectorPropProp pParent){
		parent = pParent;
	}

	public int getLabelHeight() {
		if (height ==0) {
            int spaceBetweenLines = 8;
            Font font = Font.font("DejaVu Sans", FontWeight.NORMAL, 11);
			height = VisProperty.stringHeight(font) + spaceBetweenLines;
		}
		return height;
	}
	
	public int getLabelWidth(){
		if (width ==0){
			Font font = Font.font("DejaVu Sans", FontWeight.NORMAL, 9);
			width = VisProperty.stringWidth(label+": "+range, font);
		}
		return width;
	}
	
	public static String getKey(OWLDataPropertyExpression e){
		if (e instanceof OWLDataProperty) {
			return e.asOWLDataProperty().getIRI().toString();
		}
		else 
			return e.toString();
	}
	
	public static boolean contains(ArrayList<VisDataProperty>list, OWLDataProperty dProp){
		for (VisDataProperty item : list){
			if (item.dPropExp.toString().equals(dProp.toString()))
				return true;				
		}
		return false;
	}

	public VisClass getDomain() {
		return pBox.vClass;
	}

	public void draw(GraphicsContext g){
		if (g == null){
			return;
		}
		if ((pBox.visible)&&(visible)&&(pBox.vClass.visible)){
			g.setFont(textFont);
			if ((parents!=null)&&(!parents.isEmpty())) {
				g.fillText(label, getPosX(), getPosY());
			}	
			else {
				g.fillText(visibleLabel+ " : " + range, getPosX(), getPosY());
			}
			Point2D circlePos = new Point2D(getPosX()-16, getPosY()-10);
			if (isFunctional){
				g.setFont(circleFont);
                g.strokeOval(circlePos.getX(), circlePos.getY() + 1.5, 10, 10);
                g.fillText("1", circlePos.getX() + 2.3, circlePos.getY() + 10,5);
				g.setFont(textFont);
			}
		}
		
		
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

			if (getDomain().getVisibleDefinitionLabels() != null && !getDomain().getVisibleDefinitionLabels().isEmpty()) {
				for (String defLabel : getDomain().getVisibleDefinitionLabels()) {
					description.append("<b>Domain:</b> ").append(defLabel).append("<br>");
				}
			} else {
				description.append("<b>Domain:</b> ").append(getDomain().visibleLabel).append("<br>");
            }

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

    public void swapLabel(Boolean labelRendering, Boolean qualifiedRendering, String language) {
        // this is needed for the getTooltipInfo method of the different
        // elements: as this info is refreshed at a different pace from the
        // global view refreshment, these methods have to be aware of the type of
        // rendering that is being used (labelled, qualified).
        this.qualifiedRendering = qualifiedRendering;
        this.labelRendering = labelRendering;
        this.isKorean = language.equals("ko");

        if (labelRendering){
            if (qualifiedRendering) {
                Optional<String> candidate = explicitQualifiedLabel.stream()
                    .filter(s -> s.contains("@" + language))
                    .findFirst();
                candidate.ifPresent(s -> visibleLabel = s);
            }
            else {
                Optional<String> candidate = explicitLabel.stream()
                    .filter(s -> s.contains("@" + language))
                    .findFirst();
                candidate.ifPresent(s -> visibleLabel = s);
            }
        }
        else {
            if (qualifiedRendering) {
                visibleLabel = qualifiedLabel;
            } else {
                visibleLabel = label;
            }
        }
    }
	
	// <CBL 25/9/13> 
	// method added to handle the dataProperties in the same way as ObjectProperties
	public static void addDomain(VisGraph v, NodeSet<OWLClass> propertyDomainNodeSet,
                                 OWLDataProperty property, OWLOntology ontology,
                                 String range){
		// Since property domain returned more than one class, this will have
		// to create  a new class as the intersection of all of them
		OWLDataFactory dFactory = OWLManager.getOWLDataFactory();
		HashSet<OWLClassExpression> terms = new HashSet<>();

		for ( org.semanticweb.owlapi.reasoner.Node<OWLClass> node : propertyDomainNodeSet.getNodes()){
            terms.addAll(node.getEntities());
		}
		OWLObjectIntersectionOf result = dFactory.getOWLObjectIntersectionOf(terms);
		VisLevel l = VisLevel.getLevelFromID(v.levelSet,1);
		VisClass intersection = new VisClass(1, result, ExpressionManager.getReducedClassExpression(result), v);

		l.addShape(intersection);
		v.shapeMap.put(Shape.getKey(result), intersection);
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
