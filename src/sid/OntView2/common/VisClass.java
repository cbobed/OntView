package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Font;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Text;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.search.EntitySearcher;
import sid.OntView2.expressionNaming.SIDClassExpressionNamer;
import sid.OntView2.utils.ExpressionManager;

import java.util.*;
import java.util.stream.Collectors;

import static sid.OntView2.utils.ExpressionManager.qualifyLabel;
import static sid.OntView2.utils.ExpressionManager.replaceString;

public class VisClass extends Shape {
	
    //Graphic
	public static final int FIRST_X_SEPARATION = 200;
	public static Color color;
	
	String   label;
	OWLClassExpression linkedClassExpression;
	HashSet<OWLObjectProperty> inherited;
	HashSet<OWLDataProperty>  dInherited;
	ArrayList<Shape>   children;
	ArrayList<Shape>   parents;
	Set<Shape> descendants = new HashSet<>();
    Set<Shape> orderedDescendants = new HashSet<>();
	Set<Shape> ancestors = new HashSet<>();
    Set<Shape> orderedDescendantsByLevel = new HashSet<>();
    Set<Shape> orderedDescendantsByLevelLeastImportant = new HashSet<>();
    Set<Shape> orderedDescendantsByLevelBottomTop = new HashSet<>();
    ArrayList<VisConnectorDisjoint> disjointList;
	ArrayList<String>  properties; // those that have this class as its domain
	VisPropertyBox propertyBox;
    Set<OWLClassExpression> equivalentClasses;
	Set<String>  explicitLabel = new HashSet<>();

	boolean isAnonymous;
    boolean isDefined   = false, isKorean = false;
    public boolean isBottom    = false;
    int     currentHeight;

    // 17-01-2013
    // CBL: Added the qualified label fields to store the 
    // labels with the translated namespace
    String qualifiedLabel = "";  
    Set<String> explicitQualifiedLabel = new HashSet<>();

    // <CBL 24/9/13> 
    // we have to do the same with the definitions 
    // associated to the class
    // we have the four variants: normal, qualified, labels and qualified labels
    ArrayList<String> visibleDefinitionLabels;
    ArrayList<String> definitionLabels;
    Map<OWLClassExpression, List<String>> explicitDefinitionLabels;
    ArrayList<String> qualifiedDefinitionLabels;
    Map<OWLClassExpression, List<String>> explicitQualifiedDefinitionLabels;
	// </CBL>
    
    String visibleLabel;

	boolean qualifiedRendering = false; 
	boolean labelRendering = false;
	public int topToBarDistance = 0;
	int tabSize = 15;
    private int lineSpacing = 6;

    Color mini = Color.rgb(224, 224, 224);
    Color lightgray = Color.rgb(234, 234, 234);
    Color lightBlue = Color.rgb(212, 238, 247);
    Color lightGreen = Color.rgb(212, 247, 212);
    Color barGreen = Color.rgb(120, 190, 145);
    Color lightYellow = Color.rgb(255, 255, 184);

	public boolean isAnonymous(){return isAnonymous;}
	public ArrayList<Shape> getChildren() {return children;}

	public Set<Shape> getDescendants() { return descendants; }
	public Set<Shape> getAncestors() { return ancestors; }
 	
	public VisClass(int par_depthlevel,OWLClassExpression o, String pLabel, VisGraph pGraph) {
		
		super();
		depthlevel = par_depthlevel;
        setPosX(0); 
        //first position is irrelevant after rearranging 
		setPosY((int)(Math.random()*600));
		graph     = pGraph;
		setHeight(0); 
		setWidth (0);
		currentHeight= getHeight();
		linkedClassExpression = o;
		this.label= pLabel;
		this.visibleLabel = pLabel;
		connectionPointsL = new Point2D(posx,posy+5);
		connectionPointsR = new Point2D(posx+getWidth(),posy+5);
		children    = new ArrayList<>();
        parents     = new ArrayList<>();
        properties  = new ArrayList<>();
        
        equivalentClasses = new LinkedHashSet<>();
        visibleDefinitionLabels = new ArrayList<>();
      
	}

	@Override
	public String getLabel() {
		if (label.contains(VisConstants.SIDCLASS)) {
			for (String defLabel : getVisibleDefinitionLabels()) {
				return defLabel;
			}
		}
		return label; }
	
