package sid.OntView2.common;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import openllet.aterm.pure.owl.FunBottomObjectProperty;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.xml.sax.SAXException;

import sid.OntView2.expressionNaming.NonGatheredClassExpressionsException;
import sid.OntView2.expressionNaming.SIDClassExpressionNamer;
import sid.OntView2.utils.ExpressionManager;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static sid.OntView2.utils.ExpressionManager.qualifyLabel;
import static sid.OntView2.utils.ExpressionManager.replaceString;

public class VisGraph implements Runnable{

	private int progress = 0;

    Hashtable<Integer, VisLevel>       levelSet;
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
    public Map<Integer, VisLevel> getLevelSet(){return levelSet;}
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
		levelSet 			= new Hashtable<>();
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
	public void buildReasonedGraph(OWLOntology activeOntology,OWLReasoner reasoner, Set<OWLClassExpression> rootCpts, boolean check) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {
		
		//It's important to set config constants before creating 
		//will have to move this elsewhere
		clearAll();
		set.stream().forEach(x->System.err.println("Build 1st set: "+x));
		OWLClass topClass = activeOntology.getOWLOntologyManager().getOWLDataFactory().getOWLThing(); 
		addVisClass(Shape.getKey(topClass), topClass, activeOntology, reasoner); 
		this.reasonedTraversal(activeOntology,reasoner, null, rootCpts);

		linkDefinitionsToDefinedClasses(activeOntology,reasoner);
		
		if (!VisConfig.APPLY_RENAMING_DEBUG_PURPOSES) {
			OWLDataFactory dFactory = activeOntology.getOWLOntologyManager().getOWLDataFactory();
			OWLClass topDetailedCpt = dFactory.getOWLThing(); 
			OWLClass bottomDetailedCpt = dFactory.getOWLNothing();  
			insertClassExpressions(activeOntology, reasoner, topDetailedCpt, bottomDetailedCpt);
		}
		
		createLevels(activeOntology); 
		arrangePos();
		VisLevel.shrinkLevelSet(levelSet);
		VisLevel.adjustWidthAndPos(levelSet);
		
		// here, we should add the
		// createConnections(vis,superSet,subSet,equivSet,reasoner,activeOntology);
		System.out.println("Disjoint and equivalence ... "); 

		for (Entry<String, Shape> entry : shapeMap.entrySet()){
			Shape shape = entry.getValue();
			if (shape instanceof VisClass) {
				shape.asVisClass().addAssertedDisjointConnectors();
				shape.asVisClass().addEquivalentConnectors();
			}
		}

		placeProperties(activeOntology,reasoner,set);
    	arrangePos();
		VisLevel.shrinkLevelSet(levelSet);
    	VisLevel.adjustWidthAndPos(levelSet);

    	System.out.println("Redundant ... "); 
    	removeRedundantConnector();
    	System.gc();
        reorder = new GraphReorder(this);
    	reorder.visualReorder();
    	adjustPanelSize((float) 1.0);
    	clearDashedConnectorList();
		updateXposition();
		paintframe.doKceOptionAction();

    	VisLevel.adjustWidthAndPos(getLevelSet());
    	paintframe.getParentFrame().loadSearchCombo();
    	paintframe.setStateChanged(true);
		Platform.runLater(paintframe.relaxerRunnable);
	}

