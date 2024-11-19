package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Font;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Text;

import org.apache.jena.base.Sys;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import sid.OntView2.expressionNaming.SIDClassExpressionNamer;
import sid.OntView2.utils.ExpressionManager;

import java.util.*;

public class VisClass extends Shape {
	
    //Graphic
	public static final int FIRST_X_SEPARATION = 200;
	public static final int RELATIVE_POS = 0;
	public static Color color;
	
	String   label;
//	String   mapKey;
	private String   classExpressionFragment;
	OWLClassExpression linkedClassExpression; 
	HashSet<OWLObjectProperty> inherited;
	HashSet<OWLDataProperty>  dInherited;
	HashSet<OWLObjectProperty> aplicable;
	HashSet<OWLDataProperty> dAplicable;
	ArrayList<Shape>   children;
	ArrayList<Shape>   parents;
	ArrayList<VisConnectorDisjoint> disjointList;
	ArrayList<VisConnectorEquiv> equivList;
	ArrayList<String>  properties; // those that have this class as its domain  
	VisPropertyBox propertyBox;
	// <CBL 24/9/13>: Theoretically, one term can have more than one definition
	ArrayList<OWLClassExpression> definitions;
	
	String  explicitLabel = "";
	boolean isAnonymous;
    boolean isDefined   = false;
    boolean isBottom    = false;
    int     currentHeight, currentWidth = 0;

    int     propertyBoxWidth = 0;
    
    // 17-01-2013
    // CBL: Added the qualified label fields to store the 
    // labels with the translated namespace 
    
    String qualifiedLabel = "";  
    String explicitQualifiedLabel = ""; 
    
    // <CBL 24/9/13> 
    // we have to do the same with the definitions 
    // associated to the class
    // we have the four variants: normal, qualified, labels and qualified labels
    ArrayList<String> visibleDefinitionLabels;
    ArrayList<String> definitionLabels; 
    ArrayList<String> explicitDefinitionLabels; 
	ArrayList<String> qualifiedDefinitionLabels;
	ArrayList<String> explicitQualifiedDefinitionLabels;
	// </CBL>
    
    String visibleLabel ="";

	boolean qualifiedRendering = false; 
	boolean labelRendering = false; 
	
	int tabSize = 15; 
	
	public String getClassExpressionFragment (){
		return classExpressionFragment == null ? getLinkedClassExpression().asOWLClass().getIRI().getFragment() :
												 classExpressionFragment;
	}
    
	public boolean isAnonymous(){return isAnonymous;}
	public ArrayList<Shape> getChildren() {return children;}
 	
	public VisClass(int par_depthlevel,OWLClassExpression o, String plabel, VisGraph pgraph) {
		
		super();
		depthlevel = par_depthlevel;
        setPosX(0); 
        //first position is irrelevant after rearranging 
		setPosY((int)(Math.random()*600));
		graph     = pgraph;
		setHeight(0); 
		setWidth (0);
		currentHeight= getHeight();
		linkedClassExpression = o;
		this.label= plabel;
		this.visibleLabel = plabel; 
		connectionPointsL = new Point2D(posx,posy+5);
		connectionPointsR = new Point2D(posx+getWidth(),posy+5);
		children    = new ArrayList<>();
        parents     = new ArrayList<>();
        properties  = new ArrayList<>();
        
        //<CBL 24/9/13> Added the initialization in the constructor
        definitions = new ArrayList<>();
        visibleDefinitionLabels = new ArrayList<String>(); 
      
	}

	@Override
	public String getLabel() { return label; }
	
	ArrayList<VisConnectorDisjoint> getDisjointConnectors(){
		if (disjointList==null) {
			disjointList = new ArrayList<>();
		}
		return disjointList;
	}
	
	public ArrayList<VisConnectorEquiv> getEquivConnectors(){
		if (equivList==null) {
			equivList = new ArrayList<>();
		}	
		return equivList;
	}
	
	private Set<OWLDisjointClassesAxiom> getDisJointClassesAxioms(){
		  OWLOntology ontology = graph.paintframe.getOntology();
		  if (this.getLinkedClassExpression() instanceof OWLClass)
		      return ontology.getDisjointClassesAxioms((OWLClass) this.getLinkedClassExpression());
		  return null;
	}
	