	ArrayList<VisConnectorDisjoint> getDisjointConnectors(){
		if (disjointList==null) {
			disjointList = new ArrayList<>();
		}
		return disjointList;
	}
	
	private Set<OWLDisjointClassesAxiom> getDisJointClassesAxioms(){
		  OWLOntology ontology = graph.paintframe.getOntology();
		  if (this.getLinkedClassExpression() instanceof OWLClass)
			  return ontology.disjointClassesAxioms((OWLClass) this.getLinkedClassExpression()).collect(Collectors.toSet()); 
		  return null;
	}
	
	private Set<OWLClass> getDisjointClasses() {
		OWLReasoner reasoner = graph.paintframe.getReasoner();
		return reasoner.getDisjointClasses(this.getLinkedClassExpression()).entities().collect(Collectors.toSet());
		 
	}
	
	// <CBL 25/9/13> 
	// we process the disjointness axioms also to check 
	// whether someone has explicitly asserted the disjointness to Thing or Bottom
	
	public void addAssertedDisjointConnectors() {
		if (getDisJointClassesAxioms()!=null){
			for (OWLDisjointClassesAxiom axiom: getDisJointClassesAxioms()){
                axiom.classExpressions()
                    .filter(e -> !e.equals(this.getLinkedClassExpression()))
                    .forEach(e -> {
                        if (graph.getVisualExtension(e) != null) {
                            addDisjointConnector(graph.getVisualExtension(e));
                        }
                    });
		    }	 
		}
	}

	public void addDisjointConnector(Shape dst){
		boolean already = Objects.equals(this.getLabel(), dst.getLabel());
        if (!already){
			getDisjointConnectors().add(new VisConnectorDisjoint(this, dst));
		}
	}
	
	private Font boldFont, koreanFont;
	private Font getBoldFont(){
		if (boldFont==null)
			boldFont= Font.font("DejaVu Sans", FontWeight.BOLD, 10);
		return boldFont;
	}

    private Font getKoreanFont(){
        if (koreanFont==null)
            koreanFont= Font.font("Noto Sans CJK KR", FontWeight.BOLD, 10);
        return koreanFont;
    }

	public void drawShape(GraphicsContext g) {
		if (g == null) return;

		int roundCornerValue = 10;
		int x = posx + 1, y = posy;

        Font oldFont = g.getFont();
        g.setFont(this.isKorean ? getKoreanFont() : getBoldFont());

		Text textNode = new Text();
		textNode.setFont(g.getFont());
		double fontHeight = textNode.getBoundsInLocal().getHeight();
		double ascent = textNode.getBaselineOffset() + 5.5;
	    
	    if (currentHeight == 0) {
            currentHeight = (!isAnonymous && !isDefined && asVisClass().getEquivalentClasses().isEmpty()) ? getHeight() : calculateHeight();
		}
        setWidth(calculateWidth());

        if (visible) {
            int propSpace = (propertyBox != null) ? 20 : 0;

            if (!isDefined && asVisClass().getEquivalentClasses().isEmpty()) {
                drawPrimitiveAndAnonymous(g, x, y, ascent, roundCornerValue, propSpace);
            } else if (!label.startsWith(SIDClassExpressionNamer.className)) {
                drawDefined(g, x, y, fontHeight, ascent, roundCornerValue, propSpace);
            } else if (!asVisClass().getEquivalentClasses().isEmpty()){
                drawDefined(g, x, y, fontHeight, ascent, roundCornerValue, propSpace);
            } else {
                drawAuxDefinition(g, x, y, ascent, roundCornerValue, propSpace);
            }

            // Draw hidden descendants indicator
            if (!outConnectors.isEmpty() && !outConnectors.get(0).to.asVisClass().isBottom) {
                drawHiddenNodesIndicator(g, getHiddenDescendantsSet(), getLeftCorner(), posy);
            }

            // Draw expand/collapse toggles
            drawToggles(g, x, y);
        }
		g.setFont(oldFont);
	}

