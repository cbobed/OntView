package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.xml.sax.SAXException;
import sid.OntView2.utils.ExpressionManager;
import sid.OntView2.utils.ProgressBarDialogThread;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static sid.OntView2.utils.ExpressionManager.qualifyLabel;
import static sid.OntView2.utils.ExpressionManager.replaceString;

public class VisGraph extends Observable implements Runnable{

	private int progress = 0;
    
    
    HashSet<VisLevel>       levelSet;
    ArrayList<VisConnector> connectorList;
	ArrayList<VisConnector> dashedConnectorList;
	
    final public Map<String, Shape>    shapeMap;
	HashMap<String, Shape>           tempMap;
	HashMap<String, VisObjectProperty>     propertyMap;
	HashMap<String, VisDataProperty> dPropertyMap;
	HashMap<String, OWLSubPropertyChainOfAxiom> chainPropertiesMap;
	GraphReorder reorder;
	private int zoomLevel = 1;
	PaintFrame paintframe;
    VisGraph   unexpandedGraph = null;
    boolean    expanded = false;
    boolean    disjoint = true;
    boolean    created  = false;
	private OWLOntology activeOntology;
	private HashSet<OWLClassExpression> set;
	private boolean check;
	private OWLReasoner reasoner;
	private LinkedHashSet<Observer> observers;
	private HashMap<String,String> qualifiedLabelMap;
	
    public HashMap<String, String> getQualifiedLabelMap(){return qualifiedLabelMap;}
	public List<VisConnector> getDashedConnectorList(){ return dashedConnectorList;}
	public List<VisConnector> getConnectorList(){ return connectorList;}
	public int getProgress(){return progress;}
    private void setProgress(int p){progress = p;}
    public int getZoomLevel(){return zoomLevel;}
    public void setZoomLevel(int z){zoomLevel=z;}
    public Map<String,Shape> getShapeMap() {return shapeMap;}
    public PaintFrame getPaintFrame(){return paintframe;}
    public HashSet<VisLevel> getLevelSet(){return levelSet;}
	public void setActiveOntology(OWLOntology pactiveOntology) {activeOntology = pactiveOntology;}
	public void setOWLClassExpressionSet(HashSet<OWLClassExpression> pset) { set = pset;}
	public void setCheck(boolean pcheck) {check = pcheck;	}
	public OWLOntology getActiveOntology(){return activeOntology;}
	public HashSet<OWLClassExpression> getOWLClassExpressionSet(){return set;}
	public boolean isExpanded(){return check;}
	public void setReasoner(OWLReasoner preasoner) {reasoner = preasoner;}
	public OWLReasoner getReasoner(){return reasoner;}
	public boolean isCreated(){return created;}
	
	//<CBL 25/9/13> 
	// Added a field to handle the aliases of the different shapes
	public HashMap<String, Shape> definitionsMap = null;

	private CountDownLatch latch;
	public void setLatch(CountDownLatch latch) { this.latch = latch; }

	
	/**
	 * Progress bar criteria. From 0-70 % it will depend on the number of shapes added to the map
	 */
    public  VisGraph(PaintFrame pframe) {
		shapeMap     = new ConcurrentHashMap<>();
		definitionsMap = new HashMap<>();
		tempMap      = new HashMap<>();
		propertyMap  = new HashMap<>();
		dPropertyMap = new HashMap<>();
		connectorList 		= new ArrayList<>();
		dashedConnectorList = new ArrayList<>();
		levelSet 			= new HashSet<>();
		paintframe = pframe;
		observers  = new LinkedHashSet<>();
		qualifiedLabelMap = new HashMap<>();
	}

	/**
	 * To avoid possible inheritance problems
	 */
	public void clearShapeMap() {
		shapeMap.clear();
		definitionsMap.clear();
		tempMap.clear();
		propertyMap.clear();
		dPropertyMap.clear();
		connectorList.clear();
		dashedConnectorList.clear();
		levelSet.clear();
		observers.clear();
		qualifiedLabelMap.clear();
	}

    private void addChainProperty(String str, OWLSubPropertyChainOfAxiom axiom){
    	if (chainPropertiesMap== null)
    		chainPropertiesMap = new HashMap<String, OWLSubPropertyChainOfAxiom>();
    	chainPropertiesMap.put(str, axiom);
    }
    
    public Shape getShape(String key){
    	return shapeMap.get(key);
    }
    
    public int getWidth() {
    	VisLevel lvl = VisLevel.getLevelFromID(levelSet, VisLevel.lastLevel(levelSet));
		if (lvl == null) {
			System.err.println("VisLevel is null in getWidth method.");
			return 0;
		}
		return lvl.getWidth()+lvl.getXpos()+VisConstants.WIDTH_MARGIN;
    }
    public int getHeight() {
    	int maxy=0;
		for (Entry<String, Shape> entry : shapeMap.entrySet()) {
			maxy = (entry.getValue().getPosY() > maxy ? entry.getValue().getPosY() : maxy);
		}

    	return maxy+VisConstants.HEIGHT_MARGIN;
    }
    
    public void adjustPanelSize(float factor){
		if (paintframe == null) {
			System.err.println("paintframe is null in adjustPanelSize method.");
			return;
		}
		
		int x = (int) (getWidth() * factor);
		int y = (int) (getHeight() * factor);
		paintframe.setWidth(x);
		paintframe.setHeight(y);

    }

	private OWLClassExpression getTopClass(OWLOntology activeOntology) {
		for(OWLClassExpression e : activeOntology.getClassesInSignature()){
			if (e.isTopEntity())
				return e;
		}
		return null;	
	}
	
	
	/**
	 *  builds the complete graph
	 *  relies on wether it's expanded or not, and rearranging visual info
	 */
	public void buildReasonedGraph(OWLOntology activeOntology,OWLReasoner reasoner, HashSet<OWLClassExpression> set,boolean check) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
		
		//It's important to set config constants before creating 
		//will have to move this elsewhere
		updateProgressBarObserver(0);
		clearAll();
		// <CBL 24/9/13> updated to just take into account the information out of the 
		// reasoner
		this.reasonedDepthTraversal(activeOntology,reasoner,set,0);
		
		updateProgressBarObserver(70);
		// <JBL> this method must  not be invoked after expanding
		// <CBL 24/9/13> 
		// Updated: it doesn't create the connectors anylonger
		// 		now, all the definitions that are associated to the 
		// 		same name are stored together
		linkDefinitionsToDefinedClasses(activeOntology,reasoner);
		arrangePos();
		updateProgressBarObserver(75);
		VisLevel.adjustWidthAndPos(levelSet);
		if (!check){
			expandNestedClassExpressions(reasoner,activeOntology);
	    	arrangePos();        
	    	VisLevel.shrinkLevelSet(levelSet);
		}	
		VisLevel.adjustWidthAndPos(levelSet);
		
		addBottomNode(reasoner,activeOntology);
		
		for (Entry<String,Shape> entry : shapeMap.entrySet()){
			Shape shape = entry.getValue();
			if (shape instanceof VisClass) {
				
//				// <CBL 25/9/13> 
//				// we do not add the disjoint connectors of Bottom as, by definition, it is 
//				// disjoint with anything
//				// the same reasoning is applied to the classes equivalent to bottom
//				if (reasoner.isSatisfiable(shape.asVisClass().getLinkedClassExpression()))
//					shape.asVisClass().addReasonedDisjointConnectors();
				// independently of its satisfiability 
				// we add the disjointness axioms to top and bottom
				
				// NOTE: The use of reasoned disjoint connectors introduces a lot of noise in the graph: 
				// while the equivalence could be treated looking for accessibility paths 
				// to create clusters, disjointness cannot be treated in that way
				// the reasoned information is given in the tooltip 
				
				shape.asVisClass().addAssertedDisjointConnectors(); 
				shape.asVisClass().addEquivalentConnectors();
			}	
		}
		updateProgressBarObserver(80);
		placeProperties(activeOntology,reasoner,set);
    	arrangePos();
		VisLevel.shrinkLevelSet(levelSet);
    	VisLevel.adjustWidthAndPos(levelSet);