	private Set<OWLClass> getDisjointClasses() {
		OWLReasoner reasoner = graph.paintframe.getReasoner();
		return reasoner.getDisjointClasses(this.getLinkedClassExpression()).getFlattened(); 
		 
	}
	
	// <CBL 25/9/13> 
	// we process the disjointness axioms also to check 
	// whether someone has explicitly asserted the disjointness to Thing or Bottom
	
	public void addAssertedDisjointConnectors() {
		if (getDisJointClassesAxioms()!=null){
			for (OWLDisjointClassesAxiom axiom: getDisJointClassesAxioms()){
				for (OWLClassExpression e : axiom.getClassExpressions()){
					if(e != this.getLinkedClassExpression()){
						if (graph.getVisualExtension(e)!=null){
							addDisjointConnector(graph.getVisualExtension(e));
						}
					}
				}
		    }	 
		}
	}

	public void addDisjointConnector(Shape dst){
		boolean already = Objects.equals(this.getLabel(), dst.getLabel());
        if (!already){
			getDisjointConnectors().add(new VisConnectorDisjoint(this, dst));
		}
	}
	
	public void addEquivalentConnectors(){
		  OWLReasoner reasoner = graph.paintframe.getReasoner();
		  
		  // <CBL 25/9/13> 
		  // at first sight, if a concept is equivalent to Thing, it should be expressed 
		  // removed the getEntitiesMinusTop()
		  for(OWLClass  c : reasoner.getEquivalentClasses(this.getLinkedClassExpression())){
			  if (c!= this.getLinkedClassExpression()){
				  VisClass v = graph.getVisualExtension(c);
				  if (((v.isDefined)&&(this.isAnonymous)) || 
				  			((this.isDefined)&&(v.isAnonymous))){
					  continue;
				  }	 
				  HashSet<Shape> d = null;
			   if (!VisConnectorEquiv.accesible(this, v,d)){
					  addEquivConnector(v);
					  v.addEquivConnector(this);
				  }  
			  }

			  
		  }
	}
	
	public void addEquivConnector(Shape dst){
		if ((dst==this) ||  (VisConnectorEquiv.getConnector(getEquivConnectors(), this, dst)!=null))
			return;
		getEquivConnectors().add(new VisConnectorEquiv(this,dst));
	
	}
	
	private Font defFont;
	private Font boldFont;
	private Font getDefinedClassFont(){
		if (defFont==null)
			defFont = Font.font("DejaVu Sans", FontWeight.BOLD, 10);
		return defFont;
	}
	
	private Font getBoldFont(){
		if (boldFont==null)
			boldFont= Font.font("DejaVu Sans", FontWeight.BOLD, 10);
		return boldFont;
	}