	private void insertClassExpressions (OWLOntology activeOntology, 
											OWLReasoner reasoner, 
											OWLClassExpression startingPoint, 
											OWLClassExpression endPoint) {
		SIDClassExpressionNamer renamer = new SIDClassExpressionNamer(activeOntology, reasoner);
		try {
			renamer.gatherAllExpressionsFiltering();
		}
		catch (NonGatheredClassExpressionsException e) {
			System.err.println("Problem gathering anononymous classes"); 
			renamer.nullifyClassesToAdd();
		}
		
		// <CBL> Once we have built the "simple" reasoned graph, we 
		// have to add the elements in their appropriate position according to all the subsumption
		OWLDataFactory dFactory = activeOntology.getOWLOntologyManager().getOWLDataFactory();
		int i = 0; 
	
		for (OWLClassExpression ce: renamer.getClassesToAdd()) {
			
			if ( (startingPoint.isOWLThing() || subsumes(startingPoint, ce, activeOntology, reasoner, dFactory)) && 
					(endPoint.isOWLNothing() || subsumes(ce, endPoint, activeOntology, reasoner, dFactory )) ){
				System.out.println("Inserting ..."); 
				HashSet<OWLClassExpression> superset = new HashSet<>();
				superset.add(startingPoint); 
				addGatheredClassExpression(ce, superset, reasoner, activeOntology, dFactory);
				i++; 
				System.out.println(i+" out of "+renamer.getClassesToAdd().size()+" ... "); 
			}else {
				System.out.println("Skipping ..."); 
			}
			System.out.println(ce);
		}
	}
	
	private void addGatheredClassExpression(OWLClassExpression e, 
			HashSet<OWLClassExpression> superSet, 
			OWLReasoner reasoner, 
			OWLOntology activeOntology,
			OWLDataFactory dataFactory) {

		HashSet<Shape> newParents = new HashSet<>(); 
		for (OWLClassExpression currentParent: superSet) {
			Shape currentParentShape = getShapeFromOWLClassExpression(currentParent); 
			if (currentParentShape != null) {
				for (Shape currentChild: currentParentShape.asVisClass().getChildren()) {
					if (subsumes(currentChild.getLinkedClassExpression(), e, activeOntology, reasoner, dataFactory)) {
						newParents.add(currentChild);
					}
				}
			}
		}	
		if (!newParents.isEmpty()) {
			HashSet<OWLClassExpression> auxParents = new HashSet<>();  
			newParents.forEach(x->auxParents.add(x.getLinkedClassExpression()) ); 
			addGatheredClassExpression(e, auxParents, reasoner, activeOntology, dataFactory); 
		}
		else {
			HashSet<Shape> potentialChildren = new HashSet<>();
			for (OWLClassExpression currentParent: superSet) {
				Shape currentParentShape = getShapeFromOWLClassExpression(currentParent); 
				if (currentParentShape != null) {
					for (Shape currentChild: currentParentShape.asVisClass().getChildren()) {
						if (subsumes(e, currentChild.getLinkedClassExpression(), activeOntology, reasoner, dataFactory)) {
							potentialChildren.add(currentChild); 
						}
					}
				}
			}
			
			VisClass addedVis = addVisClass(e.toString(), e,activeOntology,reasoner);
			HashSet<OWLClassExpression> equivSet = new HashSet<>(); 
			equivSet.addAll(reasoner.getEquivalentClasses(e).getEntitiesMinusTop());

			HashSet<OWLClassExpression> auxParents = new HashSet<>();  
			superSet.forEach(x->auxParents.add(x) );

			// we have to make sure that the children do not subsume each other
			Set<Node<OWLClass>> directChildren = reasoner.getSubClasses(e, true).nodes().collect(Collectors.toSet());
			System.out.println("PotentialChildren before: "+potentialChildren.size()); 
			directChildren.forEach(x-> {
				Set<Shape> toRemove = new HashSet<>(); 
				for (Shape auxShape: potentialChildren) {
					if (subsumes(x.getRepresentativeElement(), auxShape.getLinkedClassExpression(), activeOntology, reasoner, dataFactory)) {
						toRemove.add(auxShape); 
					}
				}
				potentialChildren.removeAll(toRemove); 
			}); 
			System.out.println("PotentialChildren after: "+potentialChildren.size()); 
			
			System.out.println("directChildren before: "+directChildren.size()); 
			potentialChildren.forEach (x ->  {
				HashSet<Node<OWLClass>> toRemove = new HashSet<>(); 
				for (Node<OWLClass> auxExp: directChildren) {
					if (subsumes(x.getLinkedClassExpression(), auxExp.getRepresentativeElement(), activeOntology, reasoner, dataFactory)) {
						toRemove.add(auxExp); 
					}
				}
				directChildren.removeAll(toRemove); 
			}); 
			System.out.println("directChildren after: "+directChildren.size()); 
			
			HashSet<OWLClassExpression> auxChildren = new HashSet<>(); 
			potentialChildren.forEach(x->auxChildren.add(x.getLinkedClassExpression()) );
			directChildren.forEach(x->auxChildren.add(x.getRepresentativeElement()));
			
			createParentConnections(addedVis, auxParents); 
			createEquivConnections(addedVis, equivSet); 
			createChildConnections(addedVis,auxChildren);
		}
	}
	
