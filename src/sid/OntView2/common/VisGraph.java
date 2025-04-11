package sid.OntView2.common;

import javafx.application.Platform;
import javafx.geometry.Point2D;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.xml.sax.SAXException;

import sid.OntView2.expressionNaming.NonGatheredClassExpressionsException;
import sid.OntView2.expressionNaming.SIDClassExpressionNamer;
import sid.OntView2.kcExtractors.KConceptExtractor;
import sid.OntView2.kcExtractors.KConceptExtractorFactory;
import sid.OntView2.kcExtractors.RDFRankConceptExtraction;
import sid.OntView2.utils.ExpressionManager;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static sid.OntView2.utils.ExpressionManager.qualifyLabel;
import static sid.OntView2.utils.ExpressionManager.replaceString;

public class VisGraph implements Runnable{

	private int progress = 0;

    Hashtable<Integer, VisLevel>       levelSet;
    ArrayList<VisConnector> connectorList;
	ArrayList<VisConnector> dashedConnectorList;
	
    final public Map<String, Shape>    shapeMap;
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
	private HashMap<String,String> qualifiedLabelMap;
	
    public HashMap<String, String> getQualifiedLabelMap(){return qualifiedLabelMap;}
	public List<VisConnector> getDashedConnectorList(){ return dashedConnectorList;}

	public List<VisConnector> getConnectorList(){ return connectorList;}
	public int getProgress(){return progress;}
    public int getZoomLevel(){return zoomLevel;}
    public void setZoomLevel(int z){zoomLevel=z;}
    public Map<String,Shape> getShapeMap() {return shapeMap;}
    public PaintFrame getPaintFrame(){return paintframe;}
    public Map<Integer, VisLevel> getLevelSet(){return levelSet;}
	public void setActiveOntology(OWLOntology pactiveOntology) {activeOntology = pactiveOntology;}
	public void setOWLClassExpressionSet(HashSet<OWLClassExpression> pset) { set = pset;}
	public void setCheck(boolean pcheck) {check = pcheck;	}
	public OWLOntology getActiveOntology(){return activeOntology;}
	public boolean isExpanded(){return check;}
	public void setReasoner(OWLReasoner preasoner) {reasoner = preasoner;}
	public OWLReasoner getReasoner(){return reasoner;}
	public boolean isCreated(){return created;}

	private CountDownLatch latch;
	public void setLatch(CountDownLatch latch) { this.latch = latch; }

	
	/**
	 * Progress bar criteria. From 0-70 % it will depend on the number of shapes added to the map
	 */
    public  VisGraph(PaintFrame pframe) {
		shapeMap     = new ConcurrentHashMap<>();
		propertyMap  = new HashMap<>();
		dPropertyMap = new HashMap<>();
		connectorList 		= new ArrayList<>();
		dashedConnectorList = new ArrayList<>();
		levelSet 			= new Hashtable<>();
		paintframe = pframe;
		qualifiedLabelMap = new HashMap<>();
	}

	/**
	 * To avoid possible inheritance problems
	 */
	public void clearShapeMap() {
		shapeMap.clear();
		propertyMap.clear();
		dPropertyMap.clear();
		connectorList.clear();
		dashedConnectorList.clear();
		levelSet.clear();
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
        return (lvl.getWidth()+lvl.getXpos()+VisConstants.WIDTH_MARGIN);
		//return Math.max(((lvl.getWidth()+lvl.getXpos()+VisConstants.WIDTH_MARGIN) - paintframe.screenWidth), 0);

	}
    public int getHeight() {
		int maxy=0;
		for (Entry<String, Shape> entry : shapeMap.entrySet()) {
            maxy = (Math.max(entry.getValue().getBottomCorner(), maxy));
		}
        return (maxy + VisConstants.HEIGHT_MARGIN);
		//return Math.max(((maxy + VisConstants.HEIGHT_MARGIN) - paintframe.screenHeight), 0);
    }
    
    public void adjustPanelSize(float factor){
		if (paintframe == null) {
			System.err.println("paintframe is null in adjustPanelSize method.");
			return;
		}
        double viewPortHeight = paintframe.scroll.getViewportBounds().getHeight();
        double viewPortWidth = paintframe.scroll.getViewportBounds().getWidth();

        int x = Math.max((int) (getWidth() * factor - viewPortWidth), 0);
        // viewPortHeight does not take into account TopPanel's height
        int y = Math.max((int) (getHeight() * factor - viewPortHeight - paintframe.nTopPanelHeight), 0);//
		paintframe.canvasHeight = y + paintframe.screenHeight;
		paintframe.canvasWidth = x + paintframe.screenWidth;

        // deja un espacio de 164px
		paintframe.scroll.setHmax(x);
		paintframe.scroll.setVmax(y);
    }
	