	public void drawShape(GraphicsContext g) {
		if (g == null){
			return;
		}
		int x, y;
		int roundCornerValue = 10;

		x = posx + 1;
        y = posy;

        Font oldFont=g.getFont();
	    if (this.isDefined) {
	        g.setFont(getDefinedClassFont());
	    }
	    else {
	    	g.setFont(getBoldFont());
	    }

		Text textNode = new Text();
		textNode.setFont(g.getFont());
		double fontHeight = textNode.getBoundsInLocal().getHeight();
		double ascent = textNode.getBaselineOffset() + 5.5;
	    
	    if (currentHeight == 0) {
	    	if (!isAnonymous && !isDefined) {
	    		currentHeight = getHeight();
	    	}
	    	else {
				currentHeight = calculateHeight();
			}
			setWidth(calculateWidth());
		}

	    if (visible){

			Color mini = Color.rgb(224, 224, 224);
			Color lightgray = Color.rgb(234, 234, 234);
			Color lightBlue = Color.rgb(212, 238, 247);
			Color lightGreen = Color.rgb(212, 247, 212);

			int propertySpace = (propertyBox != null) ? 20 : 0;

	    	if (!isDefined) {
	    		// CBL if it is not defined, we use the previous representation
		    	if (!isAnonymous)
		    		g.setFill(lightgray);
		    	else
		    		g.setFill(Color.WHITE);

				g.fillRoundRect(x - (double) getWidth()/2, y - (double) currentHeight/2, getWidth(), currentHeight, roundCornerValue, roundCornerValue);
			    g.setStroke(Color.BLACK);
			    if (isBottom) {
			    	g.setStroke(Color.RED);
			    }	
		 
			    //rectangle
				g.strokeRoundRect(x - (double) getWidth()/2, y - (double) currentHeight/2,  getWidth()-1, currentHeight-1, roundCornerValue, roundCornerValue);
			    g.setFill(Color.BLACK);
				// Square for properties
				if (propertyBox != null) {
					propertyDraw(g, x, y, roundCornerValue, lightBlue);
				}
				if (!getDisjointConnectors().isEmpty()) {
					disjointDraw(g, x, y, roundCornerValue, Color.LIGHTYELLOW);
				}
			    g.setFill(Color.BLACK);
	
			    if (!isAnonymous) {
					g.fillText(visibleLabel, x - (double) (getWidth())/2 + 10 + propertySpace, (y -(double) (currentHeight - 4) / 2) + ascent);
			    }
			    else {
					drawFormattedString(g, visibleLabel, x - (getWidth() - 10)/2,  (int) ((y - (currentHeight - 4) / 2) + ascent), (int) fontHeight);
			    }
	    	}
	    	else {
	    		if (!label.startsWith(SIDClassExpressionNamer.className)) {
		    		// CBL: the new definitions representation
		    		g.setFill(lightGreen);
		    		g.fillRect(x - (double) getWidth()/2, y- (double) currentHeight /2, getWidth(), currentHeight);
					g.setStroke(isBottom ? Color.RED : Color.BLACK);
		    		g.strokeRect(x - (double) getWidth()/2, y - (double) currentHeight /2, getWidth()-1, currentHeight);

		    		// now => the rectangle for the name of the concept
		    		g.setFill(lightgray);
		    		g.fillRect(x - (double) getWidth()/2+5, y- (double) currentHeight /2 - 5, getWidth()-10, fontHeight+15);
					g.setStroke(isBottom ? Color.RED : Color.BLACK);
		    		g.strokeRect(x - (double) getWidth()/2+5, y - (double) currentHeight /2 - 5,  getWidth()-10, fontHeight+14);

		    		// this is the name of the concept
					g.setFill(Color.BLACK);
		    		g.setFont(getDefinedClassFont()); 
		    		g.fillText(visibleLabel, x - (double) (getWidth() - 16) /2 + 5 + propertySpace, (y - (double) (currentHeight - 4) /2) - 6 + ascent);

		    		g.setFont(getBoldFont());
		    		double auxY = y - ((double) currentHeight /2) + (fontHeight +5) + 2;
		    		if (visibleDefinitionLabels != null) {
		    			for (String auxDefString: visibleDefinitionLabels) {
							drawFormattedString(g, auxDefString, x - (getWidth() - 12) / 2, (int) (auxY + ascent)+5, (int) fontHeight);
							auxY += (countLines(auxDefString)*fontHeight) + 5;
		    			}
		    		}

					if (propertyBox != null) {
						propertyDraw(g, x + 5, y - 5, roundCornerValue, lightBlue);
					}
					if (!getDisjointConnectors().isEmpty()) {
						disjointDraw(g, x, y - 5, roundCornerValue, Color.LIGHTYELLOW);
					}
					g.setFill(Color.BLACK);
				}
	    		else {
	    			// CBL: it is an auxiliar definition 
	    			// CBL if it is not defined, we use the previous representation

					g.setFill(Color.WHITE);
				    g.fillRoundRect(x - (double) getWidth()/2, y - (double) currentHeight /2, getWidth(), currentHeight, roundCornerValue, roundCornerValue);
				    g.setStroke(Color.BLACK);
				    if (isBottom) {
				    	g.setStroke(Color.RED);
				    }	
			 
				    //rectangle
				    g.strokeRoundRect(x - (double) getWidth()/2, y - (double) currentHeight /2,  getWidth()-1, currentHeight-1, roundCornerValue, roundCornerValue);
				    g.setFill(Color.BLACK);
				    if (propertyBox!=null){
						propertyDraw(g, x, y, roundCornerValue, lightBlue);
						propertySpace += 5;
				    }
					if (!getDisjointConnectors().isEmpty()) {
						disjointDraw(g, x, y, roundCornerValue, Color.LIGHTYELLOW);
					}

				    g.setStroke(Color.BLACK);
				    g.setFont(getBoldFont()); 
		    		double auxY = (y - (double) (currentHeight - 4) /2);
		    		if (visibleDefinitionLabels != null) {
		    			for (String auxDefString: visibleDefinitionLabels) {
							g.setFill(Color.BLACK);
							drawFormattedString(g, auxDefString, x - (getWidth() - 10) / 2 + propertySpace, (int) (auxY + ascent)-1, (int) fontHeight);
							auxY += (countLines(auxDefString)*fontHeight) + 5;
		    			}
		    		}
	    		}
	    	}

			if(childrenHidden){
				drawHiddenNodesIndicator(g, 23, getHiddenChildrenCount(), posx, posy);
			}

		    if (!children.isEmpty() && (outConnectors!=null) &&(!outConnectors.isEmpty())){
		    	switch (this.getState()) {
		    	
		    	   case Shape.PARTIALLY_CLOSED :
		    		   g.setFill(mini);
		   	           g.fillRect(x + (double) getWidth()/2, y - 10, 10, 10);
		   	           g.setStroke(Color.BLACK);
		   	           g.strokeRect(x + (double) getWidth()/2, y - 10, 10, 10);
		   	           g.strokeLine(x + (double) getWidth()/2 + 2, y - 5, x + (double) getWidth()/2 + 8, y - 5);
		   	           g.strokeLine(x + (double) getWidth()/2 + 5, y - 8, x + (double) getWidth()/2 + 5, y - 3);
		   	           // open
		   	           g.setFill(mini);
		   	           g.fillRect(x + (double) getWidth()/2, y, 10, 10);
		   	           g.setStroke(Color.BLACK);
		   	           g.strokeRect(x + (double) getWidth()/2, y, 10, 10);
		   	           g.strokeLine(x + (double) getWidth()/2 + 2, y + 5, x + (double) getWidth()/2 + 8, y + 5);
		    		   break;
		    		   
		    	   case CLOSED : 
		    		   g.setFill(mini);
		 	           g.fillRect(x + (double) getWidth()/2, y - 10, 10, 10);
		 	           g.setStroke(Color.BLACK);
		 	           g.strokeRect(x + (double) getWidth()/2, y - 10, 10, 10);
		 	           g.strokeLine(x + (double) getWidth()/2 + 2, y - 5, x + (double) getWidth()/2 + 8, y - 5);
		 	           g.strokeLine(x + (double) getWidth()/2 + 5, y - 8, x + (double) getWidth()/2 + 5, y - 3);
		    		   break;
	
		    	   case  Shape.OPEN :
		    		   g.setFill(mini);
		 	           g.fillRect(x + (double) getWidth()/2, y, 10, 10);
		 	           g.setStroke(Color.BLACK);
		 	           g.strokeRect(x + (double) getWidth()/2, y, 10, 10);
		 	           g.strokeLine(x + (double) getWidth()/2 + 2, y + 5, x + (double) getWidth()/2 + 8, y + 5);
		    		   break;
		    		   
		    	   default :
		    		   break;
		       }
			}
		    /*
		    for (VisConnectorEquiv equ: getEquivConnectors()){
			    equ.draw(g);
		    }*/
		}
		g.setFont(oldFont);
	}

