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

import java.util.*;
import java.util.stream.Collectors;

import static sid.OntView2.utils.ExpressionManager.qualifyLabel;
import static sid.OntView2.utils.ExpressionManager.replaceString;

public class VisObjectProperty extends VisProperty {

	VisPropertyBox pBox;
	int height,width;
	int vOffset;
	OWLObjectPropertyExpression oPropExp;
	String label;
	String qualifiedLabel;
    Set<String> explicitLabel = new HashSet<>();
    Set<String> explicitQualifiedLabel = new HashSet<>();
	String visibleLabel;
	Shape range;
	boolean visible = true;
	VisConnectorPropertyRange rangeConnector;
	ArrayList<VisConnectorHeritance> parentConnectors;
	ArrayList<VisObjectProperty> parents;
	VisConnectorPropProp onlyParentConnector;
	ArrayList<Point2D> connectionPoints;
	Font textFont, circleFont, koreanFont;
	boolean qualifiedRendering = false, labelRendering = false, isKorean = false;
	org.semanticweb.owlapi.reasoner.Node<OWLObjectPropertyExpression> inverseOf;
	boolean isTransitive  =  false;
	boolean isSymmetric   =  false;
	boolean isReflexive   =  false;
	boolean isFunctional  =  false;
	boolean isAsymmetric  =  false;
	boolean hasInverse    =  false;
	boolean isIrreflexive=  false;
	boolean isInverseFunctional = false;
	OWLSubPropertyChainOfAxiom propertyChainAxiom = null;
	public int getPosX(){return getDomain().getLeftCorner(); }
    public int getPosY(){return 15 + getDomain().getBottomShapeCorner() + getLabelHeight() * vOffset; }

	public OWLReasoner getReasoner(){return getDomain().graph.paintframe.getReasoner();}
	
	public boolean onProperty(Point2D p){
		return ((p.getX() >= getPosX()-20)&&(p.getX() <= getPosX())&& (p.getY() >= getPosY()-10)&&(p.getY() <= getPosY()));
	}

    public String getTooltipText() {
        StringBuilder description = new StringBuilder();
        description.append(visibleLabel).append("\n\n");

        if (parents != null && parents.size() > 1) {
            description.append("Subproperty of:\n");
            for (VisObjectProperty p : parents) {
                String item = "- " + p.visibleLabel;
                if (!description.toString().contains(item)) {
                    description.append(item).append("\n");
                }
            }
            description.append("\n");
        }

        if (getDomain().getVisibleDefinitionLabels() != null && !getDomain().getVisibleDefinitionLabels().isEmpty()) {
            for (String defLabel : getDomain().getVisibleDefinitionLabels()) {
                description.append("Domain: ").append(defLabel).append("\n");
            }
        } else {
            description.append("Domain: ").append(getDomain().visibleLabel).append("\n");
        }

        String rangeText = qualifiedRendering
            ? ExpressionManager.getReducedQualifiedClassExpression(range.getLinkedClassExpression())
            : ExpressionManager.getReducedClassExpression(range.getLinkedClassExpression());
        description.append("Range: ").append(rangeText).append("\n\n");

        description.append("Property Description:\n");
        if (isTransitive)   description.append("- Transitive\n");
        if (isFunctional)   description.append("- Functional\n");
        if (isReflexive)    description.append("- Reflexive\n");
        if (isSymmetric)    description.append("- Symmetric\n");

        if (hasInverse) {
            StringBuilder invDesc = new StringBuilder();
            for (OWLObjectPropertyExpression inv : inverseOf.getEntitiesMinusTop()) {
                if (inv instanceof OWLObjectProperty) {
                    String invText = qualifiedRendering
                        ? ExpressionManager.getReducedQualifiedObjectPropertyExpression(inv)
                        : ExpressionManager.getReducedObjectPropertyExpression(inv);
                    invDesc.append("- ").append(invText).append("\n");
                }
            }
            if (!invDesc.isEmpty()) {
                description.append("Inverse of:\n");
                description.append(invDesc);
                description.append("\n");
            }
        }

        if (propertyChainAxiom != null) {
            description.append("Chain Property:\n");
            for (OWLObjectPropertyExpression c : propertyChainAxiom.getPropertyChain()) {
                String chainText = qualifiedRendering
                    ? ExpressionManager.getReducedQualifiedObjectPropertyExpression(c)
                    : ExpressionManager.getReducedObjectPropertyExpression(c);
                description.append("- ").append(chainText).append("\n");
            }
        }
        return description.toString();
    }
	