	private void createLevels(OWLOntology activeOntology) {
		
	   OWLClassExpression topCE = activeOntology.getOWLOntologyManager().getOWLDataFactory().getOWLThing(); 
	   Set<VisClass> nextLevel = new HashSet<>(); 
	   nextLevel.add(getShapeFromOWLClassExpression(topCE).asVisClass());
	   int depthLevel = 0; 
	   while (!nextLevel.isEmpty()) {
		   System.out.println("Creating level "+depthLevel+"..."+nextLevel.size()); 
		   nextLevel.stream().forEach(x->System.out.println("including ... ("+x.getChildren().size()+") "+x.getLinkedClassExpression())); 
		   VisLevel vlevel;
	       int levelPosx = VisClass.FIRST_X_SEPARATION;
	       vlevel = VisLevel.getLevelFromID(levelSet, depthLevel);
	       if (vlevel==null) {
	    	   VisLevel auxlevel;
	    	   auxlevel = VisLevel.getLevelFromID(levelSet,depthLevel-1);
	    	   if (auxlevel != null) {
	    		   levelPosx  = auxlevel.getXpos()+auxlevel.getWidth()+30;
	    	   }
	    	   vlevel = new VisLevel(this,depthLevel, levelPosx);
	    	   levelSet.put(depthLevel, vlevel);
	       }
		   Set<OWLClassExpression> auxNextLevel = new HashSet<>(); 
		   for (VisClass s: nextLevel) {
			   vlevel.addShape(s);
			   for (Shape child: s.getChildren()) {
				   auxNextLevel.add(child.getLinkedClassExpression()); 
			   }
		   }
		   nextLevel.clear(); 
		   auxNextLevel.stream().forEach( x -> {
			   nextLevel.add(getShapeFromOWLClassExpression(x).asVisClass()); 
		   }); 
		   depthLevel++; 
	   }
	}
	
	
	private void updateXposition() {
		for ( Entry<String, Shape> entry : shapeMap.entrySet()){
			Shape shape = entry.getValue();

			if ((shape instanceof VisClass)){
				shape.asVisClass().setWidth(shape.asVisClass().calculateWidth());
				//shape.asVisClass().setHeight(shape.asVisClass().calculateHeight());
			}
		}
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
							// we add all the equivalences 
							// CBL: 27/8/2024 => We can add all the equivalences here, though it would be the assertions, not the actual 
							// 	equivalent classes -- OWLAPI considers any class with an equivalence as defined.
							if (definition.isAnonymous()) {
								// <CBL 25/9/13>
								// We also add the definition to the aliases handling
								shape.asVisClass().addDefinition(definition);
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
						c =	(VisClass) getShapeFromOWLClassExpression(oclass);
						c.properties.add(property.getIRI().toString());
						if (c.getPropertyBox() == null) 
							c.createPropertyBox();
						if (propertyMap.get(VisObjectProperty.getKey(property))!=null)
							continue;
						
					    VisObjectProperty v = c.getPropertyBox().add(property,range,activeOntology);	
					    if (v!=null){
					    	propertyMap.put(VisObjectProperty.getKey(property), v);
					    }
					}
				}
			}
		}
		VisPropertyBox.sortProperties(this);
		VisPropertyBox.buildConnections(this);
	}	
	
	private void placeChainProperties(OWLOntology activeOntology,OWLReasoner reasoner, HashSet<OWLClassExpression> set) {
		for ( OWLAxiom axiom: activeOntology.getAxioms(AxiomType.SUB_PROPERTY_CHAIN_OF, Imports.INCLUDED) ){
			OWLSubPropertyChainOfAxiom chain_axiom = (OWLSubPropertyChainOfAxiom) axiom;
			this.addChainProperty(VisObjectProperty.getKey(chain_axiom.getSuperProperty()),chain_axiom);
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
				VisDataProperty.addDomain(this, propertyDomainNodeSet, dataProperty, reasoner, activeOntology, dRange);
			}
			else {
				for (Node<OWLClass> o : propertyDomainNodeSet ){
					for (OWLClass oclass : o.getEntities()){
					    c =	(VisClass) getShapeFromOWLClassExpression(oclass);
						if (c.getPropertyBox() == null)
							c.createPropertyBox();
	//					CBL::Changing the keys
						if (dPropertyMap.get(VisDataProperty.getKey(dataProperty))!=null)
							continue;
						VisDataProperty v =c.getPropertyBox().add(dataProperty,dRange,activeOntology);
						if (v!=null){
//							CBL::Changing the keys
							dPropertyMap.put(VisDataProperty.getKey(dataProperty), v);
						}
					}
				}
			}

		}
	}

	private void reasonedTraversal(OWLOntology activeOntology, 
			OWLReasoner reasoner,
			Set<OWLClassExpression> previousCELevel, 
			Set<OWLClassExpression> currentCELevel){
		
		HashSet<OWLClassExpression> nextLevel = new HashSet<>(); 
		System.out.println("previousCELevel: "+((previousCELevel != null)?previousCELevel.size():"null")); 
		System.out.println("currentCELevel: "+currentCELevel.size()); 
		for (OWLClassExpression ce: currentCELevel) {
			System.out.println("Adding ..."+ce); 
			// all the Shapes in currentCELevel :: AlreadyCreated 
			// this implies that TOP must be created outside
			VisClass vis = getShapeFromOWLClassExpression(ce).asVisClass(); 
			
			HashSet<OWLClassExpression> equivSet = new HashSet<>();
			HashSet<OWLClassExpression> subSet = new HashSet<> ();
			reasoner.getSubClasses(ce, true).entities().forEach(subSet::add); 
			subSet.forEach(
					c -> {
						if (!c.isOWLNothing()) {
							nextLevel.add(c);				
						}
					}
			); 
			reasoner.getEquivalentClasses(ce).entities().forEach(equivSet::add);

			if (!subSet.isEmpty()) {
				createShapes(subSet);
				createChildConnections(vis, subSet); 
			}
			
			if (!equivSet.isEmpty()) {
				createShapes(equivSet);
				createEquivConnections(vis, equivSet);
				if (subSet.isEmpty()) {
					createChildConnections(vis,subSet); 
				}
			}
		}
		
		if (!nextLevel.isEmpty()) {
			System.out.println("---"); 
			System.out.println("currentCELevel: "+currentCELevel.size());
			System.out.println("nextLevel: "+nextLevel.size()); 
			reasonedTraversal(activeOntology, 
					reasoner,
					currentCELevel,
					nextLevel); 
		}
		else {
			addBottomNode(reasoner, activeOntology); 
		}
	}
	
	private void createShapes(Set<OWLClassExpression> ceToCreate) {
		for (OWLClassExpression ce: ceToCreate) {
			String auxKey  = Shape.getKey(ce);
			Shape auxValue = shapeMap.get(auxKey);
			if(auxValue != null){
				continue;
			}
			addVisClass(auxKey, ce, activeOntology, reasoner);
		}
	}

	private void addBottomNode(OWLReasoner reasoner,OWLOntology activeOntology) {
		Set<OWLClassExpression> superSet = new HashSet<>(); 
		reasoner.getSuperClasses(activeOntology.getOWLOntologyManager().getOWLDataFactory().getOWLNothing(), true).forEach(
				node->{
					node.entities().forEach( ce -> {
						superSet.add(ce); 
					}); 
				}); 
		reasoner.getBottomClassNode().forEach(e -> {
			Shape o = getShapeFromOWLClassExpression(e);
			VisClass auxVis = null; 
			 if (o==null){
				auxVis = addVisClass(e.asOWLClass().getIRI().toString(), e, activeOntology, reasoner);
				createParentConnections(auxVis, superSet); 
			 }
			for (OWLClassExpression exp : EntitySearcher.getEquivalentClasses(e, activeOntology).toList()){
				if (!(exp instanceof OWLClass)) {
					String auxKey = Shape.getKey(exp); 
					Shape auxValue = shapeMap.get(auxKey);
					if(auxValue == null){
			    		auxVis = addVisClass(auxKey, exp, activeOntology, reasoner);
			    		createParentConnections(auxVis, superSet); 
					}
				}
		     }
		});	 
	}
	
	private void createParentConnections(VisClass vis,Set<OWLClassExpression> superSet){

		for (OWLClassExpression e : superSet){
			System.out.println("connecting "+vis.getLinkedClassExpression() + " with " + e); 
			connect(vis,(VisClass) getShapeFromOWLClassExpression(e),connectorList);
		}
	}
	
	private void createEquivConnections(VisClass vis, Set<OWLClassExpression> equivSet){
		for (OWLClassExpression same : equivSet){
			// Just connect the anonymous definition to the visclass
			if ((vis.isDefined) && same.isAnonymous())
				connect(vis,getShapeFromOWLClassExpression(same).asVisClass(),connectorList);
		}
	}
	
	private void createChildConnections(VisClass vis, Set<OWLClassExpression> subSet){
		
		for (OWLClassExpression child : subSet) {
			Shape val = getShapeFromOWLClassExpression(child);
			if (val!= null){
				connect(val.asVisClass(),vis, connectorList);
			}	
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
	public Shape findShape2(Point2D p) {
		GraphicsContext g = paintframe.getGraphicsContext2D();
        for (Entry<String, Shape> e : shapeMap.entrySet()) {
            int x = e.getValue().getLeftCorner();
            int y = e.getValue().getTopCorner();
            int w = e.getValue().getWidth();
            int h = e.getValue().getHeight();

            if (isInShape(x,y,w,h,p) && e.getValue().visible){
				g.setStroke(Color.RED);
				g.strokeRect(x, y, w, h);
				return e.getValue();
			}
        }
	    return null;
	}

	public Shape findShape(Point2D p) {
		GraphicsContext g = paintframe.getGraphicsContext2D();
		int buttonMargin = 10; // Margen adicional para los botones laterales

		for (Entry<String, Shape> e : shapeMap.entrySet()) {
			int x = e.getValue().getLeftCorner();
			int y = e.getValue().getTopCorner();
			int w = e.getValue().getWidth();
			int h = e.getValue().getHeight();

			if (isInShape(x,y,w,h,p) && e.getValue().visible) {
				return e.getValue();
			}
		}
		return null;
	}

	private boolean isInShape(int x, int y, int w, int h, Point2D p) {
		int buttonMargin = 10; // Take into account the buttons size

		return (p.getX() >= x - buttonMargin) && (p.getX() <= x + w + buttonMargin) &&
				(p.getY() >= y ) && (p.getY() <= y +  h);
	}

	public VisClass addVisClass(String label, OWLClassExpression ce, OWLOntology activeOntology, OWLReasoner reasoner) {
		System.out.println("Adding "+label+" "+ce);
		String auxQLabel = ""; 
		VisClass vis = new VisClass(0, ce, label, this); 
		shapeMap.put(label, vis);
		if (ce instanceof OWLClass){
		   for (OWLAnnotation  an : EntitySearcher.getAnnotations(ce.asOWLClass(), activeOntology).toList() ){
			   if (an.getProperty().toString().equals("rdfs:label")){
				   vis.explicitLabel = an.getValue().toString();
				   vis.explicitLabel.replaceAll("\"", "");
				   vis.explicitLabel = replaceString(vis.explicitLabel);
				   auxQLabel = qualifyLabel(ce.asOWLClass(), vis.explicitLabel);
				   if (auxQLabel != null && !"null".equalsIgnoreCase(auxQLabel)) {
					   vis.explicitQualifiedLabel = auxQLabel;
				   }
				   else {
					   vis.explicitQualifiedLabel = vis.explicitLabel;
				   }
			   }
		   }
       }
	   vis.label = ExpressionManager.getReducedClassExpression(ce);
	   vis.visibleLabel = vis.label; 
	   auxQLabel = ExpressionManager.getReducedQualifiedClassExpression(ce); 
       if (auxQLabel != null && !"null".equalsIgnoreCase(auxQLabel))
    	   vis.qualifiedLabel = auxQLabel; 
       else 
    	   vis.qualifiedLabel = vis.label;

	   if (ce.isAnonymous()) {
           vis.isAnonymous = true;
	   }
	   if ((ce instanceof OWLClass) && (EntitySearcher.isDefined(ce.asOWLClass(), activeOntology) )) {
           vis.isDefined = true;
	   }
       if (reasoner != null){
    	   Node<OWLClass> equivClasses = reasoner.getEquivalentClasses(ce);
    	   equivClasses.entities().forEach(x-> {
    		   if (x.isOWLNothing()) vis.isBottom=true; 
    	   }); 
       }
       
       vis.setWidth(vis.calculateWidth());
       vis.setHeight(vis.calculateHeight());
	   
	   if (ce instanceof OWLClass){
    	   getQualifiedLabelMap().put(vis.label, ce.asOWLClass().getIRI().toString());
       }
	   return vis; 
	}
	
	
	public Set<Entry<String,Shape>> getClassesInGraph(){
		 return shapeMap.entrySet();
	 }
	
	public void clearDashedConnectorList(){
		dashedConnectorList.clear();
		for (Entry<String,Shape> entry : shapeMap.entrySet()) {
			Shape s = entry.getValue();
			s.inDashedConnectors.clear();
			s.outDashedConnectors.clear();
		}
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
				 dashLink(s, s, 1);
			 }
		 }
	}
	 
	/**
     * Searchs sublevels for nodes that are still visible/referenced
     * and adds a dashedLine connecting them
     */
	private void dashLink(Shape source,Shape current, int pathLength) {
		for (VisConnector c  : current.outConnectors){
			Shape currentSon = c.to;
			if (!currentSon.visible) {
				dashLink(source, currentSon, pathLength+1); 
			}
			else {
				if (pathLength>1) {
					VisConnector prevAdded = VisConnector.getConnector(dashedConnectorList, source, currentSon);
					if (prevAdded == null){
						dashedConnectorList.add(new VisConnectorDashed(source, currentSon));
						currentSon.inDashedConnectors.add(new VisConnectorDashed(source, currentSon));
						source.outDashedConnectors.add(new VisConnectorDashed(source, currentSon));
					}
				}
			}
		}
	}

	
	public void arrangePos() {

		boolean done = false;
		int i = 0;
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
						    	 levelSet.put(newl.getID(), newl);
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

	 // TODO: another candidate to check performance issues 
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
	 
	 
	 public	 boolean subsumes(OWLClassExpression b, OWLClassExpression a,OWLOntology ontology,
			OWLReasoner reasoner,OWLDataFactory dFactory ){
	 		OWLSubClassOfAxiom s = dFactory.getOWLSubClassOfAxiom(a,b);
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

	/**
	 * Initiates the traversal of all nodes in shapeMap and stores their descendants.
	 */
	private void storeDescendants() {
		Set<Shape> childrenToProcess = new HashSet<>(shapeMap.values());
		Set<Shape> visitedNodes = new HashSet<>();

		for (Shape child : childrenToProcess) {
			traverseAndStoreDescendants(child, visitedNodes);
		}
	}

	/**
	 * Traverses the descendants of the given node and stores them in the descendants set of the node.
	 */
	private void traverseAndStoreDescendants(Shape currentNode, Set<Shape> visitedNodes) {
		if (visitedNodes.contains(currentNode)) {
			return;
		}
		visitedNodes.add(currentNode);

		Set<Shape> allDescendants = new HashSet<>();
		for (VisConnector outConnector : currentNode.outConnectors) {
			Shape childNode = outConnector.to;
			allDescendants.add(childNode);
			traverseAndStoreDescendants(childNode, visitedNodes);
			allDescendants.addAll(childNode.asVisClass().descendants);
		}
		currentNode.asVisClass().descendants.addAll(allDescendants);
	}

	/**
	 * Initiates the traversal of all nodes in shapeMap and stores their ancestors.	 */
	private void storeAncestors() {
		Set<Shape> nodesToProcess = new HashSet<>(shapeMap.values());
		Set<Shape> visitedNodes = new HashSet<>();

		for (Shape node : nodesToProcess) {
			traverseAndStoreAncestors(node, visitedNodes);
		}
	}

	/**
	 * Traverses the ancestors of the given node and stores them in the ancestors set of the node.
	 */
	private void traverseAndStoreAncestors(Shape currentNode, Set<Shape> visitedNodes) {
		if (visitedNodes.contains(currentNode)) {
			return;
		}
		visitedNodes.add(currentNode);

		Set<Shape> allAncestors = new HashSet<>();
		for (VisConnector inConnector : currentNode.inConnectors) {
			Shape parentNode = inConnector.from;
			allAncestors.add(parentNode);
			traverseAndStoreAncestors(parentNode, visitedNodes);
			allAncestors.addAll(parentNode.asVisClass().ancestors);
		}
		currentNode.asVisClass().ancestors.addAll(allAncestors);
	}


	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			
			HashSet<OWLClassExpression> topSet = new HashSet<>();   
			topSet.add(getActiveOntology().getOWLOntologyManager().getOWLDataFactory().getOWLThing());  
			System.out.println("-->buildReasonedGraph"); 
			this.buildReasonedGraph(getActiveOntology(), getReasoner(), topSet, isExpanded());
			System.out.println("-->storeDescendants"); 
			storeDescendants();
			storeAncestors();
		} catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		} finally {
			// Release the latch to unblock the main thread
			System.out.println("-->releasing the latch "); 
			latch.countDown();
		}
	}
	
	
	/**
	 * Restarts node states so they're all visible and open
	 */
	public void showAll() {
		for (Entry<String,Shape> entry : shapeMap.entrySet()){
			Shape s = entry.getValue();
			if ((s.getState() == VisClass.CLOSED) || (s.getState() == VisClass.PARTIALLY_CLOSED)){
				s.openRight();
			}
			s.resetHiddenChildrenCount();
			s.setVisible(true);
		}
	}
	
	

	/*-**************************************************************************************
	 * Graph Observer methods
	 *
	 ****************************************************************************************/

	private final BooleanProperty progressBarObserver = new SimpleBooleanProperty();
	private final BooleanProperty generalObserver = new SimpleBooleanProperty();

	/** Adds a listener to the progress bar observer.
	 * The listener will be notified whenever the progress bar observer changes.
	 */
	public void addProgressBarObserver(ChangeListener<Boolean> listener) {
		progressBarObserver.addListener(listener);
	}

	/**
	 * Adds a listener to the general observer.
	 * The listener will be notified whenever the general observer changes.
	 */
	public void addGeneralObserver(ChangeListener<Boolean> listener) {
		generalObserver.addListener(listener);
	}

	/**
	 * Updates the appropriate observer based on the specified observer type.
	 * This method triggers either the progress bar observer or the general observer.
	 */
	public void updateObservers(int observerType) {
		if (observerType == VisConstants.PROGRESSBAROBSERVER) {
			progressBarObserver.set(!progressBarObserver.get());
		} else if (observerType == VisConstants.GENERALOBSERVER) {
			generalObserver.set(!generalObserver.get());
		}
	}
}