	private void drawHiddenNodesIndicator(GraphicsContext g, int totalNodes, int hiddenNodes, int x, int y) {

		double width = getWidth();
		double height = 10;
		double rectX = x - width / 2;
		double rectY = y - currentHeight / 2 - height - 5;

		double hiddenPercentage = (double) hiddenNodes / totalNodes;
		double visiblePercentage = 1.0 - hiddenPercentage;

		double hiddenWidth = width * hiddenPercentage;
		double visibleWidth = width * visiblePercentage;

		// hidden nodes
		g.setFill(Color.MEDIUMSEAGREEN);
		g.fillRect(rectX, rectY, hiddenWidth, height);

		// visible nodes
		g.setFill(Color.LIGHTGRAY);
		g.fillRect(rectX + hiddenWidth, rectY, visibleWidth, height);

		g.setStroke(Color.BLACK);
		g.strokeRect(rectX, rectY, width, height);
	}


	private void propertyDraw(GraphicsContext g, int x, int y, int roundCornerValue, Color colorP) {
		g.setFill(colorP);
		g.fillRoundRect(x - (double) getWidth()/2 + 5, y - (double) currentHeight / 2 + 6, 19, 14, roundCornerValue, roundCornerValue);
		g.setFill(Color.BLACK);
		g.strokeRoundRect(x - (double) getWidth()/2 + 5, y - (double) currentHeight / 2 + 6, 19, 14, roundCornerValue, roundCornerValue);
		g.fillText("P▼", x - (double) getWidth()/2 + 7, y - (double) currentHeight / 2 + 17);
	}