    /**
     * Draws the primitive and anonymous classes
     */
    private void drawPrimitiveAndAnonymous(GraphicsContext g, int x, int y, double ascent, int roundCorner, int propSpace) {
        // CBL if it is not defined, we use the previous representation
        g.setFill(isAnonymous ? Color.WHITE : lightgray);

        g.fillRoundRect(x - getWidth()/2.0, y - currentHeight/2.0, getWidth(), currentHeight, roundCorner, roundCorner);
        g.setStroke(isBottom ? Color.RED : Color.BLACK);

        //rectangle
        g.strokeRoundRect(x - getWidth()/2.0, y - currentHeight/2.0,  getWidth()-1, currentHeight-1, roundCorner, roundCorner);
        g.setFill(Color.BLACK);
        // Square for properties
        if (propertyBox != null) propertyDraw(g, x, y, roundCorner);
        if (!getDisjointConnectors().isEmpty()) disjointDraw(g, x, y, roundCorner);

        g.setFill(Color.BLACK);
        if (!isAnonymous) {
            g.fillText(visibleLabel, x - (double) (getWidth())/2 + 10 + propSpace, (y -(double) (currentHeight - 4) / 2) + ascent);
        }
        else {
            drawFormattedString(g, visibleLabel, x - (getWidth() - 10)/2,  (int) ((y - (currentHeight - 4) / 2) + ascent));
        }
    }

    /**
     * Draws defined classes
     */
    private void drawDefined(GraphicsContext g, int x, int y, double fontH, double ascent, int roundCorner, int propSpace) {
        // CBL: the new definitions representation
        g.setFill(lightGreen);
        g.fillRect(x - getWidth()/2.0, y - currentHeight/2.0, getWidth(), currentHeight);
        g.setStroke(isBottom ? Color.RED : Color.BLACK);
        g.strokeRect(x - getWidth()/2.0, y - currentHeight /2.0, getWidth()-1, currentHeight);

        // now => the rectangle for the name of the concept
        g.setFill(lightgray);
        g.fillRect(x - getWidth()/2.0 + 5, y - currentHeight/2.0 - 5, getWidth()-10, fontH+15);
        g.setStroke(isBottom ? Color.RED : Color.BLACK);
        g.strokeRect(x - getWidth()/2.0 + 5, y - currentHeight/2.0 - 5,  getWidth()-10, fontH+14);

        // this is the name of the concept
        g.setFill(Color.BLACK);
        g.fillText(visibleLabel, x - (getWidth() - 16)/2.0 + 5 + propSpace, (y - (currentHeight - 4)/2.0) - 6 + ascent);

        double auxY = y - (currentHeight/2.0) + (fontH + 5) + 2;
        if (visibleDefinitionLabels != null) {
            for (String auxDefString: visibleDefinitionLabels) {
                drawFormattedString(g, auxDefString, x - (getWidth() - 12) / 2, (int) (auxY + ascent)+5);
                auxY += textHeight(g, auxDefString);
            }
        }

        if (propertyBox != null) propertyDraw(g, x + 5, y - 5, roundCorner);
        if (!getDisjointConnectors().isEmpty()) disjointDraw(g, x-5, y - 5, roundCorner);

        g.setFill(Color.BLACK);
    }

    /**
     * Draws the auxiliary definition
     */
    private void drawAuxDefinition(GraphicsContext g, int x, int y, double ascent, int roundCorner, int propSpace) {
        // CBL: it is an auxiliary definition
        // CBL if it is not defined, we use the previous representation
        g.setFill(Color.WHITE);
        g.fillRoundRect(x - getWidth()/2.0, y - currentHeight/2.0, getWidth(), currentHeight, roundCorner, roundCorner);
        g.setStroke(isBottom ? Color.RED : Color.BLACK);

        //rectangle
        g.strokeRoundRect(x - getWidth()/2.0, y - currentHeight /2.0,  getWidth()-1, currentHeight-1, roundCorner, roundCorner);
        g.setFill(Color.BLACK);
        if (propertyBox!=null){
            propertyDraw(g, x, y, roundCorner);
            propSpace += 5;
        }
        if (!getDisjointConnectors().isEmpty()) disjointDraw(g, x, y, roundCorner);

        double auxY = (y - (double) (currentHeight - 4) /2);
        if (visibleDefinitionLabels != null) {
            for (String auxDefString: visibleDefinitionLabels) {
                g.setFill(Color.BLACK);
                drawFormattedString(g, auxDefString, x - (getWidth() - 10) / 2 + propSpace, (int) (auxY + ascent)-1);
                auxY += textHeight(g, auxDefString);
            }
        }
    }