    	updateProgressBarObserver(85);
    	removeRedundantConnector();
    	System.gc();
        reorder = new GraphReorder(this);
    	reorder.visualReorder();
    	adjustPanelSize((float) 1.0);
    	updateProgressBarObserver(90);
    	clearDashedConnectorList();
    	paintframe.doKceOptionAction();
    	
    	VisLevel.adjustWidthAndPos(getLevelSet());
    	paintframe.getParentFrame().loadSearchCombo();
    	updateProgressBarObserver(100);
    	paintframe.stateChanged.set(true);
    	
	}
	
	
	

	public void changeRenderMethod(Boolean labelRendering, Boolean qualifiedRendering ){
		
		for (Entry<String,Shape> entry : shapeMap.entrySet()){
			Shape shape = entry.getValue();
			if (shape instanceof VisClass) {
				shape.asVisClass().swapLabel(labelRendering, qualifiedRendering);
			}
		}
		
		// CBL
		// we also change the visible label for both object and datatype properties
		for (Entry<String, VisObjectProperty> oProp: propertyMap.entrySet()) {
			VisObjectProperty p = oProp.getValue(); 
			p.swapLabel(qualifiedRendering); 
		}
		for (Entry<String, VisDataProperty> dProp: dPropertyMap.entrySet()) {
			VisDataProperty v = dProp.getValue(); 
			v.swapLabel(qualifiedRendering); 
		}
		
    	VisLevel.adjustWidthAndPos(levelSet);
    	VisLevel.adjustWidthAndPos(levelSet);
    	arrangePos();
	}
	
	// Convenience method to allow backwards code compatibility 
	public void changeRenderMethod(Boolean labelRendering){
		changeRenderMethod(labelRendering,false); 
	}
	
	/**
	 * adds both object properties and data properties to the graph
	 * looks for domains (creates if not found), ranges.
	 * @param in
	 * @param delimiter
	 */
	
	
	
	public static String removePrefix(String in,String delimiter){
		String[] tokens = in.split(delimiter);
		if (tokens[1]!= null)
			return tokens[1];
		else 
			return "noDelimiter";
	}
	
	public void clearAll(){
		propertyMap.clear();
		dPropertyMap.clear();
		shapeMap.clear();
		connectorList.clear();
		dashedConnectorList.clear();
		levelSet.clear();
		// <CBL> 
		definitionsMap.clear(); 
	}
	
	
	/**
	 * it links definitions to defined classes.
	 * Note that for this to work, it must be used after creating and before expanding anon classes
	 */
	private void linkDefinitionsToDefinedClasses(OWLOntology activeOntology,OWLReasoner reasoner) {

		for ( Entry<String, Shape> entry : shapeMap.entrySet()){
			Shape shape = entry.getValue();

			if ((shape instanceof VisClass) && (shape.asVisClass().isDefined)){
				VisClass defClassShape = entry.getValue().asVisClass();
				if (defClassShape.getLinkedClassExpression() instanceof OWLClass){

					EntitySearcher.getEquivalentClasses(defClassShape.getLinkedClassExpression().asOWLClass(), activeOntology).forEach(
						definition -> {
							if (definition.isAnonymous()) {
								shape.asVisClass().addDefinition(definition);
								// <CBL 25/9/13>
								// We also add the definition to the aliases handling
								definitionsMap.put(Shape.getKey(definition), shape);
								// <CBL 24/9/13>
								// the definitions are now displayed along with the name
								// in the same shape
								// we don't need to create this connector any longer
								//									Shape definitionShape =  shapeMap.get(definition.toString());
								//									connect(definitionShape,defClassShape);
								//									((VisClass) definitionShape).addSon((VisClass)defClassShape);
								//									defClassShape.addParent((VisClass)definitionShape);
								//									</CBL>
							}
						}
					);
				}
				// <CBL 25/9/13> 
				// if we have added definitions
				// we have to update the width of the shape
				shape.asVisClass().setWidth(shape.asVisClass().calculateWidth());
				shape.asVisClass().setHeight(shape.asVisClass().calculateHeight());

			}
		}
	}
	
	private void placeProperties(OWLOntology activeOntology2,
			OWLReasoner reasoner2, HashSet<OWLClassExpression> set2) {
			placeChainProperties(activeOntology2, reasoner2, set2);
			placeObjectProperties(activeOntology2, reasoner2, set2);
			placeDataProperties(activeOntology2, reasoner2, set2);
			
		}
	
	private void placeObjectProperties(OWLOntology activeOntology,OWLReasoner reasoner, HashSet<OWLClassExpression> set) {

		VisClass c;
		Shape range = null; // non anonymous for the moment
		Set<OWLObjectProperty> propertySet = activeOntology.getObjectPropertiesInSignature();
		for (OWLObjectProperty property : propertySet){

			NodeSet<OWLClass> propertyDomainNodeSet = reasoner.getObjectPropertyDomains(property, true);
			NodeSet<OWLClass> propertyRangeNodeSet = reasoner.getObjectPropertyRanges(property, true); 
			
//			for (Node<OWLClass> z : reasoner.getObjectPropertyRanges(property,true)){
//				for (OWLClassExpression ran : z.getEntities()){
//					range = lookUpOrCreate(ran);
//					break;
//				}
//			}
//			
			
//			System.out.println(property);
//			System.out.println("domain: "+propertyDomainNodeSet);
//			System.out.println("range: "+propertyRangeNodeSet);
			
			// CBL: 
			// changed the way the range shape is added and handled
			if (propertyRangeNodeSet.getNodes().size()>1) {
				range = VisObjectProperty.addRange(this, propertyRangeNodeSet, property, reasoner, activeOntology); 
			}
			else {
				// there is only one node in the range definition
				// note that now there is no possible anonymous expression
				// and therefore the reasoner always is going to return a named class 
				// which can be used to look up for the shape 
				range = lookUpOrCreate (propertyRangeNodeSet.getNodes().iterator().next().getRepresentativeElement()); 
			}
			
			
			if (propertyDomainNodeSet.getNodes().size()>1){
				VisObjectProperty.addDomain(this,propertyDomainNodeSet,property,reasoner,activeOntology,range);
			}
			else { //common case 
				for (Node<OWLClass> o : propertyDomainNodeSet ){
					
					for (OWLClass oclass : o.getEntities()){
						
//			System.out.println("searching "+oclass.toString());
						c =	(VisClass) getShapeFromOWLClassExpression(oclass);
//			System.out.println("c is  "+c);
//			dumpShapeMap();
//						CBL :: changing the keys of the visual objects
//						c.properties.add(property.getIRI().getFragment());

						c.properties.add(property.getIRI().toString());
						if (c.getPropertyBox() == null) 
							c.createPropertyBox();
						
//						CBL :: changing the keys of the visual objects
//						if (propertyMap.get(ExpressionManager.reduceObjectPropertyName(property))!=null)
//							continue;
						
						if (propertyMap.get(VisObjectProperty.getKey(property))!=null)
							continue;
						
					    VisObjectProperty v = c.getPropertyBox().add(property,range,activeOntology);	
					    if (v!=null){
//							CBL :: changing the keys of the visual objects
//					    	propertyMap.put(ExpressionManager.reduceObjectPropertyName(property), v);
					    	propertyMap.put(VisObjectProperty.getKey(property), v);
					    }
					}
				}
			}
			
			VisPropertyBox.sortProperties(this);
			VisPropertyBox.buildConnections(this);
//			for (Entry<String,Shape> entry : shapeMap.entrySet()){
//				Shape shape = entry.getValue();
//				if ((shape instanceof VisClass) && (shape.asVisClass().getPropertyBox()!=null)){
//					shape.asVisClass().getPropertyBox().sortProperties();
//				}
//			}
			
		}
		
//		for (Entry<String,Shape> entry: shapeMap.entrySet() ){
//			if ((entry.getValue() instanceof VisClass) && (entry.getValue().asVisClass().getPropertyBox()!=null))
//				entry.getValue().asVisClass().getPropertyBox().buildConnections();
//		}
	}	
	
	private void placeChainProperties(OWLOntology activeOntology,OWLReasoner reasoner, HashSet<OWLClassExpression> set) {
		
		for ( OWLAxiom  axiom:  activeOntology.getAxioms()){
			if (axiom.getAxiomType() == AxiomType.SUB_PROPERTY_CHAIN_OF){
				OWLSubPropertyChainOfAxiom chain_axiom = (OWLSubPropertyChainOfAxiom) axiom;
//				CBL changing the keys
				this.addChainProperty(VisObjectProperty.getKey(chain_axiom.getSuperProperty()),chain_axiom);
			}
		}
	}

	private void placeDataProperties(OWLOntology activeOntology,OWLReasoner reasoner, HashSet<OWLClassExpression> set) {
		VisClass c;

		//getting datatype properties,their domains and ranges
		Set<OWLDataProperty> dataPropertySet = activeOntology.getDataPropertiesInSignature();
		String dRange ="unknown";
		for (OWLDataProperty dataProperty : dataPropertySet){

			for (OWLDataRange z : EntitySearcher.getRanges(dataProperty, activeOntology).toList()) //one range
			{
				dRange = removePrefix(z.toString(),":");
			}

			NodeSet<OWLClass> propertyDomainNodeSet = reasoner.getDataPropertyDomains(dataProperty, true);

			if (propertyDomainNodeSet.getNodes().size()>1){
				// VisObjectProperty.addDomain(this,propertyDomainNodeSet,property,reasoner,activeOntology,range);
				VisDataProperty.addDomain(this, propertyDomainNodeSet, dataProperty, reasoner, activeOntology, dRange);
			}
			else {
				for (Node<OWLClass> o : propertyDomainNodeSet ){
					for (OWLClass oclass : o.getEntities()){
					    c =	(VisClass) getShapeFromOWLClassExpression(oclass);
						if (c.getPropertyBox() == null)
							c.createPropertyBox();
	//					CBL::Changing the keys
	//					if (dPropertyMap.get(ExpressionManager.reduceDataPropertyName(dataProperty))!=null)
	//						continue;
						if (dPropertyMap.get(VisDataProperty.getKey(dataProperty))!=null)
							continue;
						VisDataProperty v =c.getPropertyBox().add(dataProperty,dRange,activeOntology);
						if (v!=null){
//							CBL::Changing the keys
//							dPropertyMap.put(ExpressionManager.reduceDataPropertyName(dataProperty), v);
							dPropertyMap.put(VisDataProperty.getKey(dataProperty), v);
						}
					}
				}
			}

			// <CBL 25/9/13>
			// we currently do not handle dataProperties hierarchies
			// TO_DO: update sortProperties and buildConnections to handle the dataproperties as well
			//
			//	VisPropertyBox.sortProperties(this);
			//	VisPropertyBox.buildConnections(this);

		}
	}


	/**
	 * Traversal of the reasoned graph 
	*/
	private void reasonedDepthTraversal(OWLOntology activeOntology, 
			                               OWLReasoner reasoner,
			                               HashSet<OWLClassExpression> par,int depthlevel){

    	String key;
    	Shape value;
    	VisClass vis;
	
	    for (OWLClassExpression loop_owlclassExp : par) {
	
         	//getting key and value. Different if it's a named class or just an expression
	    	key  = Shape.getKey(loop_owlclassExp);
	    	value = shapeMap.get(key);
	    	// if it's already been added, just skip it  
	    	if(value != null){
  	            continue;
  	        }
	  	    vis= addVisClass(key,depthlevel,loop_owlclassExp,activeOntology,reasoner);
	  	    setProgressFromShapeNumber();
            value = vis;
            // Mix into a set, both reasoned subclasses for the current owlclass expression
  	        // and explicit subclasses
            HashSet<OWLClassExpression> subSet   = new HashSet<OWLClassExpression>();
            HashSet<OWLClassExpression> equivSet = new HashSet<OWLClassExpression>();
            
            // <CBL>: 
            // At first sight, we do not need the superClasses taken from the ontology anymore
            // but we need the superClasses to establish the connections in the graph
            HashSet<OWLClassExpression> superSet = new HashSet<OWLClassExpression>();
            
            // <CBL>: 
            // We do not need to access the ontology as all the anonymous classes have been 
            // named
//            if (loop_owlclassExp instanceof OWLClass) {
//  	        	for (OWLClassExpression d : loop_owlclassExp.asOWLClass().getSubClasses(activeOntology)){
//  	        		if (reasoner.isSatisfiable(d))
//  	        			subSet.add(d);
//  	        	}
//  	            for (OWLClassExpression d : loop_owlclassExp.asOWLClass().getEquivalentClasses(activeOntology)) {equivSet.add(d);}
//  	            for (OWLClassExpression d : loop_owlclassExp.asOWLClass().getSuperClasses(activeOntology)) {superSet.add(d);}
//  	        }
            // </CBL>
            
  	        //esto probablemente me lo lleve afuera para descargar el codigo
  	        for( Node<OWLClass> loop_node : reasoner.getSubClasses(loop_owlclassExp, true).getNodes()) {
  	  	        for( OWLClass loop_var2 : loop_node.getEntities()) {
  	  	        	if (!loop_node.isBottomNode())
                        subSet.add(loop_var2);
  	  	        }
  	  	    }
  	      

  	        for( Node<OWLClass> loop_node : reasoner.getSuperClasses(loop_owlclassExp, true).getNodes()) {
  	  	        for( OWLClass loop_var2 : loop_node.getEntities()) {
  	  	        	if (!loop_node.isBottomNode())
                        superSet.add(loop_var2);
  	  	        }
  	  	    }
  	        

  	        reasonedDepthTraversal(activeOntology,reasoner,subSet, depthlevel+1);
  	        
  	      // <CBL>: 
  	        // Processing the superclasses might seem quite redundant as we now 
  	        // can traverse the reasoned graph using only the named classes
  	        // but we need to have created the shapes to include them in the graph
  	        // due to the depth first traversal we perform
  	        if (!superSet.isEmpty()){
  	        	reasonedDepthTraversal(activeOntology,reasoner,superSet, depthlevel-1);
  	        }
  	        else {
	        	if (getTopClass(activeOntology)!= null && !(vis.getLinkedClassExpression().isTopEntity())) {
	        		
	        		connect(vis,(VisClass) shapeMap.get(OWLRDFVocabulary.OWL_THING.getIRI().toString()),connectorList);
	        	}
  	        }
  	        
  	        reasonedDepthTraversal(activeOntology,reasoner,equivSet, depthlevel);
  	        createConnections(vis,superSet,subSet,equivSet,reasoner,activeOntology);

	     }
	}
	
	private void addBottomNode(OWLReasoner reasoner,OWLOntology activeOntology) {
		int maxLevel=0;
		for (VisLevel lvl :levelSet){
			maxLevel = (maxLevel < lvl.getID() ? lvl.getID() : maxLevel);
		}
		VisLevel newl = new VisLevel(this, maxLevel+1, 0);
		for (OWLClass e : reasoner.getBottomClassNode().getEntities()){
			 Shape o = getShapeFromOWLClassExpression(e);
			 if (o==null){
//				 addVisClass(e.asOWLClass().getIRI().getFragment(), maxLevel+1, e,activeOntology,reasoner);
				 addVisClass(e.asOWLClass().getIRI().toString(), maxLevel+1, e,activeOntology,reasoner);
			 }
			 else{
				 o.setVisLevel(newl);
			 }

			for (OWLClassExpression exp : EntitySearcher.getEquivalentClasses(e, activeOntology).toList()){
		    	 if (!(exp instanceof OWLClass) && (shapeMap.get(exp.toString())==null)){
		    		addVisClass(exp.toString(), maxLevel+1, exp, activeOntology,reasoner); 
		    	 }
		     }
		}
	}
	
	
	/**
	*  Creates connections between classes
	*  In reasoned graph, it doesn't add bottom-level nodes
	*  That level is processed after creating the body (first reasonedDepthTraversal call)
    */
	private void createConnections(VisClass vis,HashSet<OWLClassExpression> superSet,HashSet<OWLClassExpression> subSet,
			                                    HashSet<OWLClassExpression> equivSet,OWLReasoner reasoner,OWLOntology activeOntlogy){

		
		for (OWLClassExpression child : subSet) {
		
			if ( (child instanceof OWLClass) && (hasAnonymousDefinition(child, reasoner)) )
				continue;
			Shape val = getShapeFromOWLClassExpression(child);
			if (val!= null){
				connect(val.asVisClass(),vis, connectorList);
			}	
		}
		for (OWLClassExpression same : equivSet){
			// Just connect the anonymous definition to the visclass
			if ((vis.isDefined) && same.isAnonymous())
				connect(vis,getShapeFromOWLClassExpression(same).asVisClass(),connectorList);
		}
		for (OWLClassExpression e : superSet){
			connect(vis,(VisClass) getShapeFromOWLClassExpression(e),connectorList);
		}
	}
	
	
	/**
	 *   has an anonymous definition ?
	 *   @return boolean
	 */
	private boolean hasAnonymousDefinition(OWLClassExpression e,OWLReasoner reasoner){

		for (OWLClass equ : reasoner.getEquivalentClasses(e)){
			if (equ.isAnonymous()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * used by LinkToParents()	
	 */
	private void connect(VisClass vis,VisClass parent,ArrayList<VisConnector> pconnectorList) {

		
//		System.err.println("Connecting: "); 
//		System.err.println("\t"+(vis!=null?vis.visibleLabel:"nullClass")); 
//		System.err.println("\t"+(parent!=null?parent.visibleLabel:"nullClass")); 
		
		VisConnector con; 
		parent.addSon(vis);
        vis.addParent(parent);
        //skip if previously added
        for (VisConnector c :pconnectorList) {
           if ((c.from == parent) && (c.to == vis)){
        	  return;}
        }
        con = new VisConnectorIsA(parent, vis);
        
        pconnectorList.add(con);
        vis.inConnectors.add(con);
        parent.outConnectors.add(con);
	}
	
	/**
	 *  Given an OWLClassExpression returns its shape class 	
	 */
	public VisClass getVisualExtension(OWLClassExpression o){
		if (o  instanceof OWLClass) {
			return (VisClass)shapeMap.get(o.asOWLClass().getIRI().toString());
		}		
		if (o  instanceof OWLClassExpression) {
			// <CBL 25/9/13> 
			// we now check also in the definition aliases for the shapes
			// to deal indisticntly with both representations of the same shape
			if (definitionsMap.containsKey(Shape.getKey(o)))
				return (VisClass) definitionsMap.get(Shape.getKey(o)); 
			else 
				return (VisClass) shapeMap.get(Shape.getKey(o));
		}
		return null;
	}
	
	
	/**
	 *  If there's a visible shape in that point this will return a reference to it and null otherwise
	 *  @return shape or null if not found
	 */
	public Shape findShape(Point2D p) {
		for(Iterator<Entry<String,Shape>> it = shapeMap.entrySet().iterator(); it.hasNext();) {
            Entry<String,Shape> e = (Entry<String, Shape>)it.next();
	        int x = e.getValue().getPosX();
	        int y = e.getValue().getPosY();
	        int w = e.getValue().getWidth();
	        int h = e.getValue().getHeight();
	        
	         if (isInShape(x,y,w,h,p) && e.getValue().visible){
	            return e.getValue();
	         }       
	     }
	    return null;
	 }

	private boolean isInShape(int x,int y,int w, int h,Point2D p) {
		return ((p.getX() >= x- (double) w /2)  && (p.getX() <= x+ (double) w /2+ (double) h /2)
        		&& (p.getY() >=y - (double) h /2)  && (p.getY() <= y+ (double) h /2));
	}
	
	/**
	 * Creates a new visclass instance and adds it to the set(hashmap) of visclasses
	 * Creates or adjusts new Vislevels width
	 */
	public VisClass addVisClass(String label,int depthlevel,OWLClassExpression e,OWLOntology activeOntology, OWLReasoner reasoner) {

	       VisLevel vlevel;
	       int levelPosx = VisClass.FIRST_X_SEPARATION;
	       String auxQLabel = ""; 
	       //creating level and adding it
	       // posx = prevLevel posx + width
	       vlevel = VisLevel.getLevelFromID(levelSet, depthlevel);
	       if (vlevel==null) {
	    	   for (VisLevel l : levelSet){
		    	   VisLevel auxlevel;
		    	   auxlevel = VisLevel.getLevelFromID(levelSet,depthlevel-1);
		    	   if (auxlevel != null) {
		    		 levelPosx  = auxlevel.getXpos()+auxlevel.getWidth()+30;
		    	   }
		       }
	    	   vlevel = new VisLevel(this,depthlevel, levelPosx);
	    	   levelSet.add(vlevel);
	       }		   
	       
	       //actual add of the visclass to both the graph and the level
	       VisClass vis = new VisClass(depthlevel,e,label,this);
	       if (e instanceof OWLClass){
			   for (OWLAnnotation  an : EntitySearcher.getAnnotations(e.asOWLClass(), activeOntology).toList() ){
				   if (an.getProperty().toString().equals("rdfs:label")){
					   vis.explicitLabel = an.getValue().toString();
					   vis.explicitLabel.replaceAll("\"", "");
					   vis.explicitLabel = replaceString(vis.explicitLabel);
					   auxQLabel = qualifyLabel(e.asOWLClass(), vis.explicitLabel);
					   if (auxQLabel != null && !"null".equalsIgnoreCase(auxQLabel)) {
						   vis.explicitQualifiedLabel = auxQLabel;
					   }
					   else {
						   vis.explicitQualifiedLabel = vis.explicitLabel;
					   }
				   }
			   }
	       }
	       
	       // CBL: label is the IRI of the element, not to  
	       shapeMap.put(label, vis);
	      
	       vis.label = ExpressionManager.getReducedClassExpression(e);
	       vis.visibleLabel = vis.label; 
	       auxQLabel = ExpressionManager.getReducedQualifiedClassExpression(e); 
	       if (auxQLabel != null && !"null".equalsIgnoreCase(auxQLabel))
	    	   vis.qualifiedLabel = auxQLabel; 
	       else 
	    	   vis.qualifiedLabel = vis.label; 
	    
		   vlevel.addShape(vis);
		   vis.setVisLevel(vlevel);
		   if (e.isAnonymous()) {
	           vis.isAnonymous = true;
		   }
		   if ((e instanceof OWLClass) && (EntitySearcher.isDefined(e.asOWLClass(), activeOntology) )) {
	           vis.isDefined = true;
		   }
	       if (reasoner != null){
	    	   Node<OWLClass> equivClasses = reasoner.getEquivalentClasses(e);
	    	   for (OWLClass entity : equivClasses.getEntities())
	    		   if (entity.isOWLNothing())
	    			   		vis.isBottom= true;
	       }
	       vis.setWidth(vis.calculateWidth());
	       vis.setHeight(vis.calculateHeight());
		   if (vlevel.getWidth() < (vis.getWidth()+20)){
	    	   vlevel.setWidth(vis.getWidth()+20);
		   }
	    	vlevel.updateWidth(vis.getWidth()+20);		   
		   //return the created class
	       if (e instanceof OWLClass){
    		   getQualifiedLabelMap().put(vis.label, e.asOWLClass().getIRI().toString());
	       }
	       return vis;
	}
	
	public Set<Entry<String,Shape>> getClassesInGraph(){
		 return shapeMap.entrySet();
	 }
	
	public void clearDashedConnectorList(){
		dashedConnectorList.clear();
	}
	 
    /**
     * This will be called when closing a node in Paintframe
     * it will redo the dashed connector list
     */
	public void addDashedConnectors(){
		 dashedConnectorList.clear();
		 for (Entry<String,Shape> entry : shapeMap.entrySet()) {
    		Shape s = entry.getValue();
			if (((s.getState()==Shape.CLOSED) || (s.getState()==Shape.PARTIALLY_CLOSED)) && (s.visible))  {
				 dashLink(s, s);
			 }	 
		 }
	 }
	 
	/**
     * Searchs sublevels for nodes that are still visible/referenced
     * and adds a dashedLine connecting them
     */
	private void dashLink(Shape source,Shape current) {

		for (VisConnector c  : current.outConnectors){
			Shape currentSon = c.to;
			if ((currentSon.visible) && !c.visible) {
				// not added if there's already one way
				VisConnector prevAdded = VisConnector.getConnector(dashedConnectorList, source, currentSon);
				if (prevAdded == null){
					dashedConnectorList.add(new VisConnectorDashed(source, currentSon));
				}
			}
			else {
				dashLink(source,currentSon);
			}
		}
	}

	public void arrangePos() {

		boolean done = false;
		int i = 0;
//		while (!done){
		while ((!done) || (i< 50)){
		i++;
			done = true;
			for (Entry<String,Shape> entry : shapeMap.entrySet()){
				Shape currentShape=entry.getValue();
				int currentShapeLevel = currentShape.getVisLevel().getID();
				for (VisConnector con : currentShape.inConnectors){
					Shape parentShape = con.from;
					VisLevel parentLevel = parentShape.getVisLevel();
					if (parentShape.getVisLevel().getID() >= currentShapeLevel) {
						done = false;
						//to
						if (VisLevel.getLevelFromID(levelSet, currentShapeLevel).isBottomLevel()){
							VisLevel.insertLevel(levelSet, currentShapeLevel, this);
							currentShape.setVisLevel(VisLevel.getLevelFromID(levelSet, currentShapeLevel-1));
						}
						else {

							VisLevel newl = VisLevel.getLevelFromID(levelSet, parentShape.getVisLevel().getID()+1);
							if (newl == null) {
								System.out.println("is bottom");
					    		 newl = new VisLevel(this, parentLevel.getID()+1, 
					    				                   parentLevel.getXpos()+parentLevel.getWidth()+30);
						    	 levelSet.add(newl);
								 currentShape.setVisLevel(VisLevel.getLevelFromID(levelSet, currentShapeLevel-1));
     	                    }
							else if ( VisLevel.getLevelFromID(levelSet, parentShape.getVisLevel().getID()+1).isBottomLevel()){
								VisLevel.insertLevel(levelSet, parentShape.getVisLevel().getID()+1, this);

							}
							currentShape.setVisLevel(newl);
						}
					}
				}
			}
		}
	}
	
	private void flushConstraintsToShapeMap(){
		for (Entry<String,Shape> entry : tempMap.entrySet()){
			shapeMap.put(entry.getKey(),entry.getValue());
		}
		tempMap.clear();
	}
	
	 public void expandNestedClassExpressions(OWLReasoner reasoner,OWLOntology activeOntology){
	 /*
	  * Expands anonymous class expressions that appeared in step 1 
	  * Makes use of the private method 'expand' which should be right below
	  */
		OWLClassExpression exp;
		HashSet<Shape> setOfExpanded = new HashSet<Shape>();
		HashSet<Shape> children      = new HashSet<Shape>();
		for (Entry<String,Shape> entry : shapeMap.entrySet() ) {
			if (entry.getValue() instanceof VisClass){
				exp = entry.getValue().asVisClass().getLinkedClassExpression();
				if (exp.isAnonymous()){
			    	setOfExpanded.add(entry.getValue());
			    	for (Shape child :entry.getValue().asVisClass().children){
			    		children.add(child);
			    	}
                    expand(exp,entry.getValue().getVisLevel().getID(),children,activeOntology,reasoner,false);
                    children.clear();
			     }	
		     } 
	     } 
		 removeAnnonymousClassExpressionShape(setOfExpanded);

		 flushConstraintsToShapeMap();
		 setOfExpanded.clear();
     }
	 
	 /**
	  * Expands anonymous nodes into contraint nodes
	  * @param exp
	  * @param depth
	  * @param rightShapes
	  * @param activeOntology
	  * @param reasoner
	  * @param first
	  */
	 private void expand(OWLClassExpression exp,int depth,HashSet<Shape> rightShapes,OWLOntology activeOntology,OWLReasoner reasoner,boolean first){

		 	
		    ClassExpressionType type = exp.getClassExpressionType();
			List<OWLClassExpression> listOps;
			VisConstraint constraint;
			HashSet<Shape> expandedSet = new HashSet<Shape>();
	 	    Set<OWLClassExpression> auxSet= new HashSet<OWLClassExpression>();
	 	    OWLClassExpression selectedDomain;

			int cardinality = 0;
			switch (type) {

		         case OWL_CLASS:
		        	Shape s = getShapeFromOWLClassExpression(exp);
		    		 for (Shape rightShape : rightShapes){
		    			 if (s!=null){
		    			 	connect(s, rightShape);	
		    			 }	
	    		     }
		             break;
		
		         case OBJECT_INTERSECTION_OF:
		                 OWLObjectIntersectionOf andExp = (OWLObjectIntersectionOf)exp;
		                 listOps = andExp.getOperandsAsList();
		                 makeRoomForConstraint(first, depth);
		                 constraint = addVisConstraint(OntViewConstants.AND, exp,type,depth);
			    		 for (Shape rightShape : rightShapes) connect(constraint,rightShape);
			    		 expandedSet.add(constraint);		  
			    		 for (OWLClassExpression op: listOps) { 
		                	 expand(op, depth,expandedSet, activeOntology, reasoner,false); 
		                 }	 
	                     break;
	                   
	              case OBJECT_UNION_OF:
	            	  	 OWLObjectUnionOf orExp = (OWLObjectUnionOf) exp;
		                 listOps = orExp.getOperandsAsList();
		                 makeRoomForConstraint(first, depth);
		                 constraint = addVisConstraint(OntViewConstants.OR, exp,type,depth);
		                 // if it's a definition, connect defined class to the definition (constraint)
		                 Node<OWLClass> equiv = reasoner.getEquivalentClasses(orExp);
		                 for (OWLClass defined : equiv){
		                	 getVisualExtension(defined).removeAllChildren();
		                	 connect(getVisualExtension(defined),constraint);
		                 }
		                 for (OWLClassExpression operand : listOps){
		                	 //connect to the operands if not found, it's probably related to an anonymous class to be expanded
		                	 Shape to = getShapeFromOWLClassExpression(operand);
		                	 if (to == null) {
		                		 expand(operand, depth, new HashSet<Shape>(), activeOntology, reasoner, false);
		                	 	 to = getShapeFromOWLClassExpression(operand);
		                	 }	 
		                 	 if (to!=null)
		                 		connect(constraint,to);
		                 }
		                 break;
	              
	              case OBJECT_ALL_VALUES_FROM:
	                    OWLObjectAllValuesFrom allExp = (OWLObjectAllValuesFrom) exp;
	                    //allExp.getFiller();
	                    makeRoomForConstraint(first, depth);
	                    constraint = addVisConstraint(OntViewConstants.FOR_ALL, exp,type,depth);
			    	    for (Shape f : rightShapes) { connect(constraint,f); }  
	     	    	    expandedSet.add(constraint);		 
	     	    	    for (Node<OWLClass> nodes : reasoner.getObjectPropertyDomains(allExp.getProperty(), false).getNodes()){
			    		  for (OWLClassExpression domain: nodes.getEntities()){
			    			  if (!(reasoner.getEquivalentClasses(allExp).contains((OWLClass) domain)))
			    				 auxSet.add(domain);
			    		  }
			    	    }
			    	    selectedDomain = getDomain(reasoner,auxSet);
			            expand(selectedDomain, depth, expandedSet, activeOntology, reasoner,false);
	                    break;
	                  
	              case OBJECT_COMPLEMENT_OF:
	                   OWLObjectComplementOf notExp = (OWLObjectComplementOf) exp;
	                   makeRoomForConstraint(first, depth);
	                   constraint = addVisConstraint("¬", exp,type,depth);
			    	   for (Shape f : rightShapes) { connect(constraint,f); } 
			    	   expandedSet.add(constraint);		 
	                   expand(notExp.getOperand(),depth,expandedSet,activeOntology, reasoner, first);
	                   break;
	                  
	              case OBJECT_SOME_VALUES_FROM:
	            	   OWLObjectSomeValuesFrom someExp = (OWLObjectSomeValuesFrom) exp;
	            	   makeRoomForConstraint(first, depth);
		               constraint = addVisConstraint(OntViewConstants.SOME, exp,type,depth);
			    	   for (Shape f : rightShapes) { connect(constraint,f); }  
	    	    	   expandedSet.add(constraint);		 
	    	    	   for (Node<OWLClass> nodes : reasoner.getObjectPropertyDomains(someExp.getProperty(), false).getNodes()){
	 		    		  for (OWLClassExpression domain: nodes.getEntities()){
	 		    			  if (!(reasoner.getEquivalentClasses(someExp).contains((OWLClass) domain)))
	 		    				 auxSet.add(domain);
	 		    		  }
	 		    	  }
	 		    	   selectedDomain = getDomain(reasoner,auxSet);
	 		           expand(selectedDomain, depth, expandedSet, activeOntology, reasoner,false);	    	  
	                   break;
	                  
	              case OBJECT_HAS_VALUE:
	                   OWLObjectHasValue hasValueExp = (OWLObjectHasValue) exp;
	            	   makeRoomForConstraint(first, depth);
		               constraint = addVisConstraint(OntViewConstants.HASVALUE, exp,type,depth);
			    	   for (Shape f : rightShapes) { connect(constraint,f); }  
	    	    	   expandedSet.add(constraint);		 
	            	   for (Node<OWLClass> nodes : reasoner.getObjectPropertyDomains(hasValueExp.getProperty(), false).getNodes()){
	 		    		  for (OWLClassExpression domain: nodes.getEntities()){
	 		    			  if (!(reasoner.getEquivalentClasses(hasValueExp).contains((OWLClass) domain)))
	 		    				 auxSet.add(domain);
	 		    		  }
	 		    	   }
	 		    	   selectedDomain = getDomain(reasoner,auxSet);
	 		           expand(selectedDomain, depth, expandedSet, activeOntology, reasoner,false);   	  
	                  break;
	              
	              case OBJECT_ONE_OF:
	                   	OWLObjectOneOf oneOfExp = (OWLObjectOneOf) exp;
	                   	makeRoomForConstraint(first, depth);
			            constraint = addVisConstraint("OneOf", exp,type,depth);
			            for (Shape f : rightShapes) { connect(constraint,f); }  
			               expandedSet.add(constraint);
			               for (OWLClassExpression e : oneOfExp.getClassesInSignature()){
			 		           expand(e, depth, expandedSet, activeOntology, reasoner,false);
			               }   	
	                  break;
	                  
		          case OBJECT_HAS_SELF:
	                   OWLObjectHasSelf hasSelfExp = (OWLObjectHasSelf) exp;
	                   makeRoomForConstraint(first, depth);
		               constraint = addVisConstraint("Self", exp,type,depth);
			    	   for (Shape f : rightShapes) { connect(constraint,f); }  
	    	    	   expandedSet.add(constraint);		 
	    	    	   for (Node<OWLClass> nodes : reasoner.getObjectPropertyDomains(hasSelfExp.getProperty(), false).getNodes()){
	 		    		  for (OWLClassExpression domain: nodes.getEntities()){
	 		    			  if (!(reasoner.getEquivalentClasses(hasSelfExp).contains((OWLClass) domain)))
	 		    				 auxSet.add(domain);
	 		    		  }
	 		    	   }
	 		    	   selectedDomain = getDomain(reasoner,auxSet);
	 		           expand(selectedDomain, depth, expandedSet, activeOntology, reasoner,false);    	  
	                   break;
		                  
		          case OBJECT_EXACT_CARDINALITY:
	        	  	   OWLObjectExactCardinality exactCardExp = (OWLObjectExactCardinality) exp;
	        	  	   makeRoomForConstraint(first, depth);
	        	  	   cardinality = exactCardExp.getCardinality();
		               constraint = addVisConstraint("="+String.valueOf(cardinality), exp,type,depth);
		               for (Shape f : rightShapes) { connect(constraint,f); } 
			    	   expandedSet.add(constraint);		 
	    	    	   for (Node<OWLClass> nodes : reasoner.getObjectPropertyDomains(exactCardExp.getProperty(), false).getNodes()){
	 		    		  for (OWLClassExpression domain: nodes.getEntities()){
	 		    			  if (!(reasoner.getEquivalentClasses(exactCardExp).contains((OWLClass) domain)))
	 		    				 auxSet.add(domain);
	 		    		  }
	 		    	  }
	 		    	   selectedDomain = getDomain(reasoner,auxSet);
	 		           expand(selectedDomain, depth, expandedSet, activeOntology, reasoner,false);	    	  
			    	   break;
	                  
		          case OBJECT_MAX_CARDINALITY:
		        	    try{
			        	   OWLObjectMaxCardinality maxCardExp = (OWLObjectMaxCardinality) exp;
		                   cardinality = maxCardExp.getCardinality();
		                   makeRoomForConstraint(first, depth);
			               constraint = addVisConstraint(OntViewConstants.GREATER_EQUAL+String.valueOf(cardinality), exp,type,depth);
			               for (Shape f : rightShapes) { connect(constraint,f); }
				    	   expandedSet.add(constraint);
				    	   for (Node<OWLClass> nodes : reasoner.getObjectPropertyDomains(maxCardExp.getProperty(), false).getNodes()){
				    		  for (OWLClassExpression domain: nodes.getEntities()){
				    			  if (!(reasoner.getEquivalentClasses(maxCardExp).contains((OWLClass) domain)))
				    				  auxSet.add(domain);
				    		  }
				    	   }
				    	   selectedDomain = getDomain(reasoner,auxSet);

				           expand(selectedDomain, depth, expandedSet, activeOntology, reasoner,false);
		        	   }
	                   catch(Exception e ){e.printStackTrace();}
			           break;
		        	   
		                  
		          case OBJECT_MIN_CARDINALITY:

		        	   OWLObjectMinCardinality minCardExp = (OWLObjectMinCardinality) exp;
	                   cardinality = minCardExp.getCardinality();
	                   makeRoomForConstraint(first, depth);
		               constraint = addVisConstraint(OntViewConstants.GREATER_EQUAL+String.valueOf(cardinality), exp,type,depth);
		               for (Shape f : rightShapes) { connect(constraint,f); } 
			    	   expandedSet.add(constraint);		
			    	   for (Node<OWLClass> nodes : reasoner.getObjectPropertyDomains(minCardExp.getProperty(), false).getNodes()){
			    		  for (OWLClassExpression domain: nodes.getEntities()){
			    			  if (!(reasoner.getEquivalentClasses(minCardExp).contains((OWLClass) domain)))
			    				  auxSet.add(domain);
			    		  }
			    	   }
			    	   selectedDomain = getDomain(reasoner,auxSet);
			           expand(selectedDomain, depth, expandedSet, activeOntology, reasoner,false);
	                   break;
	                  
		          case DATA_ALL_VALUES_FROM:
	                   OWLDataAllValuesFrom dAllExp = (OWLDataAllValuesFrom) exp;
	                   makeRoomForConstraint(first, depth);
	                   constraint = addVisConstraint(OntViewConstants.FOR_ALL, exp,type,depth);
			    	   for (Shape f : rightShapes) { connect(constraint,f); }  
	     	    	   expandedSet.add(constraint);		 
	     	    	   for (Node<OWLClass> nodes : reasoner.getDataPropertyDomains((OWLDataProperty) dAllExp.getProperty(), false).getNodes()){
			    		  for (OWLClassExpression domain: nodes.getEntities()){
			    			  if (!(reasoner.getEquivalentClasses(dAllExp).contains((OWLClass) domain)))
			    				  auxSet.add(domain);
			    		  }
			    	   }
			    	   selectedDomain = getDomain(reasoner,auxSet);
			           expand(selectedDomain, depth, expandedSet, activeOntology, reasoner,false);
	                   break;
	                  
		          case DATA_SOME_VALUES_FROM:
		        	   OWLDataSomeValuesFrom dSomeExp = (OWLDataSomeValuesFrom) exp;
		               makeRoomForConstraint(first, depth);
		               constraint = addVisConstraint(OntViewConstants.SOME, exp,type,depth);
			    	   for (Shape f : rightShapes) { connect(constraint,f); }  
		 	    	   expandedSet.add(constraint);		 
		 	    	   for (Node<OWLClass> nodes : reasoner.getDataPropertyDomains((OWLDataProperty) dSomeExp.getProperty(), false).getNodes()){
			    		  for (OWLClassExpression domain: nodes.getEntities()){
			    			  if (!(reasoner.getEquivalentClasses(dSomeExp).contains((OWLClass) domain)))
			    				  auxSet.add(domain);
			    		  }
			    	   }
			    	   selectedDomain = getDomain(reasoner,auxSet);
			           expand(selectedDomain, depth, expandedSet, activeOntology, reasoner,false);
	                   break;
	                   
		          case DATA_HAS_VALUE:
	                   OWLDataHasValue dFillExp = (OWLDataHasValue) exp;
	                   makeRoomForConstraint(first, depth);
//	                   constraint = (VisConstraint) shapeMap.get("C_"+exp.toString()); 
                	   constraint = addVisConstraint(OntViewConstants.HASVALUE, exp,type,depth);
	                   for (Shape f : rightShapes) { connect(constraint,f); }
		 	    	   expandedSet.add(constraint);		 
		 	    	   for (Node<OWLClass> nodes : reasoner.getDataPropertyDomains((OWLDataProperty) dFillExp.getProperty(), false).getNodes()){
			    		  for (OWLClassExpression domain: nodes.getEntities()){
			    			  if (!(reasoner.getEquivalentClasses(dFillExp).contains((OWLClass) domain)))
			    				  auxSet.add(domain);
			    		  }
			    	   }
			    	   selectedDomain = getDomain(reasoner,auxSet);
			           expand(selectedDomain, depth, expandedSet, activeOntology, reasoner,false);
	                   break;
	                  
		          case DATA_EXACT_CARDINALITY:
	                   OWLDataExactCardinality dExactlyExp = (OWLDataExactCardinality) exp;
	                   makeRoomForConstraint(first, depth);
		               constraint = addVisConstraint("=", exp,type,depth);
			    	   for (Shape f : rightShapes) { connect(constraint,f); }  
		 	    	   expandedSet.add(constraint);		 
		 	    	   for (Node<OWLClass> nodes : reasoner.getDataPropertyDomains((OWLDataProperty) dExactlyExp.getProperty(), false).getNodes()){
			    		  for (OWLClassExpression domain: nodes.getEntities()){
			    			  if (!(reasoner.getEquivalentClasses(dExactlyExp).contains((OWLClass) domain)))
			    				  auxSet.add(domain);
			    		  }
			    	   }
			    	   selectedDomain = getDomain(reasoner,auxSet);
			           expand(selectedDomain, depth, expandedSet, activeOntology, reasoner,false);
	                   break;
	                  
		          case DATA_MAX_CARDINALITY:
	                   OWLDataMaxCardinality dMaxCardExp = (OWLDataMaxCardinality) exp;
	                   makeRoomForConstraint(first, depth);
		               constraint = addVisConstraint(OntViewConstants.LOWER_EQUAL, exp,type,depth);
			    	   for (Shape f : rightShapes) { connect(constraint,f); }  
		 	    	   expandedSet.add(constraint);		 
		 	    	   for (Node<OWLClass> nodes : reasoner.getDataPropertyDomains((OWLDataProperty) dMaxCardExp.getProperty(), false).getNodes()){
			    		  for (OWLClassExpression domain: nodes.getEntities()){
			    			  if (!(reasoner.getEquivalentClasses(dMaxCardExp).contains((OWLClass) domain)))
			    				  auxSet.add(domain);
			    		  }
			    	   }
			    	   selectedDomain = getDomain(reasoner,auxSet);
			           expand(selectedDomain, depth, expandedSet, activeOntology, reasoner,false);
	                   break;
		          
		          case DATA_MIN_CARDINALITY:
	                   OWLDataMinCardinality dMinCardExp = (OWLDataMinCardinality) exp;
	                   makeRoomForConstraint(first, depth);
		               constraint = addVisConstraint(OntViewConstants.GREATER_EQUAL, exp,type,depth);
			    	   for (Shape f : rightShapes) { connect(constraint,f); }  
		 	    	   expandedSet.add(constraint);		
		 	    	   for (Node<OWLClass> nodes : reasoner.getDataPropertyDomains((OWLDataProperty) dMinCardExp.getProperty(), false).getNodes()){
			    		  for (OWLClassExpression domain: nodes.getEntities()){
			    			  if (!(reasoner.getEquivalentClasses(dMinCardExp).contains((OWLClass) domain)))
			    				  auxSet.add(domain);
			    		  }
			    	   }
			    	   selectedDomain = getDomain(reasoner,auxSet);
			           expand(selectedDomain, depth, expandedSet, activeOntology, reasoner,false);
	                  // has to be completed with the generic range
	                  break;
	          default:	                  
	                  break;
			  }            
		 }	
	 
	 
	
	/**
	 *  When asking the reasoner for a property domain, it may return more than one answer
	 * @param reasoner
	 * @param set
	 * @return
	 */
	private OWLClassExpression getDomain(OWLReasoner reasoner,
			Set<OWLClassExpression> set) {

		Set<OWLClassExpression> copy = new HashSet<OWLClassExpression>();
		for ( OWLClassExpression e1 : set){
			copy.add(e1);
		}
		for (OWLClassExpression e1 : set){
			for (OWLClassExpression e2 : set){
				if (e1!=e2){
					boolean discard = false;
					for (Node<OWLClass> subclassNode: reasoner.getSubClasses(e1, false).getNodes()) {
						for (OWLClass subclass : subclassNode.getEntities()){
							discard = discard || (set.contains(subclass));
						}
						if (discard)
							copy.remove(e1);
						if (copy.size()==1)
							break;
					}
				}
			}
		}
		OWLClassExpression result = null;
		for ( OWLClassExpression e : copy){
			result = e;
		}
        if (result == null) {
        	result = getTopClass(this.paintframe.getOntology());
        }
		return result;
	}
	 
    private void makeRoomForConstraint(boolean first,int depth){
    	VisLevel current = VisLevel.getLevelFromID(levelSet, depth);
    	if (!current.isConstraintLevel() && !first){
			 VisLevel.insertLevel(levelSet, depth, this);
    	}
    }

	public  VisConstraint  addVisConstraint(String label,OWLClassExpression nested,ClassExpressionType type,int depth) {
	/*
	 * When adding constraints to the graph, we're iterating through the shapeMap
	 * That's why I use a tempMap; to flush them later
	 */
		
		VisConstraint constraint = null;
		VisLevel currentLevel = VisLevel.getLevelFromID(levelSet, depth);
		constraint = (VisConstraint)tempMap.get(nested.toString());
		if (constraint == null){
			constraint = new VisConstraint(currentLevel, nested, label,this);
			tempMap.put(nested.toString(),constraint);

		}
		return constraint;
		}
	    
	private void connect(Shape parent,Shape vis) {

        if (parent == null) 
        	System.out.println("parent null");
        if (vis == null)
        	System.out.println("vis null");
		VisConnector con = new VisConnectorIsA(parent, vis);
        connectorList.add(con);
        vis.inConnectors.add(con);
        parent.outConnectors.add(con);
	}		
	 
		
	 private void removeAnnonymousClassExpressionShape(Set<Shape> set){
		 //only if it was previously added
		 
		 for (Shape s : set){
			 shapeMap.remove(s.asVisClass().label);
			 s.getVisLevel().getShapeSet().remove(s);
			 
			 s.visible = false;
			 s.asVisClass().children.clear();
			 s.asVisClass().parents.clear();
			 for (VisConnector v : s.outConnectors){
				 v.to.inConnectors.remove(v);
				 if (v.to instanceof VisClass) {
					 v.to.asVisClass().parents.remove(v);
				 }
				 connectorList.remove(v);
			 }
			 for (VisConnector v : s.inConnectors){
				 v.from.outConnectors.remove(v);
				 connectorList.remove(v);
				 if (v.from instanceof VisClass) {
					 v.from.asVisClass().children.remove(v);
				 }
				 connectorList.remove(v);
	
			 }
			 s.outConnectors.clear();
			 s.inConnectors.clear();
			 s.inDashedConnectors.clear();
			 s.outDashedConnectors.clear();
		 }	 
	 }
	 
	 public Shape getShapeFromOWLClassExpression (OWLClassExpression e){
		String key = Shape.getKey(e); 
		Shape retShape = shapeMap.get(key);
		if (retShape != null){
		    return retShape;
	 	}
		else { //is it a constraint waiting to be placed in shapeMap?
			return tempMap.get(key);
		}
	 }

	 private void removeRedundantConnector(){
	 //removes connectors that are implied by others
		 OWLReasoner reasoner = paintframe.getReasoner();
		 OWLOntology ontology = paintframe.getOntology();
		 HashSet<Shape> parents = new HashSet<Shape>();
		 for (Entry<String,Shape> entry : shapeMap.entrySet()){
			 Shape shape = entry.getValue();
			 for (VisConnector in : shape.inConnectors){
				 parents.add(in.from);
			 }
			 remove(parents,shape,reasoner,ontology);
			 parents.clear();
			 
		 }
		 
	 }
	 
	 private void remove(HashSet<Shape>parents,Shape son,OWLReasoner reasoner,OWLOntology ontology){
		 //to remove unnecesary connectors
		 OWLDataFactory dFactory = OWLManager.getOWLDataFactory();
		// until here we had the definition and equivalent problems for removing connectors
		removeImplicitConnectorsBySubsumption(son, parents, ontology, reasoner, dFactory);
			 
	 }
	 
	 
	 /**
	  * A->B, B->C, A->C   ----- > it would remove A->C as it's implicit
	  * @param son
	  * @param parents
	  * @param ontology
	  * @param reasoner
	  * @param dFactory
	  */
	 
	 private void removeImplicitConnectorsBySubsumption(Shape son,HashSet<Shape> parents,OWLOntology ontology,
			 											OWLReasoner reasoner,OWLDataFactory dFactory ){
		
		 //if a subsumes b  remove conector(a,son), as it's implicit
		 for (Shape pi : parents){
			 for (Shape pj : parents){
				 if (pi!=pj){
					 if ((subsumes(pi, pj, ontology, reasoner, dFactory)) && (subsumes(pj, pi, ontology, reasoner, dFactory))){
						 if (pi.getVisLevel().getID() != pj.getVisLevel().getID()){
	
							 Shape candidate = (pi.getVisLevel().getID() < pj.getVisLevel().getID() ? pi:pj);
							 VisConnector c = VisConnector.getConnector(connectorList, candidate, son);
							 if ( (son instanceof VisConstraint)||(candidate instanceof VisConstraint)){
								 c.setRedundant();
							 }
							 else {
								 VisConnector.removeConnector(connectorList, candidate, son);
							 }	 
						 }
					 }
					 else if (subsumes(pi, pj, ontology, reasoner, dFactory)){
						 VisConnector c = VisConnector.getConnector(connectorList, pi, son);
						 if ( (son instanceof VisConstraint)||(pi instanceof VisConstraint)){
							 c.setRedundant();
						 }
						 else {
							 VisConnector.removeConnector(connectorList, pi, son);
						 }	 
					 }
					
					 
				 }
			 }
		 }
	 }	 
	 
	 /**
	  * returns the result of the operation classA subsumes classB
	  * @return
	  */
	 
	 public	 boolean subsumes(Shape b,Shape a,OWLOntology ontology,
			OWLReasoner reasoner,OWLDataFactory dFactory ){
	 		OWLSubClassOfAxiom s = dFactory.getOWLSubClassOfAxiom(a.getLinkedClassExpression(),b.getLinkedClassExpression());
	 		return reasoner.isEntailed(s);
	 }
	 
	 /**
	 * looks up for the shape or creates if not found
	 * @param e
	 * @return
	 */
	 public Shape lookUpOrCreate(OWLClassExpression e){
		  //looks up for the shape or creates if not found
//		 CBL::Changing the keys
//		  Shape sup = getShape(ExpressionManager.getReducedClassExpression(e));
		 Shape sup = getShape(Shape.getKey(e));
		  if (sup!= null){
			  return sup;
		  }
		  else {
			  VisLevel l = VisLevel.getLevelFromID(levelSet,1);
			  VisClass nVis = new VisClass(1, e, ExpressionManager.getReducedClassExpression(e), this);
			  l.addShape(nVis);
			  nVis.isAnonymous= true;
			  nVis.setHeight(nVis.calculateHeight());
			  nVis.setWidth(nVis.calculateWidth());
			  nVis.setVisLevel(l);		
			  return nVis;
			  
		  }	
		}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			
			HashSet<OWLClassExpression> topSet = new HashSet<>(); 
			topSet.add(getActiveOntology().getOWLOntologyManager().getOWLDataFactory().getOWLThing()); 
			this.buildReasonedGraph(getActiveOntology(), getReasoner(), topSet, isExpanded());
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// Release the latch to unblock the main thread
			latch.countDown();
		}
		updateProgressBarObserver(100);

	}
	
	
	/**
	 * Restarts node states so they're all visible and open
	 */
	public void showAll() {
		for (Entry<String,Shape> entry : shapeMap.entrySet()){
			Shape s = entry.getValue();
			if ((s.getState() == VisClass.CLOSED) || (s.getState() == VisClass.PARTIALLY_CLOSED)){
				s.open();
			}
			s.setVisible(true);
			
		}
		
	}
	
	

	/*-**************************************************************************************
	 * Graph Observer methods
	 *
	 ****************************************************************************************/
	
	/**
	 * Updates progress bar observer
	 * @param percent
	 */
	
	private void updateProgressBarObserver(int percent){
		setProgress(percent);
		for (Observer o: observers){
			if (o instanceof ProgressBarDialogThread){
				o.update(this, null);
			}
		}
	}

	/**
	 * Calculate progress from 0-70 upon number of shape classes created
	 */
	
    private void setProgressFromShapeNumber(){
        int prev = (int) ( 70.0f * ((float)getShapeMap().size()/
        		                 (float)getActiveOntology().getClassesInSignature(true).size()));
    	int progress = prev > 70 ? 70 : prev;
    	setProgress(progress);
    	updateProgressBarObserver(progress);
    }

	public void addObserver(Observer o){ observers.add(o);}
	
	/**
	 * Updates Observers added to the graph. In order to not waste performance, Observer type must be specified.
	 * (e.g. PROGRESSBAROBSERVER)
	 * @param observerType
	 */

	public void updateObservers(int observerType) {
		for (Observer ob : observers) {
			if ((observerType == VisConstants.PROGRESSBAROBSERVER) && (ob instanceof ProgressBarDialogThread)) {
				ob.update(this, null);
			} else if ((observerType == VisConstants.GENERALOBSERVER) && (ob instanceof VisGraphObserver)) {
				ob.update(this, null);
			}
		}
	}
}