	private void disjointDraw(GraphicsContext g, int x, int y, int roundCornerValue, Color colorD) {
		g.setFill(colorD);
		g.fillRoundRect(x + (double) getWidth() / 2 - 20, y - (double) currentHeight / 2 + 6, 14, 14, roundCornerValue, roundCornerValue);
		g.setFill(Color.BLACK);
		g.strokeRoundRect(x + (double) getWidth() / 2 - 20, y - (double) currentHeight / 2 + 6, 14, 14, roundCornerValue, roundCornerValue);
		g.fillText("D", x + (double) getWidth() / 2 - 17, y - (double) currentHeight / 2 + 17);

	}

	public void swapLabel(Boolean labelRendering, Boolean qualifiedRendering){
		
		String oldVisibleLabel = visibleLabel; 
		
		this.qualifiedRendering = qualifiedRendering; 
		this.labelRendering = labelRendering; 
		
		if (!explicitLabel.equals("")) {
			
			if (labelRendering) {
				if (qualifiedRendering) {
					if (!explicitQualifiedLabel.equals("")) {
						visibleLabel = explicitQualifiedLabel;
					}
				}
				else {
					if (!explicitLabel.equals("")) {
						visibleLabel = explicitLabel; 
					}
				}
			}
			else {
				// we do not want to use labels
				if (qualifiedRendering) {
					if (!qualifiedLabel.equals("")) {
						visibleLabel = qualifiedLabel; 
					}
				}
				else  {
					if (!label.equals("")){
						visibleLabel = label; 
					}
				}
			}
		}
		else {
			if (qualifiedRendering) {
				visibleLabel = qualifiedLabel; 
			}
			else {
				visibleLabel = label; 
			}
		}
		setWidth(calculateWidth());
	}

     public void addParent(Shape parent) {
    	if (!parents.contains(parent)) {
    	    parents.add(parent);
    	}
     }
	
	 public void addSon(Shape son){
		if (!children.contains(son)){
		    children.add(son);
	    }
	 }
	 
	 public void removeAllChildren(){
		 for (Shape child : children) {
			 child.asVisClass().removeParent(this);
		 }
		 children.clear();
	 }
	 
	 public void removeParent(Shape parent) {
		if (parents.contains(parent)){ 
			parents.remove(parent);
			VisConnector.removeConnector(graph.connectorList, parent, this);
		}	
	 }
		
	 public void removeSon(Shape son){
		if (children.contains(son)){ 
			children.remove(son);
		}	
	 }
		
	public Point2D getConnectionPoint(Point2D p,boolean left) {
	//return Closest conection point	
		if (left){
			connectionPointsL = new Point2D(getPosX() - (double) getWidth()/2, getPosY());
			return connectionPointsL;
		}
     	else {
			connectionPointsR = new Point2D(getPosX() + (double) getWidth()/2, getPosY());
		    return connectionPointsR;
		}
	}
	

	public OWLClassExpression getLinkedClassExpression (){
		return linkedClassExpression;
	}

	// <CBL 24/9/13> 
	// instead of setDefinition, we now add a new one
	// and update the labels
	public void addDefinition(OWLClassExpression def){
		
		definitions.add(def);
		
		if (definitionLabels == null) {
			definitionLabels = new ArrayList<>();
			qualifiedDefinitionLabels = new ArrayList<> ();
			// <CBL> for the time being, we are not considering the rendering 
			// of labels in the anonymous expressions, just qualified/nonQualified names of the 
			// concepts 
			// TO_DO: add two new methods to obtain the reducedClassExpression 
			// using the labels and the qualified labels for each of the terms. 
			explicitDefinitionLabels = definitionLabels; 
			explicitQualifiedDefinitionLabels = qualifiedDefinitionLabels; 
		}	
		// CBL: we add the different labels
		String label = ExpressionManager.getReducedClassExpression(def);
		definitionLabels.add(label);
		String auxQLabel = ExpressionManager.getReducedQualifiedClassExpression(def); 
       if (auxQLabel != null && !"null".equalsIgnoreCase(auxQLabel))
    	   qualifiedDefinitionLabels.add(auxQLabel); 
       else 
    	   qualifiedDefinitionLabels.add(label); 
       
	   visibleDefinitionLabels = definitionLabels;
	   
	}
	public ArrayList<OWLClassExpression> getDefinitions(){
		return definitions;
	}