	public VisObjectProperty(VisPropertyBox ppbox, OWLObjectPropertyExpression po, int pvoffset, Shape prange, OWLOntology ontology) {
		pBox = ppbox;
		parents  = new ArrayList<>();
		label    = ExpressionManager.getReducedObjectPropertyExpression (po);
		visibleLabel = label;

        String auxQLabel;

        OWLObjectProperty namedProp = po.getNamedProperty();
        List<OWLAnnotation> annotations = EntitySearcher.getAnnotations(namedProp, ontology).toList();
        for (OWLAnnotation ann : annotations) {
            if (ann.getProperty().isLabel()) {
                String auxLabel = replaceString(ann.getValue().toString());
                explicitLabel.add(auxLabel);
                auxQLabel = qualifyLabel(namedProp, auxLabel);
                explicitQualifiedLabel.add(!"null".equalsIgnoreCase(auxQLabel) ? auxQLabel : auxLabel);
            }
        }
        if (explicitLabel.isEmpty()) explicitLabel.add(label);

		qualifiedLabel = ExpressionManager.getReducedQualifiedObjectPropertyExpression(po);
		if (qualifiedLabel == null || "null".equalsIgnoreCase(qualifiedLabel)) {
			qualifiedLabel = label; 
		}
		
		oPropExp = po;
		range    = prange;
		vOffset = pvoffset;

		textFont    = Font.font("DejaVu Sans", FontWeight.NORMAL, FontPosture.REGULAR, 11);
		circleFont  = Font.font("DejaVu Sans", FontWeight.BOLD, 10);
        koreanFont = Font.font("Noto Sans CJK KR", FontWeight.NORMAL, FontPosture.REGULAR, 11);

		connectionPoints = new ArrayList<>();
		if (getDomain() != range) {
			rangeConnector = new VisConnectorPropertyRange(ppbox, getDomain(), range, this);
		}
		if (EntitySearcher.isFunctional(oPropExp, ontology)) isFunctional = true;
		if (EntitySearcher.isTransitive(oPropExp, ontology)) isTransitive = true;
		if (EntitySearcher.isSymmetric(oPropExp, ontology)) isSymmetric  = true;
		inverseOf  = getReasoner().getInverseObjectProperties(po);
		hasInverse = ((inverseOf !=null)&& !inverseOf.getEntities().isEmpty());

        if (EntitySearcher.isReflexive(oPropExp, ontology)) isReflexive =true;
		if (EntitySearcher.isAsymmetric(oPropExp, ontology)) isAsymmetric = true;
		if (EntitySearcher.isIrreflexive(oPropExp, ontology)) isIrreflexive = true;
		if (EntitySearcher.isInverseFunctional(oPropExp, ontology))	isInverseFunctional = true;
		if (getDomain().graph.chainPropertiesMap!=null)
			propertyChainAxiom = getDomain().graph.chainPropertiesMap.get(getKey (oPropExp));
	}
	
	public void add(VisObjectProperty pParent){
		parents.add(pParent);
		if (parentConnectors == null){
			parentConnectors = new ArrayList<>();
			parentConnectors.add(new VisConnectorHeritance(this, pParent));
		}
		parentConnectors.add(new VisConnectorHeritance(this, pParent));
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
			Font font = Font.font("DejaVu Sans", FontWeight.NORMAL, 11);
			width = VisProperty.stringWidth(label, font);
		}
		return width;
	}
	
	public static String getKey (OWLObjectPropertyExpression e){
		if (e instanceof OWLObjectProperty) {
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

	public VisClass getDomain() {
		return pBox.vClass;
	}

	public void draw(GraphicsContext g){
		if (g == null){
			return;
		}
		if ((pBox.visible)&&(visible)&&(getDomain().visible)){
            g.setFont(this.isKorean ? koreanFont : textFont);
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

    public void drawConnectors(GraphicsContext g) {
		if (g == null){
			return;
		}
		if (visible) {
			if (rangeConnector != null)
				rangeConnector.draw(g);
			if (parents.size()>1){
				g.setFont(circleFont);
				if (getDomain().visible){
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
            g.setFont(this.isKorean ? koreanFont : textFont);
		}
	}


	public static void addDomain(VisGraph v, NodeSet<OWLClass> propertyDomainNodeSet,
                                 OWLObjectProperty property, OWLOntology ontology,
                                 Shape range){
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
	
	/**
	 * Esta en wip
	 */
	public static Shape addRange(VisGraph v, NodeSet<OWLClass> propertyRangeNodeSet){
		// Since property range returned more than one class, this will have
		// to create  a new class as the intersection of all of them
		
		OWLDataFactory dFactory = OWLManager.getOWLDataFactory();
		HashSet<OWLClassExpression> terms = new HashSet<>();
		
		for ( org.semanticweb.owlapi.reasoner.Node<OWLClass> node : propertyRangeNodeSet.getNodes()){
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
		// CBL: the range should not be added to the new shape 
		// it is only the new Shape that is connected

		if (intersection.getPropertyBox() == null) {
			intersection.createPropertyBox();
		}	
		
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
}
	