    /**
     * Draws the expand/collapse toggles
     */
    private void drawToggles(GraphicsContext g, int x, int y) {
        if (isBottom) return;

        // right side
        if (!children.isEmpty() && (outConnectors != null) && (!outConnectors.isEmpty()) &&
            (!outConnectors.get(0).to.asVisClass().isBottom)) {
            drawSideToggles(g, x + getWidth() / 2.0, y, getState());
        }

        // left side
        if (!parents.isEmpty() && (inConnectors != null) && (!inConnectors.isEmpty()) &&
            (!inConnectors.get(0).from.getLabel().matches("Thing"))) {
            drawSideToggles(g, x - getWidth()/2.0 - 10, y, getLeftState());
        }
    }

    private void drawSideToggles(GraphicsContext g, double posX, double posY, int state) {
        double toggleSize = 10;
        g.setFill(mini);
        g.setStroke(Color.BLACK);

        switch (state) {
            case PARTIALLY_CLOSED:
            case LEFT_PARTIALLY_CLOSED:
                // top toggle
                g.fillRect(posX, posY - toggleSize, toggleSize, toggleSize);
                g.strokeRect(posX, posY - toggleSize, toggleSize, toggleSize);
                g.strokeLine(posX + 2, posY - toggleSize/2, posX + 8, posY - toggleSize/2);
                g.strokeLine(posX + toggleSize/2, posY - toggleSize + 2, posX + toggleSize/2, posY - 3);
                // bottom toggle
                g.fillRect(posX, posY, toggleSize, toggleSize);
                g.strokeRect(posX, posY, toggleSize, toggleSize);
                g.strokeLine(posX + 2, posY + toggleSize/2, posX + 8, posY + toggleSize/2);
                break;

            case CLOSED:
            case LEFTCLOSED:
                // single toggle
                g.fillRect(posX, posY - toggleSize, toggleSize, toggleSize);
                g.strokeRect(posX, posY - toggleSize, toggleSize, toggleSize);
                g.strokeLine(posX + 2, posY - toggleSize/2, posX + 8, posY - toggleSize/2);
                g.strokeLine(posX + toggleSize/2, posY - toggleSize + 2, posX + toggleSize/2, posY - 3);
                break;

            case OPEN:
            case LEFTOPEN:
                g.fillRect(posX, posY, toggleSize, toggleSize);
                g.strokeRect(posX, posY, toggleSize, toggleSize);
                g.strokeLine(posX + 2, posY + toggleSize/2, posX + 8, posY + toggleSize/2);
                break;
            default:
                break;
        }
    }

    public void paintFocus(GraphicsContext g) {
        if (g == null){
            return;
        }
        Color oldColor = (Color) g.getFill();
        Color oldStroke = (Color) g.getStroke();
        g.setFill(lightYellow);
        g.fillRoundRect(getLeftCorner() - 15, getTopCorner()-15, getWidth() + 30, getHeight() + 30, 10, 10);

        g.setStroke(Color.GOLD);
        g.strokeRoundRect(getLeftCorner() - 15, getTopCorner()-15, getWidth() + 30, getHeight() + 30, 10, 10);
        g.setFill(oldColor);
        g.setStroke(oldStroke);
    }

    public void setMaxSizeHiddenNodesIndicator(){
        GraphicsContext g = graph.paintframe.getGraphicsContext2D();
        setIndicatorSize(getIndicatorSize(g, graph.getShapeMap().size() - 2) + 8);
    }

	private void drawHiddenNodesIndicator(GraphicsContext g, int hiddenNodes, int x, int y) {
		double width = getIndicatorSize();
		double height = 13;
		double rectY = y - (double) currentHeight / 2 - height - 5;

		if ((isDefined && !label.startsWith(SIDClassExpressionNamer.className))
            || !asVisClass().getEquivalentClasses().isEmpty()) rectY -= 5;

		double visiblePercentage = (double) (descendants.size() - hiddenNodes) / descendants.size();
		double hiddenPercentage = 1.0 - visiblePercentage;

        double visibleWidth = width * visiblePercentage;
        double hiddenWidth = width * hiddenPercentage;

		g.setFill(barGreen); // visible nodes
		g.fillRect(x, rectY, visibleWidth, height);
		g.setFill(Color.LIGHTGRAY); // hidden nodes
		g.fillRect((double) x + visibleWidth, rectY, hiddenWidth, height);
		g.setStroke(Color.BLACK);
		g.strokeRect(x, rectY, width, height);

		String text;
        if (hiddenNodes == 0) {
            text = (descendants.size() == 1) ? "1 visible descendant" : descendants.size() + " visible descendants";
        } else {
            text = (descendants.size() - hiddenNodes) + " / " + descendants.size() + " visible desc.";
        }
        drawCenteredText(g, text, x, rectY, width, height);


        topToBarDistance = (int) ((y - (double) currentHeight / 2) - rectY);
	}