	public String getToolTipInfo() {
	/*
	 * Renders html for class info
	 */			
		StringBuilder other = new StringBuilder();
		if (!isAnonymous){
			getInheritedObjectProperties();
			getInheritedDataProperties();

			if (!getVisibleDefinitionLabels().isEmpty()) {
				for (String defLabel : getVisibleDefinitionLabels()) {
					other = new StringBuilder("<b>"
							+ (isAnonymous ? removeFormatInformation(defLabel) : defLabel)
							+ "</b><br><br>");
				}
			} else {
				other = new StringBuilder("<b>"
						+ (isAnonymous ? removeFormatInformation(this.visibleLabel) : this.visibleLabel)
						+ "</b><br><br>");
			}
			if ((getDisjointClasses() !=null)&& (!getDisjointClasses().isEmpty())) {
				other.append("<b>Disjoint</b><ul>");
				
				VisClass auxVisClass = null;
				ArrayList<OWLClassExpression> auxArray = null;
				for (OWLClass cl: getDisjointClasses()) {
					auxVisClass = graph.getVisualExtension(cl); 
					if (auxVisClass != null) {
						
						if (auxVisClass.label.startsWith(sid.OntView2.expressionNaming.SIDClassExpressionNamer.className)) {
							auxArray = auxVisClass.getDefinitions();
							if (auxArray != null){ 
								for (OWLClassExpression ce: auxArray) {
									other.append("<li>").append(qualifiedRendering ?
                                            ExpressionManager.getReducedQualifiedClassExpression(ce) :
                                            ExpressionManager.getReducedClassExpression(ce)).append("</li>");
								}
							}
						}
						else {	
							other.append("<li>").append(qualifiedRendering ?
                                    ExpressionManager.getReducedQualifiedClassExpression(cl) :
                                    ExpressionManager.getReducedClassExpression(cl)).append("</li>");
						}
					}
				}
				other.append("</ul>");
			}
			other.append("</ul>");
		}
		return other.toString();
	}

	
	public void getInheritedObjectProperties(){
		if (inherited==null){
			inherited = new HashSet<OWLObjectProperty>();
			OWLReasoner reasoner       = graph.paintframe.getReasoner();
			OWLOntology activeOntology = graph.paintframe.getOntology();
			//all properties
		
			Set<OWLObjectProperty> objPropSet = activeOntology.getObjectPropertiesInSignature();
			for (OWLObjectProperty prop : objPropSet){
				Set<Node<OWLClass>> domainNodes = reasoner.getObjectPropertyDomains(prop, false).getNodes();
				Set<Node<OWLClass>> directNodes = reasoner.getObjectPropertyDomains(prop, true).getNodes();
				for (Node<OWLClass>directNode : directNodes) {
					if (directNode.contains((OWLClass) this.getLinkedClassExpression())){
						inherited.add(prop);
						domainNodes.removeAll(directNodes);
						getSuperClassExpression(reasoner,inherited,domainNodes,objPropSet);
					}
				}
			}
		}
	}
	
	private void getSuperClassExpression(OWLReasoner reasoner,HashSet<OWLObjectProperty> inherited2,
									Set<Node<OWLClass>> domainNodes,Set<OWLObjectProperty> objPropertySet) {
		// TODO Auto-generated method stub
		for (Node<OWLClass> node : domainNodes){
			for (OWLClass clase : node.getEntities()) {
				for (OWLObjectProperty prop : objPropertySet){
					NodeSet<OWLClass> p= reasoner.getObjectPropertyDomains(prop, true);
						if (p.containsEntity(clase))
							inherited2.add(prop);
				}
			}
		}
	}
	
