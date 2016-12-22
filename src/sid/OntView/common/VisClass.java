package sid.OntView.common;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import sid.OntView.utils.ExpressionManager;

public class VisClass extends Shape {
	
    //Graphic
	public static int instances = 0;
	public static final int FIRST_X_SEPARATION = 200;
	public static final int LAST_X_SEPARATION = 100;
	public static final int NODES_X_SEPARATION = 200;
	public static final int NODES_Y_SEPARATION = 40;
	public static final int RELATIVE_POS = 0;
	public static Color  color;
	public static Stroke stroke;
	
	
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
	VisPropertyBox     propertyBox;
	// <CBL 24/9/13>: Theoretically, one term can have more than one definition
	ArrayList<OWLClassExpression> definitions;
	
	String  explicitLabel = "";
	boolean isAnonymous;
    boolean isDefined   = false;
    boolean isBottom    = false;
    int     currentHeight;
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
		connectionPointsL = new Point(posx,posy+5);
		connectionPointsR = new Point(posx+getWidth(),posy+5);
		children    = new ArrayList<Shape>();
        parents     = new ArrayList<Shape>();
        properties  = new ArrayList<String>();
        
        //<CBL 24/9/13> Added the initialization in the constructor
        definitions = new ArrayList<OWLClassExpression>(); 
        visibleDefinitionLabels = new ArrayList<String>(); 
      
	}
	
	private ArrayList<VisConnectorDisjoint> getDisjointConnectors(){
		if (disjointList==null) {
			disjointList = new ArrayList<VisConnectorDisjoint>();
		}	
		return disjointList;
	}
	
	public ArrayList<VisConnectorEquiv> getEquivConnectors(){
		if (equivList==null) {
			equivList = new ArrayList<VisConnectorEquiv>();
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
	// updated to use the reasoner to obtain the disjoint classes
	public void addReasonedDisjointConnectors(){
		OWLReasoner reasoner = graph.paintframe.getReasoner(); 
		
		for (Node<OWLClass> disjointNode: reasoner.getDisjointClasses(this.getLinkedClassExpression())) {
			for (OWLClass c: disjointNode.getEntitiesMinusTop()) {
				if (c != this.getLinkedClassExpression() && reasoner.isSatisfiable(c)) {
					addDisjointConnector(graph.getVisualExtension(c)); 
				}
			}
		}
	}
	
	// <CBL 25/9/13> 
	// we process the disjointness axioms also to check 
	// whether someone has explicitly asserted the disjointness to Thing or Bottom
	
	public void addAssertedDisjointConnectors() {
		if (getDisJointClassesAxioms()!=null){
			for (OWLDisjointClassesAxiom axiom: getDisJointClassesAxioms()){
				for (OWLClassExpression e : axiom.getClassExpressions()){
					if(e != this.getLinkedClassExpression()){	
						addDisjointConnector(graph.getVisualExtension(e));
					}
				}
		    }	 
		}
	}
	
	public void addDisjointConnector(Shape dst){
		ArrayList<VisConnectorDisjoint> dstDisj;
		boolean already = false;
		dstDisj = dst.asVisClass().getDisjointConnectors();
		for (VisConnectorDisjoint d : dstDisj){
			if ((d.from==dst)&&(d.to==this)){
				already= true;
			}
		}
		if (!already){
			getDisjointConnectors().add(new VisConnectorDisjoint(this,dst));
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
			defFont= new Font(Font.DIALOG,Font.ITALIC,12);
		return defFont;
	}
	
	private Font getBoldFont(){
		if (boldFont==null)
			boldFont= new Font(Font.DIALOG,Font.BOLD,10);
		return boldFont;
	}
	
	public void drawShape(Graphics g) {
	    int x = posx+1;
	    int y = posy;
	    	    
	    Font oldFont=g.getFont();
	    if (this.isDefined) {
	        g.setFont(getDefinedClassFont());
	    }
	    else {
	    	g.setFont(getBoldFont());
	    }
	    FontMetrics fm = g.getFontMetrics(); 
	    int fontHeight = fm.getHeight();
	    
	    if (currentHeight == 0) {
	    	
	    	if (!isAnonymous && !isDefined) {
	    		currentHeight = getHeight();
	    	}
	    	else {
	    		currentHeight = calculateHeight(); 
	    	}
	    }
	    
	    if (visible){

	    	Color mini = new Color(224,224,224);
	    	
	    	if (!isDefined) {
	    		// CBL if it is not defined, we use the previous representation
		    	if (!isAnonymous) 
		    		g.setColor(mini);
		    	else
		    		g.setColor(Color.white);	
		    	
			    g.fillRect(x - getWidth()/2, y - currentHeight/2, getWidth(), currentHeight);
			    g.setColor(Color.black);
			    if (isBottom) {
			    	g.setColor(Color.red);
			    }	
		 
			    //rectangle
			    g.drawRect(x -  getWidth()/2, y - currentHeight/2,  getWidth()-1, currentHeight-1);
			    g.setColor(Color.lightGray);
			    if (propertyBox!=null){
			    	g.fillRect(x -  getWidth()/2, y + currentHeight/2, getWidth()-1, 6);
			    	g.setColor(Color.black);
			    	g.drawRect(x -  getWidth()/2, y + currentHeight/2,getWidth()-1, 6);
	
			    }	
			    g.setColor(Color.black);
	
	//    		g.drawString(label, x -(getWidth()-10)/2, (y - (oldHeight-4)/2) + fm.getAscent());
			    if (!isAnonymous) { 
			    	g.drawString(visibleLabel, x -(getWidth()-10)/2, (y - (currentHeight-4)/2) + fm.getAscent());
			    }
			    else {
			    	drawFormattedString(g, visibleLabel, x -(getWidth()-10)/2, (y - (currentHeight-4)/2) + fm.getAscent(), fm.getMaxAscent()); 
	//		    	g.drawString(removeFormatInformation(visibleLabel), x -(getWidth()-10)/2, (y - (currentHeight-4)/2) + fm.getAscent());
			    }
	    	}
	    	else {
	    		
	    		if (!label.startsWith(sid.OntView.expressionNaming.SIDClassExpressionNamer.className)) {
		    		// CBL: the new definitions representation 
		    		// a Background white rectangle for the definition
		    		// a grey for the 
		    		g.setColor(Color.white); 
		    		g.fillRect(x - getWidth()/2, y-currentHeight/2+5, getWidth(), currentHeight); 
		    		if (isBottom) 
		    			g.setColor(Color.red); 
		    		else 
		    			g.setColor(Color.black);
		    		g.drawRect(x -  getWidth()/2, y - currentHeight/2+5,  getWidth()-1, currentHeight-1);
		    		
		    		// now => the rectangle for the name of the concept
		    			
		    		g.setColor(mini); 
		    		g.fillRect(x - getWidth()/2 + 5, y-currentHeight/2, getWidth()-10, fontHeight+5); 
		    		if (isBottom) 
		    			g.setColor(Color.red); 
		    		else 
		    			g.setColor(Color.black);
		    		g.drawRect(x -  getWidth()/2+5, y - currentHeight/2,  getWidth()-10, fontHeight+4);
		    		
		    		// this is the name of the concept
		    		g.setFont(getDefinedClassFont()); 
		    		g.drawString(visibleLabel, x -(getWidth()-16) /2, (y - (currentHeight-4)/2) + fm.getAscent()); 
		    		
		    		g.setFont(getBoldFont()); 
		    		int auxY = y - (currentHeight/2) + (fontHeight +5) + 2; 
		    		if (visibleDefinitionLabels != null) {
		    			for (String auxDefString: visibleDefinitionLabels) {
		    				drawFormattedString(g, auxDefString, x -(getWidth()-12)/2, auxY + fm.getAscent(), fm.getMaxAscent()); 
		    				auxY += (countLines(auxDefString)*fontHeight) + 5; 
		    			}
		    		}
	    		}
	    		else {
	    			// CBL: it is an auxiliar definition 
	    			// CBL if it is not defined, we use the previous representation
			    	
			    	g.setColor(Color.white);	
			    	
				    g.fillRect(x - getWidth()/2, y - currentHeight/2, getWidth(), currentHeight);
				    g.setColor(Color.black);
				    if (isBottom) {
				    	g.setColor(Color.red);
				    }	
			 
				    //rectangle
				    g.drawRect(x -  getWidth()/2, y - currentHeight/2,  getWidth()-1, currentHeight-1);
				    g.setColor(Color.lightGray);
				    if (propertyBox!=null){
				    	g.fillRect(x -  getWidth()/2, y + currentHeight/2, getWidth()-1, 6);
				    	g.setColor(Color.black);
				    	g.drawRect(x -  getWidth()/2, y + currentHeight/2,getWidth()-1, 6);
		
				    }	
				    g.setColor(Color.black);
				    g.setFont(getBoldFont()); 
		    		int auxY = (y - (currentHeight-4)/2); 
		    		if (visibleDefinitionLabels != null) {
		    			for (String auxDefString: visibleDefinitionLabels) {
		    				drawFormattedString(g, auxDefString, x -(getWidth()-10)/2, auxY + fm.getAscent(), fm.getMaxAscent()); 
		    				auxY += (countLines(auxDefString)*fontHeight) + 5; 
		    			}
		    		}
//		//    		g.drawString(label, x -(getWidth()-10)/2, (y - (oldHeight-4)/2) + fm.getAscent());
//				    if (!isAnonymous) { 
//				    	g.drawString(visibleLabel, x -(getWidth()-10)/2, (y - (currentHeight-4)/2) + fm.getAscent());
//				    }
//				    else {
//				    	drawFormattedString(g, visibleLabel, x -(getWidth()-10)/2, (y - (currentHeight-4)/2) + fm.getAscent(), fm.getMaxAscent()); 
//		//		    	g.drawString(removeFormatInformation(visibleLabel), x -(getWidth()-10)/2, (y - (currentHeight-4)/2) + fm.getAscent());
//				    }
//	    			
	    		}
	    	}
		    
	    	//test

		    if (children.size() > 0 && (outConnectors!=null) &&(outConnectors.size()>0)){
		    	switch (this.getState()) {
		    	
		    	   case Shape.PARTIALLY_CLOSED : 
		    		   g.setColor(mini);
		   	           g.fillRect(x + getWidth()/2, y - 10, 10, 10);
		   	           g.setColor(Color.black);
		   	           g.drawRect(x + getWidth()/2, y - 10, 10, 10);
		   	           g.drawLine(x + getWidth()/2 + 2, y - 5, x + getWidth()/2 + 8, y - 5);
		   	           g.drawLine(x + getWidth()/2 + 5, y - 8, x + getWidth()/2 + 5, y - 3);
		   	           // open
		   	           g.setColor(mini);
		   	           g.fillRect(x + getWidth()/2, y, 10, 10);
		   	           g.setColor(Color.black);
		   	           g.drawRect(x + getWidth()/2, y, 10, 10);
		   	           g.drawLine(x + getWidth()/2 + 2, y + 5, x + getWidth()/2 + 8, y + 5);
		    		   break;
		    		   
		    	   case CLOSED : 
		    		   g.setColor(mini);
		 	           g.fillRect(x + getWidth()/2, y - 10, 10, 10);
		 	           g.setColor(Color.black);
		 	           g.drawRect(x + getWidth()/2, y - 10, 10, 10);
		 	           g.drawLine(x + getWidth()/2 + 2, y - 5, x + getWidth()/2 + 8, y - 5);
		 	           g.drawLine(x + getWidth()/2 + 5, y - 8, x + getWidth()/2 + 5, y - 3);
		    		   break;
	
		    	   case  Shape.OPEN : 
		    		   g.setColor(mini);
		 	           g.fillRect(x + getWidth()/2, y, 10, 10);
		 	           g.setColor(Color.black);
		 	           g.drawRect(x + getWidth()/2, y, 10, 10);
		 	           g.drawLine(x + getWidth()/2 + 2, y + 5, x + getWidth()/2 + 8, y + 5);
		    		   break;
		    		   
		    	   default :
		    		   break;
		       }
	     }

		  for (VisConnectorDisjoint disj :  getDisjointConnectors()){
			  disj.draw(g);
		  }
		  for (VisConnectorEquiv equ: getEquivConnectors()){
			  equ.draw(g);
		  }
 	  }
      g.setFont(oldFont);
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
		
//		if (!explicitLabel.equals("")){
//			String aux = label;
//			label = explicitLabel;
//			explicitLabel = aux;
//			setWidth(calculateWidth());
////			if (getWidth() > getVisLevel().getWidth())
////				getVisLevel().updateWidth(getWidth());
//		}
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
		
	public Point getConnectionPoint(Point p,boolean left) {
	//return Closest conection point	
		if (left){
			connectionPointsL.x = getPosX()-getWidth()/2;
			connectionPointsL.y = getPosY();
			return connectionPointsL;
		}
     	else {
			connectionPointsR.x = getPosX()+getWidth()/2;
			connectionPointsR.y = getPosY();
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
			definitionLabels = new ArrayList<String>(); 
			qualifiedDefinitionLabels = new ArrayList<String> (); 
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
		String other = "";
		if (!isAnonymous){
			getInheritedObjectProperties();
			getInheritedDataProperties();
			getAplicableObjectProperties();
			other = "<html><b>"
				+ (isAnonymous?removeFormatInformation(this.visibleLabel):this.visibleLabel)
				+ "</b><br><br>";
			// <CBL 24/9/13>
			// This info is not included in the tooltip anymore 
//			if ((isDefined)&&(!definitions.isEmpty())) {
//				other+= (qualifiedRendering?
//							ExpressionManager.getReducedQualifiedClassExpression(definition)
//							:ExpressionManager.getReducedClassExpression(definition))+"<br><br>";
//			}
			if ((getDisjointClasses() !=null)&& (getDisjointClasses().size()>0)) {
				other+="<b>Disjoint</b><ul>";
				// <CBL 25/9/13> 
				// updated to deal with the set of OWLClasses returned by the reasoner
//				for ( OWLDisjointClassesAxiom axiom : getDisJointClassesAxioms()){
//					for (OWLClass cl : axiom.getClassesInSignature()){
//						if (cl!= this.getLinkedClassExpression()){
//							other+="<li>"+(qualifiedRendering?
//									ExpressionManager.getReducedQualifiedClassExpression(cl):
//									ExpressionManager.getReducedClassExpression(cl))+"</li>";
//						}
//					}
//				}
				
				VisClass auxVisClass = null;
				ArrayList<OWLClassExpression> auxArray = null; 
				for (OWLClass cl: getDisjointClasses()) {
					auxVisClass = graph.getVisualExtension(cl); 
					if (auxVisClass != null) {
						
						if (auxVisClass.label.startsWith(sid.OntView.expressionNaming.SIDClassExpressionNamer.className)) {
							auxArray = auxVisClass.getDefinitions();
							if (auxArray != null){ 
								for (OWLClassExpression ce: auxArray) {
									other+="<li>"+(qualifiedRendering?
											ExpressionManager.getReducedQualifiedClassExpression(ce):
											ExpressionManager.getReducedClassExpression(ce)); 	
								}
							}
						}
						else {	
							other+="<li>"+(qualifiedRendering? 
									ExpressionManager.getReducedQualifiedClassExpression(cl):
									ExpressionManager.getReducedClassExpression(cl)); 
							
						}
					}
				}
				
				
				other+="</ul>";
			}
//			if  (inherited.size()>0) {
//				other += "<b>Inherited Properties</b><br><ul>";
//				for (OWLObjectProperty prop : inherited) {
//					other +="<li>" + VisObjectProperty.reduce(prop)+ "</li>";
//				}
//			}
//			if  (dInherited.size()>0) {
//				for (OWLDataProperty prop : dInherited) {
//					other +="<li><b>(d)</b>" + VisDataProperty.reduce(prop)+ "</li>";
//				}
//			}
			
			if (aplicable.size()>0) {
				other += "</ul><br><b>Aplicable</b><br><ul>";
				for (OWLObjectProperty prop : aplicable) {
					other +="<li>" + (qualifiedRendering?
										ExpressionManager.getReducedQualifiedObjectPropertyExpression(prop):
										ExpressionManager.getReducedObjectPropertyExpression(prop))+ "</li>";
				}
			}
//			if (dAplicable.size()>0) {
//				for (OWLDataProperty prop : dAplicable) {
//					other +="<li>" + VisDataProperty.reduce(prop)+ "</li>";
//				}
//			}
			other += "</ul></html>";
		}
		return other;
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
			return;
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
		return;
	}
	


	public void getAplicableObjectProperties(){
		if (aplicable==null){
			aplicable = new HashSet<OWLObjectProperty>();
			OWLOntologyManager ontManager = OWLManager.createOWLOntologyManager();
			OWLDataFactory dataFactory = ontManager.getOWLDataFactory();
			OWLReasoner reasoner       = graph.paintframe.getReasoner();
			OWLOntology activeOntology = graph.paintframe.getOntology();
			//all properties
			Set<OWLObjectProperty> objPropSet = activeOntology.getObjectPropertiesInSignature();
			for (OWLObjectProperty prop : objPropSet){
				//property domain
				if (isAplicable(reasoner,dataFactory, activeOntology, prop)){
					aplicable.add(prop);
				}
			}
			// getDefinition();
			return;
		}		
	}
	
	public void getAplicableDataProperties(){
		if (dAplicable==null){
			dAplicable = new HashSet<OWLDataProperty>();
			OWLOntologyManager ontManager = OWLManager.createOWLOntologyManager();
			OWLDataFactory dataFactory = ontManager.getOWLDataFactory();
			OWLReasoner reasoner       = graph.paintframe.getReasoner();
			OWLOntology activeOntology = graph.paintframe.getOntology();
			//all properties
			Set<OWLDataProperty> dPropSet = activeOntology.getDataPropertiesInSignature();
			for (OWLDataProperty prop : dPropSet){
				//property domain
				if (isAplicable(reasoner,dataFactory, activeOntology, prop)){
					dAplicable.add(prop);
				}
			}
			//getDefinition();
			return;
		}		
	}
	
	private boolean isAplicable(OWLReasoner reasoner,OWLDataFactory dFactory,OWLOntology activeOntology, OWLObjectProperty prop) {
		
		// returns true if the intersection of the concept and the property domain is satisfiable
		Set<Node<OWLClass>> domainNodeSet = reasoner.getObjectPropertyDomains(prop, true).getNodes();
		HashSet<OWLClassExpression> terms = new HashSet<OWLClassExpression>();
		for (Node<OWLClass> node : domainNodeSet){
			for (OWLClassExpression exp : node){
				terms.add(exp);
			}
		}
		terms.add(this.linkedClassExpression);
		OWLObjectIntersectionOf result = dFactory.getOWLObjectIntersectionOf(terms);
		return reasoner.isSatisfiable(result);
	}

	private boolean isAplicable(OWLReasoner reasoner,OWLDataFactory dFactory,OWLOntology activeOntology, OWLDataProperty prop) {
		
		// returns true if the intersection of the concept and the property domain is satisfiable
		Set<Node<OWLClass>> domainNodeSet = reasoner.getDataPropertyDomains(prop, true).getNodes();
		HashSet<OWLClassExpression> terms = new HashSet<OWLClassExpression>();
		for (Node<OWLClass> node : domainNodeSet){
			for (OWLClassExpression exp : node){
				terms.add(exp);
			}
		}
		terms.add(this.linkedClassExpression);
		OWLObjectIntersectionOf result = dFactory.getOWLObjectIntersectionOf(terms);
		return reasoner.isSatisfiable(result);
	}

	
	@Override
	public int getLevelRelativePos() {
		// TODO Auto-generated method stub
		return RELATIVE_POS;
	}
	
	public int calculateWidth () {
		Graphics g = graph.paintframe.getGraphics();
	    int max;
	    max = getWidth();
	    Font prevFont = g.getFont();
	    g.setFont(new Font(Font.DIALOG,Font.BOLD,10));
	    FontMetrics f = g.getFontMetrics(g.getFont());
	    
	    StringTokenizer sTokenizer = null; 
    	String token; 
    	int candidate = 0;	
	    if (!isAnonymous) {
	    	if (!label.startsWith(sid.OntView.expressionNaming.SIDClassExpressionNamer.className)) {
	    		max = (int) (f.getStringBounds(visibleLabel,g).getWidth()+40);
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
	    	    		candidate = (int)f.getStringBounds(removeFormatInformation(token),g).getWidth()+25;
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
	    		candidate = (int)f.getStringBounds(removeFormatInformation(token),g).getWidth()+25;
	    		candidate += tabsSize(token); 
	    		
	    		if (candidate > max) {
	    			max = candidate; 
	    		}
	    	}
	    }

	    g.setFont(prevFont);
	    f=g.getFontMetrics(new Font(Font.DIALOG,Font.ITALIC,9));
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
		Graphics g = graph.paintframe.getGraphics();
	    FontMetrics f = g.getFontMetrics(g.getFont());
	    int fontHeight = f.getHeight();
	    
	    int result = 0;
	    if (isAnonymous) {
	    	result += (countLines(label) * fontHeight) + 5;  
	    }
	    else {
	    	// <CBL 24/9/13> <
	    	// first step to the new representation of defined concepts
	    	if (!isDefined) { 
	    		result += fontHeight+5;
	    	}
	    	else {
	    		
	    		// we have to check whether it is a special defined concept
	    		if (!label.startsWith(sid.OntView.expressionNaming.SIDClassExpressionNamer.className)) {
		    		// CBL: for the grey box
		    		result += fontHeight+10;
		    		// CBL: for the underlying white box containing the definitions
		    		for (String auxLabel: getVisibleDefinitionLabels()) 
		    			result += (countLines(auxLabel)*fontHeight);
		    		if (!getVisibleDefinitionLabels().isEmpty())
		    			result += (getVisibleDefinitionLabels().size()-1)*5; 
	    		}
	    		else {
	    			for (String auxLabel: getVisibleDefinitionLabels()) 
		    			result += (countLines(auxLabel)*fontHeight + 5);
		    		
	    		}
	    	}
	    	
	    }
		if (propertyBox != null) {
			result += getPropertyBox().getHeight();
		}
		return result; 
	}
	
	public ArrayList<String> getVisibleDefinitionLabels() {
		return visibleDefinitionLabels;
	}

	public void setVisibleDefinitionLabels(ArrayList<String> visibleDefinitionLabels) {
		this.visibleDefinitionLabels = visibleDefinitionLabels;
	}

	public boolean onCloseBox(int x,int y){
		int px,py;
		px = getPosX()-getWidth()/2;
		py = getPosY()+getHeight()/2;
		return ((x>= px)&&(x<=px+getWidth()) &&(y>=py) &&(y<=py+6));
	}

	
	public  NodeSet<OWLNamedIndividual> getInstances(){
		OWLReasoner reasoner = graph.paintframe.getReasoner();
		return reasoner.getInstances(this.getLinkedClassExpression(), false);
	}
	
	
	public String removeFormatInformation (String str) {
		String aux; 
		aux = str.replace("\n", ""); 
		aux = str.replace("\t", ""); 
		return aux; 
	}
	
	public int countLines (String str) {
		int lines=1; 
		for (int i=0; i<str.length(); i++)
		{
			if (str.charAt(i) == '\n') {
				lines++; 
			}
		}
		return lines; 
	}
	
	public int tabsSize (String str) {
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

	public void drawFormattedString (Graphics g, String toDraw, int x, int y, int ascent) {
		StringTokenizer sTokenizer = new StringTokenizer(toDraw,"\n"); 
		int currentX = x; 
		int currentY = y;
		String token; 
		while (sTokenizer.hasMoreTokens()) {
			token = sTokenizer.nextToken();
			currentX = x+tabsSize(token); 
			g.drawString(removeFormatInformation(token), currentX, currentY); 
			currentY += VisProperty.stringHeight(new Font(Font.DIALOG,Font.PLAIN,9), g)+6; 
		}
	}
	
//	x -(getWidth()-10)/2, (y - (currentHeight-4)/2) + fm.getAscent()
	
}