	private void drawCenteredText(GraphicsContext g, String text, double rectX, double rectY, double rectWidth, double rectHeight) {
		g.setFill(Color.BLACK);
		g.setFont(Font.font("Arial", FontWeight.BOLD, 11));

		Text tempText = new Text(text);
		tempText.setFont(g.getFont());
		double textX = rectX + (rectWidth - tempText.getLayoutBounds().getWidth()) / 2;
		double textY = rectY + (rectHeight + tempText.getLayoutBounds().getHeight()) / 2 - 3;

		g.fillText(text, textX, textY);
	}

	private double getIndicatorSize(GraphicsContext g, int totalNodes) {
		Text textNode = new Text(totalNodes + "visible descendants");
		textNode.setFont(g.getFont());
		return textNode.getLayoutBounds().getWidth();
	}


	private void propertyDraw(GraphicsContext g, int x, int y, int roundCornerValue) {
		g.setFill(lightBlue);
		g.fillRoundRect(x - (double) getWidth()/2 + 5, y - (double) currentHeight / 2 + 6, 19, 14, roundCornerValue, roundCornerValue);
		g.setFill(Color.BLACK);
		g.strokeRoundRect(x - (double) getWidth()/2 + 5, y - (double) currentHeight / 2 + 6, 19, 14, roundCornerValue, roundCornerValue);
		g.fillText("P"+OntViewConstants.UPSIDE_DOWN_TRIANGLE, x - (double) getWidth()/2 + 7, y - (double) currentHeight / 2 + 17);
	}

	private void disjointDraw(GraphicsContext g, int x, int y, int roundCornerValue) {
		g.setFill(Color.LIGHTYELLOW);
		g.fillRoundRect(x + (double) getWidth() / 2 - 20, y - (double) currentHeight / 2 + 6, 14, 14, roundCornerValue, roundCornerValue);
		g.setFill(Color.BLACK);
		g.strokeRoundRect(x + (double) getWidth() / 2 - 20, y - (double) currentHeight / 2 + 6, 14, 14, roundCornerValue, roundCornerValue);
		g.fillText("D", x + (double) getWidth() / 2 - 17, y - (double) currentHeight / 2 + 17);

	}

    public void swapLabelEquivalentClasses(Boolean labelRendering, Boolean qualifiedRendering, String language) {
        if (labelRendering) {
            if (qualifiedRendering) {
                if (explicitQualifiedDefinitionLabels == null || explicitQualifiedDefinitionLabels.isEmpty()) return;

                ArrayList<String> aux = new ArrayList<>();
                for (OWLClassExpression eqDef : getEquivalentClasses()) {
                    if (eqDef instanceof OWLClass) {
                        List<String> labels = explicitQualifiedDefinitionLabels.getOrDefault(eqDef, Collections.emptyList());
                        List<String> matches = labels.stream().filter(s -> s.contains("@" + language)).toList();

                        if (!matches.isEmpty()) {
                            aux.addAll(matches);
                        } else {
                            aux.add(qualifyLabel(eqDef.asOWLClass().getIRI().toString(), ExpressionManager.getReducedClassExpression(eqDef)));
                        }
                    } else {
                        aux.add(ExpressionManager.getReducedClassExpression(eqDef));
                    }
                }
                if (!aux.isEmpty()) visibleDefinitionLabels = aux;
            } else {
                if (explicitDefinitionLabels == null || explicitDefinitionLabels.isEmpty()) return;

                ArrayList<String> aux = new ArrayList<>();
                for (OWLClassExpression eqDef : getEquivalentClasses()) {
                    List<String> labels = explicitDefinitionLabels.getOrDefault(eqDef, Collections.emptyList());
                    List<String> matches = labels.stream().filter(s -> s.contains("@" + language)).toList();

                    if (!matches.isEmpty()) {
                        aux.addAll(matches);
                    } else {
                        aux.add(ExpressionManager.getReducedClassExpression(eqDef));
                    }

                }
                if (!aux.isEmpty()) visibleDefinitionLabels = aux;
            }
        } else {
            if (qualifiedRendering){
                if (qualifiedDefinitionLabels != null) {
                    visibleDefinitionLabels = qualifiedDefinitionLabels;
                }
            }
            else {
                if (definitionLabels != null) {
                    visibleDefinitionLabels = definitionLabels;
                }
            }
        }
    }