	private void getSuperClassExpression(OWLReasoner reasoner,
			Set<Node<OWLClass>> domainNodes,Set<OWLDataProperty> objPropertySet) {
		// TODO Auto-generated method stub
		for (Node<OWLClass> node : domainNodes){
			for (OWLClass clase : node.getEntities()) {
				for (OWLDataProperty prop : objPropertySet){
					NodeSet<OWLClass> p= reasoner.getDataPropertyDomains(prop, true);
					if (p.containsEntity(clase))
						dInherited.add(prop);
				}
			}
		}
	}
	
	
	public void getInheritedDataProperties(){
		if (dInherited==null){
			dInherited = new HashSet<OWLDataProperty>();
			OWLReasoner reasoner       = graph.paintframe.getReasoner();
			OWLOntology activeOntology = graph.paintframe.getOntology();
			//all properties
		
			Set<OWLDataProperty> objPropSet = activeOntology.getDataPropertiesInSignature();
			for (OWLDataProperty prop : objPropSet){
				Set<Node<OWLClass>> domainNodes = reasoner.getDataPropertyDomains(prop, false).getNodes();
				Set<Node<OWLClass>> directNodes = reasoner.getDataPropertyDomains(prop, true).getNodes();
				for (Node<OWLClass>directNode : directNodes) {
					if (directNode.contains((OWLClass) this.getLinkedClassExpression())){
						dInherited.add(prop);
						domainNodes.removeAll(directNodes);
						getSuperClassExpression(reasoner,domainNodes,objPropSet);
					}
				}
			}
		}
	}
	
	@Override
	public int getLevelRelativePos() {
		// TODO Auto-generated method stub
		return RELATIVE_POS;
	}
	
