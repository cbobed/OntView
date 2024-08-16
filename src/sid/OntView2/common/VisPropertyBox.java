package sid.OntView2.common;

import javafx.scene.canvas.GraphicsContext;
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
	int maxWidth=0;
	int height = 0;
	int posy;
	boolean visible = false;
	
	VisClass vclass;
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
	
	
	ArrayList<String> rep;
	public void setVisible(boolean b){visible=b;}
	public int getHeight(){
		if (visible) {
			return height;
		}
		else {
			return 0; 
		}
	}
	public void setHeight(int x){height =x;}
	
	private HashMap<String, VisObjectProperty> getVisPropertiesInGraph(){
		if (graphVisProperties ==null)
			graphVisProperties = vclass.graph.propertyMap;
		return graphVisProperties;
	}
	
	
	private OWLReasoner getReasoner(){
		if (reasoner==null)
			reasoner = vclass.graph.paintframe.getReasoner();
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
		vclass = c;
		posy = vclass.currentHeight;
		propRangeConnectorList = new ArrayList<>();
	}

	// REVISAR
	public void calculateHeight(){
		GraphicsContext context = vclass.graph.paintframe.getGraphicsContext2D();
		Text f = new Text();
		f.setFont(context.getFont());
		int ascent = (int)f.getLayoutBounds().getHeight();
		height = (ascent + 3) * (propertyList.size()+dPropertyList.size());
		objectPropHeight =  (ascent + 3) * (propertyList.size());
		
	}
	
	public void draw(GraphicsContext g){
		if (g == null){
			return;
		}
		Font font = Font.font("Dialog", FontWeight.NORMAL, 9);
		g.setFont(font);
	    for (VisObjectProperty p: propertyList){
	    	if (p.visible){
	    	p.draw(g);
	    	p.drawConnectors(g);
	    	}
	    }
	    for (VisDataProperty p: dPropertyList){
	    	p.draw(g);
}
    }
	
	public void sortProperties(){
		HashSet<OWLObjectProperty> rootProperties = rootProperties();
		ArrayList<VisObjectProperty> ordered = new ArrayList<VisObjectProperty>();
		for (OWLObjectProperty parent : rootProperties){
//			CBL::Changing the keys
//			VisObjectProperty vparent = getVisPropertiesInGraph().get(ExpressionManager.reduceObjectPropertyName(parent));
			VisObjectProperty vparent = getVisPropertiesInGraph().get(VisObjectProperty.getKey(parent));
			
			ordered.add(vparent);
			for(VisObjectProperty prop : propertyList){

				if ((prop!= vparent) && (!ordered.contains(prop)) && (isSubProperty(prop.oPropExp, vparent.oPropExp)))
					ordered.add(prop);
			}
		}
		int offset = 0;
		for (VisObjectProperty orderedProperty : ordered){
			orderedProperty.voffset=offset;
			offset++;
		}
		ordered.clear();
		offset = 0;
	}
	
	public boolean isSubProperty(OWLObjectPropertyExpression e1,OWLObjectPropertyExpression e2){
		OWLDataFactory dFactory = OWLManager.getOWLDataFactory();
	    if (getReasoner().isEntailed(dFactory.getOWLSubObjectPropertyOfAxiom(e1, e2))){
	    	return true;	    	
	    }
	    else {
	    	return false;
	    }	
		
	}

	public VisObjectProperty add(OWLObjectProperty objProp, Shape range, OWLOntology ontology){
		VisObjectProperty v =null;
		if (!VisObjectProperty.contains(propertyList, objProp)){
			v = new VisObjectProperty(this,objProp,propertyList.size(),range,ontology);
			propertyList.add(v);		
//			CBL::changing the keys
//			vclass.graph.propertyMap.put(ExpressionManager.reduceObjectPropertyName(objProp),v);
			vclass.graph.propertyMap.put(VisObjectProperty.getKey(objProp),v);
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
//			vclass.graph.dPropertyMap.put(ExpressionManager.reduceDataPropertyName(dProp),v);
			vclass.graph.dPropertyMap.put(VisDataProperty.getKey(dProp),v);
		}
		calculateHeight();
		return v;
	}
	
	public void buildConnections(){
		
		for (VisObjectProperty vprop : propertyList) {

			NodeSet<OWLObjectPropertyExpression> propNodeSet = getReasoner().getSuperObjectProperties(vprop.oPropExp, true);
			for ( Node<OWLObjectPropertyExpression> node : propNodeSet){
				Set<OWLObjectPropertyExpression> entities = node.getEntities();
				if (entities.size() ==1) {
					for (OWLObjectPropertyExpression exp :entities){
//						CBL::Changing keys
//						String key = ExpressionManager.reduceObjectPropertyName(exp);
						String key = VisObjectProperty.getKey(exp);
						VisObjectProperty p = vclass.graph.propertyMap.get(key);
						if (p!=null){
							vprop.add(p);
						}
					}
				}
				else { //entities size > 1 implies multiple heritance
					;
				}
			}
		}
	}
	
	
	private HashSet<OWLObjectProperty> rootProperties() {
	/*
	 * Gets object properties in a VisPropertyBox that are not subsumed by others in
	 * the same box
	 */
		HashSet<OWLObjectProperty> rootSet = new HashSet<OWLObjectProperty>();
		for (VisObjectProperty prop : propertyList){
			if (!prop.subsumed(propertyList)){	
		    	rootSet.add((OWLObjectProperty) prop.oPropExp);
			}	
		}
		return rootSet;
	}

	public VisObjectProperty getVisProperty(OWLObjectProperty oprop){
//		CBL::Changing the keys
//		return vclass.graph.propertyMap.get(ExpressionManager.reduceObjectPropertyName(oprop));
		return vclass.graph.propertyMap.get(VisObjectProperty.getKey(oprop));
	}

	
	public int getObjectPropHeight(){
		calculateHeight();
		return objectPropHeight;
	}
	public int getMaxWidth(){
		if (maxWidth ==0){
			for (VisObjectProperty op : propertyList){
				maxWidth = (op.getLabelWidth() > maxWidth ? op.getLabelWidth() : maxWidth); 
			}
			for (VisDataProperty dp : dPropertyList){
				maxWidth = (dp.getLabelWidth() > maxWidth ? dp.getLabelWidth() : maxWidth);
			}
		}
		return maxWidth;
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
