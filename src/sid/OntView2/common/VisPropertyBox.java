package sid.OntView2.common;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.Font;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import java.util.*;

public class VisPropertyBox {
	int height = 0;
	int posy;
	boolean visible = false;
	VisClass vClass;
	HashSet<VisClass> visClassSet;
	HashMap<OWLObjectProperty, Shape> objectPropertyMap;
	HashMap<OWLDataProperty, String> dataPropertyMap;
	HashSet<OWLObjectProperty>   objectPropertySet;
	ArrayList<VisObjectProperty>       propertyList;
	ArrayList<VisDataProperty>   dPropertyList;
	ArrayList<OWLObjectProperty> orderedObjectProperties; //to keep track of the y offset
	ArrayList<VisConnectorPropertyRange> propRangeConnectorList;
	int objectPropHeight;
	OWLReasoner reasoner;
	HashMap<String, VisObjectProperty> graphVisProperties =null;
	
	public void setVisible(boolean b){visible=b;}
	public int getHeight(){
		if (visible) {
			return height;
		}
		else {
			return 0; 
		}
	}
	public void setHeight(int newHeight){ height = newHeight; }
	
	private HashMap<String, VisObjectProperty> getVisPropertiesInGraph(){
		if (graphVisProperties ==null)
			graphVisProperties = vClass.graph.propertyMap;
		return graphVisProperties;
	}
	
	private OWLReasoner getReasoner(){
		if (reasoner==null)
			reasoner = vClass.graph.paintframe.getReasoner();
		return reasoner;
	}
	public VisPropertyBox(VisClass c){
		propertyList = new ArrayList<>();
		objectPropertyMap = new HashMap<>();
		dataPropertyMap   = new HashMap<>();
		objectPropertySet = new HashSet<>();
		orderedObjectProperties = new ArrayList<>();
		visClassSet   = new HashSet<>();
		dPropertyList = new ArrayList<>();
		vClass = c;
		posy = vClass.currentHeight;
		propRangeConnectorList = new ArrayList<>();
	}

	public void calculateHeight(){
		GraphicsContext context = vClass.graph.paintframe.getGraphicsContext2D();
		Text f = new Text();
		f.setFont(context.getFont());
		int ascent = (int)f.getLayoutBounds().getHeight();
		height = (ascent + 4) * (propertyList.size()+dPropertyList.size());
		objectPropHeight =  (ascent + 4) * (propertyList.size());
	}
	
	public void draw(GraphicsContext g){
		if (g == null){
			return;
		}
		Color prevColor = (Color) g.getFill();
        Font prevFont = g.getFont();
		Font font = Font.font("DejaVu Sans", FontWeight.NORMAL, 9);
		g.setFont(font);
		g.setFill(Color.BLACK);
		for (VisObjectProperty p: propertyList){
	    	if (p.visible){
				p.draw(g);
				p.drawConnectors(g);
	    	}
	    }
	    for (VisDataProperty p: dPropertyList){
	    	p.draw(g);
		}
		g.setFill(prevColor);
        g.setFont(prevFont);
    }
	
	public void sortProperties(){
		HashSet<OWLObjectProperty> rootProperties = rootProperties();
		ArrayList<VisObjectProperty> ordered = new ArrayList<>();
		for (OWLObjectProperty parent : rootProperties){
			VisObjectProperty vParent = getVisPropertiesInGraph().get(VisObjectProperty.getKey(parent));
			ordered.add(vParent);
			for(VisObjectProperty prop : propertyList){
				if ((prop!= vParent) && (!ordered.contains(prop)) && (isSubProperty(prop.oPropExp, vParent.oPropExp)))
					ordered.add(prop);
			}
		}
		int offset = 0;
		for (VisObjectProperty orderedProperty : ordered){
			orderedProperty.vOffset =offset;
			offset++;
		}
		ordered.clear();
    }
	
	public boolean isSubProperty(OWLObjectPropertyExpression e1,OWLObjectPropertyExpression e2){
		OWLDataFactory dFactory = OWLManager.getOWLDataFactory();
        return getReasoner().isEntailed(dFactory.getOWLSubObjectPropertyOfAxiom(e1, e2));
	}

	public VisObjectProperty add(OWLObjectProperty objProp, Shape range, OWLOntology ontology){
		VisObjectProperty v =null;
		if (!VisObjectProperty.contains(propertyList, objProp)){
			v = new VisObjectProperty(this,objProp,propertyList.size(),range,ontology);
			propertyList.add(v);		
			vClass.graph.propertyMap.put(VisObjectProperty.getKey(objProp),v);
		}
		calculateHeight();
		return v; 
	}
		
	public VisDataProperty add(OWLDataProperty dProp, String dRange, OWLOntology ontology){
		int offset = propertyList.size() + dPropertyList.size();
		VisDataProperty v = null;
		if (!VisDataProperty.contains(dPropertyList, dProp)){
		    v = new VisDataProperty(this,dProp,offset,dRange,ontology);
			dPropertyList.add(v);		
//			CBL::Changing the keys
			vClass.graph.dPropertyMap.put(VisDataProperty.getKey(dProp),v);
		}
		calculateHeight();
		return v;
	}

	public void buildConnections(){
		for (VisObjectProperty vProp : propertyList) {
			NodeSet<OWLObjectPropertyExpression> propNodeSet = getReasoner().getSuperObjectProperties(vProp.oPropExp, true);
			for ( Node<OWLObjectPropertyExpression> node : propNodeSet){
				Set<OWLObjectPropertyExpression> entities = node.getEntities();
				if (entities.size() ==1) {
					for (OWLObjectPropertyExpression exp :entities){
//						CBL::Changing keys
						String key = VisObjectProperty.getKey(exp);
						VisObjectProperty p = vClass.graph.propertyMap.get(key);
						if (p!=null){
							vProp.add(p);
						}
					}
				}
            }
		}
	}
	
	
	private HashSet<OWLObjectProperty> rootProperties() {
	/*
	 * Gets object properties in a VisPropertyBox that are not subsumed by others in
	 * the same box
	 */
		HashSet<OWLObjectProperty> rootSet = new HashSet<>();
		for (VisObjectProperty prop : propertyList){
			if (!prop.subsumed(propertyList)){	
		    	rootSet.add((OWLObjectProperty) prop.oPropExp);
			}	
		}
		return rootSet;
	}

    public static void sortProperties(VisGraph visGraph) {
		for ( java.util.Map.Entry<String, Shape> entry : visGraph.getShapeMap().entrySet()){
			Shape shape = entry.getValue();
			if ((shape instanceof VisClass) && (shape.asVisClass().getPropertyBox()!=null)){
				shape.asVisClass().getPropertyBox().sortProperties();
			}
		}		
	}

	public static void buildConnections(VisGraph visGraph) {
		for (java.util.Map.Entry<String, Shape> entry: visGraph.getShapeMap().entrySet() ){
			if ((entry.getValue() instanceof VisClass) && (entry.getValue().asVisClass().getPropertyBox()!=null))
				entry.getValue().asVisClass().getPropertyBox().buildConnections();
		}
		
	}

	public ArrayList<VisObjectProperty> getProperties() {
		return propertyList;
	}
	
}