	public int calculateWidth() {
		GraphicsContext g = graph.paintframe.getGraphicsContext2D();
	    int max = 0;
	    Font prevFont = g.getFont();
		Font newFont = Font.font("DejaVu Sans", FontWeight.BOLD, 10);

		g.setFont(newFont);

		// Way to measure text width
		Text textNode = new Text();
		textNode.setFont(newFont);

	    StringTokenizer sTokenizer = null; 
    	String token; 
    	int candidate = 0;
		try {
			if (!isAnonymous) {
				if (!label.startsWith(SIDClassExpressionNamer.className)) {
					textNode.setText(visibleLabel);
					max = (int) textNode.getLayoutBounds().getWidth() + 25 ;
				}
				else {
					// there is no label as it is supposed to be anonymous
					max = 0;
				}
				//<CBL> for the defined, max might not be the desired value
				if (isDefined) {

					// we have to do the same as for the anonymous ones
					// for each of the definitions
					for (String auxLabel: getVisibleDefinitionLabels()) {
						sTokenizer = new StringTokenizer(auxLabel, "\n");
						while (sTokenizer.hasMoreElements()) {
							token = sTokenizer.nextToken();
							textNode.setText(removeFormatInformation(token));
							candidate = (int) textNode.getLayoutBounds().getWidth() + 25;
							candidate += tabsSize(token);
							if (candidate > max) {
								max = candidate;
							}
						}
					}
				}
			}
			else {
				sTokenizer = new StringTokenizer(visibleLabel, "\n");
				max = 0;
				while (sTokenizer.hasMoreElements()) {
					token = sTokenizer.nextToken();
					textNode.setText(removeFormatInformation(token));
					candidate = (int) textNode.getLayoutBounds().getWidth() + 25;
					candidate += tabsSize(token);

					if (candidate > max) {
						max = candidate;
					}
				}
			}
			if (!getDisjointConnectors().isEmpty()) {
				max += 10;
			}
			if (propertyBox != null) {
				max += 20;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			g.setFont(prevFont);
		}

		return max;
		
	}
	public void createPropertyBox() {
		propertyBox = new VisPropertyBox(this);
	}
	
	public VisPropertyBox getPropertyBox(){
		return propertyBox;
	}
	
	@Override
	public int  getTotalHeight(){
		if ((propertyBox!=null)&&(propertyBox.visible))
			return getHeight()+propertyBox.getHeight();
		else 
			return getHeight();
	}

	public int calculateHeight(){
		GraphicsContext g = graph.paintframe.getGraphicsContext2D();
		Text textNode = new Text();
		textNode.setFont(g.getFont());

		int fontHeight = (int) textNode.getFont().getSize();
		int SPACE = 10;

	    int result = 0;
	    if (isAnonymous) {
			result += calculateTextHeight(label);
	    }
	    else {
	    	// <CBL 24/9/13> <
	    	// first step to the new representation of defined concepts
	    	if (!isDefined) { 
	    		result += fontHeight+5;
	    	}
	    	else {
	    		
	    		// we have to check whether it is a special defined concept
	    		if (!label.startsWith(SIDClassExpressionNamer.className)) {
		    		// CBL: for the grey box
		    		result += fontHeight+15;
		    		// CBL: for the underlying white box containing the definitions
		    		for (String auxLabel: getVisibleDefinitionLabels()) {
						result += calculateTextHeight(auxLabel);
					}
		    		if (!getVisibleDefinitionLabels().isEmpty())
		    			result += (getVisibleDefinitionLabels().size()-1)*5; 
	    		}
	    		else {
	    			for (String auxLabel: getVisibleDefinitionLabels()) {
						result += calculateTextHeight(auxLabel) + 5;
					}
		    		
	    		}
	    	}
	    	
	    }
		return result + SPACE;
	}

	private int calculateTextHeight(String text) {
		String[] lines = text.split("\n");
		int totalHeight = 0;

		for (String line : lines) {
			Text textNode = new Text(line);
			textNode.setFont(Font.font("DejaVu Sans", FontWeight.BOLD, 10));
			totalHeight += (int) textNode.getLayoutBounds().getHeight();
		}

		return totalHeight + (lines.length - 1) * 5 + 5;
	}
	
	public ArrayList<String> getVisibleDefinitionLabels() {
		return visibleDefinitionLabels;
	}

	public boolean onCloseBox(int x,int y){
		int px, py, pWidth, pHeight;
		if (!label.startsWith(SIDClassExpressionNamer.className)){
			px = getPosX() - getWidth() / 2 + 10;
			py = getPosY() - getHeight() / 2 + 1;
        } else {
			px = getPosX() - getWidth() / 2 + 5;
			py = getPosY() - getHeight() / 2 + 6;
        }
        pWidth = 19;
        pHeight = 14;

        return (x >= px && x <= px + pWidth) && (y >= py && y <= py + pHeight);
	}

	public boolean onCloseDisjoints(int x, int y) {
        int disjointX = getPosX() + getWidth() / 2 - 20;
        int disjointY = getPosY() - getHeight() / 2 + 6;
        int disjointWidth = 14;
		int disjointHeight = 14;

		return (x >= disjointX && x <= disjointX + disjointWidth) &&
				(y >= disjointY && y <= disjointY + disjointHeight);
	}


	public NodeSet<OWLNamedIndividual> getInstances(){
		OWLReasoner reasoner = graph.paintframe.getReasoner();
		return reasoner.getInstances(this.getLinkedClassExpression(), false);
	}
	
	public String removeFormatInformation(String str) {
		String aux; 
		aux = str.replace("\n", ""); 
		aux = str.replace("\t", ""); 
		return aux; 
	}
	
	public int countLines(String str) {
		int lines=1; 
		for (int i=0; i<str.length(); i++)
		{
			if (str.charAt(i) == '\n') {
				lines++; 
			}
		}
		return lines; 
	}
	
	public int tabsSize(String str) {
		boolean noMore = false; 
		int numTabs = 0;
		for (int i=0; i<str.length() && !noMore; i++) {
			if (str.charAt(i) == '\t') {
				numTabs++; 
			}
			else {
				noMore = true; 
			}
		}
		return numTabs*tabSize; 
	}

	public void drawFormattedString (GraphicsContext g, String toDraw, int x, int y, int ascent) {
		StringTokenizer sTokenizer = new StringTokenizer(toDraw,"\n"); 
		int currentX = x; 
		int currentY = y;
		String token; 
		while (sTokenizer.hasMoreTokens()) {
			token = sTokenizer.nextToken();
			currentX = x+tabsSize(token); 
			g.fillText(removeFormatInformation(token), currentX, currentY);
			Font font = Font.font("DejaVu Sans", FontWeight.NORMAL, 9);

			currentY += VisProperty.stringHeight(font, g)+6;
		}
	}

	public List<VisObjectProperty> getProperties() {
		if (propertyBox == null) {
			return Collections.emptyList();
		}
		return propertyBox.getProperties();
	}


}