    public void swapLabel(Boolean labelRendering, Boolean qualifiedRendering, String language) {
		this.qualifiedRendering = qualifiedRendering; 
		this.labelRendering = labelRendering;
        this.isKorean = language.equals("ko");

        swapLabelEquivalentClasses(labelRendering, qualifiedRendering, language);

        if (!explicitLabel.isEmpty()) {
			if (labelRendering) {
				if (qualifiedRendering) {
					if (!explicitQualifiedLabel.isEmpty()) {
                        Optional<String> candidate = explicitQualifiedLabel.stream()
                            .filter(s -> s.contains("@" + language))
                            .findFirst();
                        candidate.ifPresent(s -> visibleLabel = s);
					}
				}
				else {
                    Optional<String> candidate = explicitLabel.stream()
                        .filter(s -> s.contains("@" + language))
                        .findFirst();
                    candidate.ifPresent(s -> visibleLabel = s);
                }
			}
			else {
				// we do not want to use labels
				if (qualifiedRendering) {
					if (!qualifiedLabel.isEmpty()) {
						visibleLabel = qualifiedLabel;
					}
				}
				else  {
					if (!label.isEmpty()){
						visibleLabel = label;
					}
				}
			}
		}
		else {
			if (qualifiedRendering) { // QUALIFIED
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
		
	public Point2D getConnectionPoint(Point2D p,boolean left) {
	    //return Closest connection point
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

    public void setLinkedClassExpression(OWLClassExpression linkedClassExpression) {
        this.linkedClassExpression = linkedClassExpression;
    }

	// <CBL 24/9/13> 
	// instead of setDefinition, we now add a new one
	// and update the labels
	public void addEquivalentExpression(OWLClassExpression def){

        if (def.equals(linkedClassExpression)) return;

		if (!equivalentClasses.contains(def)) {
			equivalentClasses.add(def);

			if (definitionLabels == null) {
                definitionLabels = new ArrayList<>();
                qualifiedDefinitionLabels = new ArrayList<> ();
                explicitDefinitionLabels = new LinkedHashMap<>();
                explicitQualifiedDefinitionLabels = new LinkedHashMap<>();
            }
        }
	}

    public void removeEquivalentExpression(OWLClassExpression def) {
        equivalentClasses.remove(def);
    }

    public void reorderEquivalentClasses() {
        Set<OWLClassExpression> named = new LinkedHashSet<>();
        Set<OWLClassExpression> noNamed = new LinkedHashSet<>();
        for (OWLClassExpression def : getEquivalentClasses()){
            if (def.isNamed()) named.add(def);
            else noNamed.add(def);
        }

        equivalentClasses.clear();
        equivalentClasses.addAll(noNamed);
        equivalentClasses.addAll(named);

        for (OWLClassExpression def : equivalentClasses) {
            saveLabelsQualifiedNamesEquivalentClasses(def);
        }
    }

    private void saveLabelsQualifiedNamesEquivalentClasses(OWLClassExpression def) {
        List<String> labelsForDef = new ArrayList<>();
        List<String> qualifiedLabelsForDef = new ArrayList<>();
        if (def instanceof OWLClass) {
            for (OWLAnnotation  an : EntitySearcher.getAnnotations(def.asOWLClass(), graph.getActiveOntology()).toList() ){
                if (an.getProperty().toString().equals("rdfs:label")){
                    String auxLabel = replaceString(an.getValue().toString().replaceAll("\"", ""));
                    labelsForDef.add(auxLabel);
                    String auxQLabel = qualifyLabel(def.asOWLClass().getIRI().toString(), auxLabel);
                    qualifiedLabelsForDef.add(auxQLabel != null ? auxQLabel : auxLabel);

                    if (auxLabel.contains("@")) {
                        graph.paintframe.languagesLabels.add(auxLabel.split("@")[1]);
                    }
                }
            }
        }
        explicitDefinitionLabels.put(def, labelsForDef);
        explicitQualifiedDefinitionLabels.put(def, qualifiedLabelsForDef);

        // CBL: we add the different labels
        String label = ExpressionManager.getReducedClassExpression(def);
        definitionLabels.add(label);
        String auxQLabel = ExpressionManager.getReducedQualifiedClassExpression(def);
        if (!"null".equalsIgnoreCase(auxQLabel))
            qualifiedDefinitionLabels.add(auxQLabel);
        else
            qualifiedDefinitionLabels.add(label);

        visibleDefinitionLabels = definitionLabels;
    }

	public Set<OWLClassExpression> getEquivalentClasses(){
		return equivalentClasses;
	}

    public String getToolTipInfo() {
        StringBuilder other = new StringBuilder();
        if (!isAnonymous){
            getInheritedObjectProperties();
            getInheritedDataProperties();

            other = new StringBuilder((isAnonymous ? removeFormatInformation(this.visibleLabel) : this.visibleLabel) + "\n\n");

            if (getDisjointClasses() != null && !getDisjointClasses().isEmpty()) {
                other.append("Disjoint:\n");
                VisClass auxVisClass;
                Set<OWLClassExpression> auxSet;
                for (OWLClass cl: getDisjointClasses()) {
                    auxVisClass = graph.getVisualExtension(cl);
                    if (auxVisClass != null) {
                        if (auxVisClass.label.startsWith(sid.OntView2.expressionNaming.SIDClassExpressionNamer.className)) {
                            auxSet = auxVisClass.getEquivalentClasses();
                            if (auxSet != null){
                                for (OWLClassExpression ce: auxSet) {
                                    other.append("- ").append(qualifiedRendering ?
                                        ExpressionManager.getReducedQualifiedClassExpression(ce) :
                                        ExpressionManager.getReducedClassExpression(ce)).append("\n");
                                }
                            }
                        } else {
                            other.append("- ").append(qualifiedRendering ?
                                ExpressionManager.getReducedQualifiedClassExpression(cl) :
                                ExpressionManager.getReducedClassExpression(cl)).append("\n");
                        }
                    }
                }
            }
        }
        return other.toString();
    }
	
	public void getInheritedObjectProperties(){
		if (inherited==null){
			inherited = new HashSet<>();
			OWLReasoner reasoner       = graph.paintframe.getReasoner();
			OWLOntology activeOntology = graph.paintframe.getOntology();
			//all properties
		
			Set<OWLObjectProperty> objPropSet = activeOntology.objectPropertiesInSignature().collect(Collectors.toSet());
			for (OWLObjectProperty prop : objPropSet){
				Set<Node<OWLClass>> domainNodes = reasoner.getObjectPropertyDomains(prop, false).nodes()
                    .collect(Collectors.toSet());
				Set<Node<OWLClass>> directNodes = reasoner.getObjectPropertyDomains(prop, true).nodes()
                    .collect(Collectors.toSet());
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
            node.entities().forEach(oClass -> {
                for (OWLObjectProperty prop : objPropertySet) {
                    if (reasoner.getObjectPropertyDomains(prop, true).containsEntity(oClass)) {
                        inherited2.add(prop);
                    }
                }
            });
		}
	}
	
	private void getSuperClassExpression(OWLReasoner reasoner,
			Set<Node<OWLClass>> domainNodes,Set<OWLDataProperty> objPropertySet) {
		// TODO Auto-generated method stub
		for (Node<OWLClass> node : domainNodes){
            node.entities().forEach(owlClass -> {
                for (OWLDataProperty prop : objPropertySet) {
                    if (reasoner.getDataPropertyDomains(prop, true).containsEntity(owlClass)) {
                        dInherited.add(prop);
                    }
                }
            });
		}
	}
	
	public void getInheritedDataProperties(){
		if (dInherited==null){
			dInherited = new HashSet<>();
			OWLReasoner reasoner       = graph.paintframe.getReasoner();
			OWLOntology activeOntology = graph.paintframe.getOntology();
			//all properties
		
			Set<OWLDataProperty> objPropSet = activeOntology.dataPropertiesInSignature().collect(Collectors.toSet());
			for (OWLDataProperty prop : objPropSet){
				Set<Node<OWLClass>> domainNodes = reasoner.getDataPropertyDomains(prop, false).nodes()
                    .collect(Collectors.toSet());
				Set<Node<OWLClass>> directNodes = reasoner.getDataPropertyDomains(prop, true).nodes()
                    .collect(Collectors.toSet());
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

    public int calculateWidth() {
        GraphicsContext g = graph.paintframe.getGraphicsContext2D();
        int max = 0;
        Font prevFont = g.getFont(), newFont;
        newFont = this.isKorean ? getKoreanFont() : getBoldFont();
        g.setFont(newFont);

        // Way to measure text width
        Text textNode = new Text();
        textNode.setFont(newFont);

        StringTokenizer sTokenizer;
        String token;
        int candidate;
        try {
            if (!isAnonymous) {
                if (!label.startsWith(SIDClassExpressionNamer.className) ) {
                    textNode.setText(visibleLabel);
                    max = (int) textNode.getLayoutBounds().getWidth() + 25 ;
                }
                //<CBL> for the defined, max might not be the desired value
                if (isDefined || !asVisClass().getEquivalentClasses().isEmpty()) {
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
	    	if (!isDefined && asVisClass().getEquivalentClasses().isEmpty()) {
	    		result += fontHeight + 5;
	    	}
	    	else {
	    		// we have to check whether it is a special defined concept
	    		if (!label.startsWith(SIDClassExpressionNamer.className)) {
		    		// CBL: for the grey box
		    		result += fontHeight + 10;
		    		// CBL: for the underlying white box containing the definitions
		    		for (String auxLabel: getVisibleDefinitionLabels()) {
						result += calculateTextHeight(auxLabel);
					}
		    		if (getVisibleDefinitionLabels() != null && !getVisibleDefinitionLabels().isEmpty())
		    			result +=  (getVisibleDefinitionLabels().size()-1);
	    		}
	    		else {
	    			for (String auxLabel: getVisibleDefinitionLabels()) {
						result += calculateTextHeight(auxLabel);
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
			totalHeight += (int) textNode.getLayoutBounds().getHeight() + lineSpacing;
		}

		return totalHeight + (lines.length - 1);
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

		if (isDefined && !label.startsWith(SIDClassExpressionNamer.className)) disjointX -= 5;

		return (x >= disjointX && x <= disjointX + disjointWidth) &&
				(y >= disjointY && y <= disjointY + disjointHeight);
	}


	public NodeSet<OWLNamedIndividual> getInstances(){
		OWLReasoner reasoner = graph.paintframe.getReasoner();
		return reasoner.getInstances(this.getLinkedClassExpression(), false);
	}
	
	public String removeFormatInformation(String str) {
		return str.replace("\n", "").replace("\t", "");
	}

    private double textHeight(String text) {
        Font font = Font.font("DejaVu Sans", FontWeight.NORMAL, 9);
        int lines = countLines(text) + 1;

        Text helper = new Text("ONTVIEW");
        helper.setFont(font);
        double lineHeight = helper.getLayoutBounds().getHeight();

        return lines * lineHeight;
    }

    private double textHeight(GraphicsContext g, String text) {
        int numLines = countLines(text);
        Text helper = new Text(text);
        helper.setFont(g.getFont());
        return helper.getLayoutBounds().getHeight() + numLines*lineSpacing;
    }
	
	public int countLines(String str) {
		int lines=1; 
		for (int i=0; i<str.length(); i++) {
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

	public void drawFormattedString(GraphicsContext g, String toDraw, int x, int y) {
		StringTokenizer sTokenizer = new StringTokenizer(toDraw,"\n"); 
		int currentX;
		int currentY = y;
		String token; 
		while (sTokenizer.hasMoreTokens()) {
			token = sTokenizer.nextToken();
			currentX = x+tabsSize(token); 
			g.fillText(removeFormatInformation(token), currentX, currentY);
			currentY += VisProperty.stringHeight(g.getFont())+6;
		}
	}

	public List<VisObjectProperty> getProperties() {
		if (propertyBox == null) {
			return Collections.emptyList();
		}
		return propertyBox.getProperties();
	}


}