	/**
	 *  builds the complete graph
	 *  relies on wether it's expanded or not, and rearranging visual info
	 */
    public void buildReasonedGraph(OWLOntology activeOntology,OWLReasoner reasoner, Set<OWLClassExpression> rootCpts, boolean check) throws XPathExpressionException, ParserConfigurationException, SAXException, IOException {

        System.out.println("-->buildReasonedGraph");
        //It's important to set config constants before creating
        //will have to move this elsewhere
        clearAll();
        set.stream().forEach(x->System.err.println("Build 1st set: "+x));
        OWLClass topClass = activeOntology.getOWLOntologyManager().getOWLDataFactory().getOWLThing();
        // we start from a fresh graph, so it is safe to create OWLThing directly
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
        System.out.println("Disjointness ... ");
        for (Entry<String, Shape> entry : shapeMap.entrySet()){
            Shape shape = entry.getValue();
            if (shape instanceof VisClass) {
                shape.asVisClass().addAssertedDisjointConnectors();
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
        System.out.println("Visual reordering ... ");
        reorder.visualReorder();
        adjustPanelSize((float) 1.0);
        clearDashedConnectorList();
        updateXposition();
        System.out.println("Applying KCE Option if required ...");
        paintframe.doKceOptionAction();
        //showAll();
        VisLevel.adjustWidthAndPos(getLevelSet());
        paintframe.getParentFrame().loadSearchCombo();
        paintframe.setStateChanged(true);
        System.out.println("<--buildReasonedGraph");
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
			if (getShapeFromOWLClassExpression(ce) == null) {
				if ( (startingPoint.isOWLThing() || subsumes(startingPoint, ce, activeOntology, reasoner, dFactory)) && 
						(endPoint.isOWLNothing() || subsumes(ce, endPoint, activeOntology, reasoner, dFactory )) ){
					HashSet<OWLClassExpression> superset = new HashSet<>();
					superset.add(activeOntology.getOWLOntologyManager().getOWLDataFactory().getOWLThing()); 
					addGatheredClassExpression(ce, activeOntology.getOWLOntologyManager().getOWLDataFactory().getOWLThing(), reasoner, activeOntology, dFactory);
					i++; 
					if (i%5==0) 
						System.out.println(i+" out of "+renamer.getClassesToAdd().size()+" ... "); 
				}else {
					System.out.println("Skipping ..."); 
				}
			}
			else {
				System.out.println(ce + " already included in a shape"); 
			}
		}
	}
	
	private Set<Shape> exploreParent(OWLClassExpression e, 
													OWLClassExpression currentParent, 
													OWLReasoner reasoner, 
													OWLOntology activeOntology,
													OWLDataFactory dataFactory) {
		HashSet<Shape> result = new HashSet<> ();
		HashSet<Shape> auxChildren = new HashSet<>(); 
		Shape currentParentShape = getShapeFromOWLClassExpression(currentParent); 
		if (currentParentShape != null) {
			for (Shape currentChild: currentParentShape.asVisClass().getChildren()) {
				if (subsumes(currentChild.getLinkedClassExpression(), e, activeOntology, reasoner, dataFactory)) {
					auxChildren.add(currentChild);
				}
			}
		}
		if (auxChildren.isEmpty()) {
			result.add(currentParentShape); 
		}
		else {
			for (Shape currentChild: auxChildren) {
				result.addAll(exploreParent(e, currentChild.asVisClass().getLinkedClassExpression(), reasoner, activeOntology, dataFactory)); 
			}
		}
		return result; 
	}
	
	private void addGatheredClassExpression(OWLClassExpression e, 
			OWLClassExpression entryPointParent, 
			OWLReasoner reasoner, 
			OWLOntology activeOntology,
			OWLDataFactory dataFactory) {

		HashSet<Shape> directParents = new HashSet<>();
		directParents.addAll(exploreParent(e, entryPointParent, reasoner, activeOntology, dataFactory)); 
		
		// We check for the equivalences 
		for (Shape directParent: directParents) {
			if (subsumes(e, directParent.asVisClass().getLinkedClassExpression(), activeOntology, reasoner, dataFactory)) {
				System.out.println("equivalence detected in a gathered anonymous expression: "); 
				System.out.println("\t gathered: "+e); 
				System.out.println("\t prevExp: "+directParent.asVisClass().getLinkedClassExpression()); 
				directParent.asVisClass().addEquivalentExpression(e);
				return; 
			}
		}
		
		// If we haven't returned so far, we must check which children must be connected 
		HashSet<Shape> potentialChildren = new HashSet<>();
		for (Node<OWLClass> subclassNode: reasoner.getSubClasses(e, true)) {
			for (OWLClass subclass: subclassNode) {
				Shape aux = getShapeFromOWLClassExpression(subclass); 
				if(aux != null) {
					potentialChildren.add(aux); 
				}
			}
		}
		
		for (Shape currentParent: directParents) {
			for (Shape currentChild: currentParent.asVisClass().getChildren()) {
				if (subsumes(e, currentChild.getLinkedClassExpression(), activeOntology, reasoner, dataFactory)) {
					potentialChildren.add(currentChild); 
				}
			}
		}
		
		VisClass addedVis = addVisClass(e.toString(), e,activeOntology,reasoner);
		
		createParentConnections(addedVis, directParents.stream()
														.map(x->x.getLinkedClassExpression())
														.collect(Collectors.toSet())); 
		createChildConnections(addedVis, potentialChildren.stream()
															.map(x->x.getLinkedClassExpression())
															.collect(Collectors.toSet()));
		
	}
	
	private void createLevels(OWLOntology activeOntology) {
		
	   OWLClassExpression topCE = activeOntology.getOWLOntologyManager().getOWLDataFactory().getOWLThing(); 
	   Set<VisClass> nextLevel = new HashSet<>(); 
	   nextLevel.add(getShapeFromOWLClassExpression(topCE).asVisClass());
	   int depthLevel = 0; 
	   while (!nextLevel.isEmpty()) {
		   System.out.println("Creating level "+depthLevel+"..."+nextLevel.size()); 
		   nextLevel.stream().forEach(x-> {System.out.println("including ... ("+x.getChildren().size()+") "+x.getLinkedClassExpression()); 
		   				x.getChildren().forEach (y -> System.out.println("\t"+y.getLabel())); 
		   }); 
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
                if (shape.asVisClass().getIndicatorSize() == -1) {
                    shape.asVisClass().setMaxSizeHiddenNodesIndicator();
                }
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
	}
	
	
	/**
	 * it links definitions to defined classes.
	 *  Note that this must be done after having created the reasoned graph with the named classes 
	 */
	private void linkDefinitionsToDefinedClasses(OWLOntology activeOntology,OWLReasoner reasoner) {
		for (Entry<String, Shape> entry : shapeMap.entrySet()){
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
								shape.asVisClass().addEquivalentExpression(definition);
								shapeMap.put(Shape.getKey(definition), shape); 
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


	/**
	 * Traversal of the reasoned graph 
	*/
	private void reasonedTraversal(OWLOntology activeOntology, 
			OWLReasoner reasoner,
			Set<OWLClassExpression> previousCELevel, 
			Set<OWLClassExpression> currentCELevel){
		
		HashSet<OWLClassExpression> nextLevel = new HashSet<>(); 
		System.out.println("previousCELevel: "+((previousCELevel != null)?previousCELevel.size():"null")); 
		System.out.println("currentCELevel: "+currentCELevel.size()); 
		for (OWLClassExpression ce: currentCELevel) {
			// all the Shapes in currentCELevel :: AlreadyCreated 
			// this implies that TOP must be created outside
			VisClass vis = getShapeFromOWLClassExpression(ce).asVisClass(); 
			HashSet<OWLClassExpression> subSet = new HashSet<> ();
			reasoner.getSubClasses(ce, true).entities().forEach(subSet::add); 
			subSet.forEach(
					c -> {
						if (!c.isOWLNothing()) {
							nextLevel.add(c);				
						}
					}
			); 
			if (!subSet.isEmpty()) {
				createShapes(subSet);
				createChildConnections(vis, subSet); 
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
			if(getShapeFromOWLClassExpression(ce) != null){
				continue;
			}
			addVisClass(Shape.getKey(ce), ce, activeOntology, reasoner);
		}
	}

	private void addBottomNode(OWLReasoner reasoner,OWLOntology activeOntology) {
		reasoner.getBottomClassNode().forEach(e -> {
			Shape o = getShapeFromOWLClassExpression(e);
			 if (o==null){
				addVisClass(e.asOWLClass().getIRI().toString(), e, activeOntology, reasoner); 
			 }
			for (OWLClassExpression exp : EntitySearcher.getEquivalentClasses(e, activeOntology).toList()){
		    	 if (!(exp instanceof OWLClass) && (shapeMap.get(Shape.getKey(exp))==null)){
		    		addVisClass(Shape.getKey(exp), exp, activeOntology, reasoner);  
		    	 }
		     }
		});	 
	}
	
	private void createParentConnections(VisClass vis,Set<OWLClassExpression> superSet){

		for (OWLClassExpression e : superSet){
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
		return (VisClass) shapeMap.get(Shape.getKey(o)); 
	}
	
	
	/**
	 *  If there's a visible shape in that point this will return a reference to it and null otherwise
	 *  @return shape or null if not found
	 */
	public Shape findShape(Point2D p) {
		for (Entry<String, Shape> e : shapeMap.entrySet()) {
			int x = e.getValue().getLeftCorner();
			int y = e.getValue().getTopCorner();
			int w = e.getValue().getWidth();
			int h = e.getValue().getHeight();

			if (isInShape(e.getValue(),x,y,w,h,p) && e.getValue().visible) {
				return e.getValue();
			}
		}
		return null;
	}

	private boolean isInShape(Shape shape, int x, int y, int w, int h, Point2D p) {
        // Take into account the buttons size
        int buttonMarginLeft = shape.canShowFunctionalityLeft() ? VisConstants.BUTTON_SIZE : 0;
        int buttonMarginRight = shape.canShowFunctionalityRight() ? VisConstants.BUTTON_SIZE : 0;

		return (p.getX() >= x - buttonMarginLeft) && (p.getX() <= x + w + buttonMarginRight) &&
				(p.getY() >= y ) && (p.getY() <= y +  h);
	}


	/**
	 * Creates a new visclass instance and adds it to the set(hashmap) of visclasses
	 * Creates or adjusts new Vislevels width
	 */
	public VisClass addVisClass(String label, OWLClassExpression ce, OWLOntology activeOntology, OWLReasoner reasoner) {

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
    		   if (!x.equals(ce)) {
    			   vis.addEquivalentExpression(x);
    			   shapeMap.put(Shape.getKey(x), vis);
    			   // to make it appear in the SearchCombo
    			   if (x instanceof OWLClass) {    				   
    				   getQualifiedLabelMap().put(ExpressionManager.getReducedClassExpression(x), x.asOWLClass().getIRI().toString());
    			   }
    		   }
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
	
	 public Shape getShapeFromOWLClassExpression (OWLClassExpression e){
		String key = Shape.getKey(e); 
		Shape retShape = shapeMap.get(key);
		return retShape;
	 }

	 // TODO: another candidate to check performance issues 
	 private void removeRedundantConnector(){
	 //removes connectors that are implied by others
		 OWLReasoner reasoner = paintframe.getReasoner();
		 OWLOntology ontology = paintframe.getOntology();
		 HashSet<Shape> parents = new HashSet<Shape>();
		 for (Entry<String,Shape> entry : shapeMap.entrySet()){
			 System.out.println("Processing "+entry.getValue().asVisClass().getLabel()); 
			 Shape shape = entry.getValue();
			 for (VisConnector in : shape.inConnectors){
				 System.out.println("\t"+in.from.asVisClass().getLabel()+"-->"+in.to.asVisClass().getLabel()); 
				 parents.add(in.from);
			 }
			 remove(parents,shape,reasoner,ontology);
			 parents.clear();
			 
		 }
		 System.out.println("--> Exit redundancy removal ... "); 
		 
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
		 Shape OWLThingShape = getShapeFromOWLClassExpression(dFactory.getOWLThing()); 
		 //if a subsumes b  remove conector(a,son), as it's implicit
		 for (Shape pi : parents){
			 for (Shape pj : parents){
				 if (pi!=pj){
					 if (existSubsumptionPath(pi,pj, OWLThingShape)) {
						 System.out.println(pi.getLabel()+" ==> "+pj.getLabel()); 
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
					 
				 }
			 }
		 }
	 }	 
	 private boolean existSubsumptionPath(Shape potentialSon, Shape potentialParent, Shape OWLThingShape) {
		 boolean result = false; 
		 for (VisConnector nextLevelConnector: potentialSon.inConnectors) {
			if (nextLevelConnector instanceof VisConnectorIsA) {
				if (nextLevelConnector.from == potentialParent) {
					return true; 
				}
				else {
					if (nextLevelConnector.from != OWLThingShape) {   
						result = result || existSubsumptionPath(nextLevelConnector.from, potentialParent, OWLThingShape); 
					}
				}
			}
			// early termination if at any time this is evaluated to true
			if (result) break; 
		 }
		 return result; 
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
            if (childNode.asVisClass().isBottom) continue;

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

    /**
     * Stores the ordered descendants and ancestors for each shape based on RDFRank.
     */
    public void storeOrderedDescendantsByRDFRank() {
        for (Shape shape : shapeMap.values()) {
            Set<Shape> orderedDescendants = new LinkedHashSet<>();
            List<Shape> descendantsListLR = new ArrayList<>();

            for (Shape candidate : paintframe.orderedShapesByRDF) {
                if (shape.asVisClass().descendants.contains(candidate)) {
                    orderedDescendants.add(candidate);
                    descendantsListLR.add(candidate);
                }
            }

            descendantsListLR.sort(Comparator.comparingInt(s -> s.depthlevel));

            shape.asVisClass().orderedDescendants = orderedDescendants;
            shape.asVisClass().orderedDescendantsByLevel = new LinkedHashSet<>(descendantsListLR);
            shape.asVisClass().orderedDescendantsByLevelBottomTop = reorderListBottomTopImportance(shape.asVisClass().orderedDescendantsByLevel);
            shape.asVisClass().orderedDescendantsByLevelLeastImportant = reorderListLessImportant(shape.asVisClass().orderedDescendantsByLevel);
        }
    }

    /**
     * Reorders a collection of shapes by reversing their level order (from highest to lowest),
     * while preserving the descending order of importance within each level.
     */
    private Set<Shape> reorderListBottomTopImportance(Collection<Shape> shapes) {
        Map<Integer, List<Shape>> shapesByLevel = new TreeMap<>(Collections.reverseOrder());
        for (Shape shape : shapes) {
            shapesByLevel.computeIfAbsent(shape.depthlevel, k -> new ArrayList<>()).add(shape);
        }

        Set<Shape> reordered = new LinkedHashSet<>();
        for (List<Shape> shapesGroup : shapesByLevel.values()) {
            reordered.addAll(shapesGroup);
        }
        return reordered;
    }

    /**
     * Reorders a collection of shapes by maintaining the original level order while reversing
     * the importance order within each level (from least important to most important).
     */
    private Set<Shape> reorderListLessImportant(Collection<Shape> shapes) {
        Map<Integer, List<Shape>> shapesByLevel = new HashMap<>();
        for (Shape shape : shapes) {
            shapesByLevel.computeIfAbsent(shape.depthlevel, k -> new ArrayList<>()).add(shape);
        }

        Set<Shape> reversedShapes = new LinkedHashSet<>();
        for (List<Shape> shapesGroup : shapesByLevel.values()) {
            Collections.reverse(shapesGroup);
            reversedShapes.addAll(shapesGroup);
        }
        return reversedShapes;
    }

    /**
     * Gets the shapes ordered by RDFRank.
     */
    public void getShapeOrderedByRDFRank() {
        KConceptExtractor extractor = KConceptExtractorFactory.getInstance(VisConstants.RDFRANKCOMBOOPTION1, null);
        if (extractor instanceof RDFRankConceptExtraction rdfExtractor) {
            paintframe.orderedShapesByRDF = rdfExtractor.getShapesOrderedByRDFRank(activeOntology, this, shapeMap.size());
            storeOrderedDescendantsByRDFRank();
        }
    }


	@Override
	public void run() {
		try {
			
			HashSet<OWLClassExpression> topSet = new HashSet<>();   
			topSet.add(getActiveOntology().getOWLOntologyManager().getOWLDataFactory().getOWLThing());  
			System.out.println("-->buildReasonedGraph"); 
			this.buildReasonedGraph(getActiveOntology(), getReasoner(), topSet, isExpanded());
			System.out.println("-->storeDescendants"); 
			storeDescendants();
			storeAncestors();
            getShapeOrderedByRDFRank();
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
				for (VisConnector connector : s.inConnectors) connector.show();
                for (VisConnector connector : s.outConnectors) connector.show();
			}
			s.resetHiddenChildrenCount();
			s.setVisible(true);
		}
	}
}
