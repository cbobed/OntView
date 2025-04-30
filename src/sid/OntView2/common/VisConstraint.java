package sid.OntView2.common;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;

import sid.OntView2.utils.ExpressionManager;

public class VisConstraint extends Shape {
	public static final int RELATIVE_POS =0;
	String label;
    String property = "";
    String filler= "";
	OWLClassExpression linkedClassExpression; 
	
	public VisConstraint(VisLevel level, OWLClassExpression o, String pLabel, VisGraph pGraph) {
	/*
	 * constructor for the VisConstraint class	
	 */
		super();
		
		setVisLevel(level);
		graph = pGraph;
		setPosX(0); 
		setPosY((int) (10 + 500*Math.random()));
		setHeight(20); 
		setWidth(20);
		linkedClassExpression=o;
		this.label= pLabel;

		connectionPointsL = new Point2D(posx- (double) getWidth() /2,posy+ (double) getHeight() /2);
		connectionPointsR = new Point2D(posx- (double) getWidth() /2,posy+ (double) getHeight() /2);
		setVisLevel(level);
	    ClassExpressionType type = o.getClassExpressionType();
		switch (type){
          
          case OBJECT_ALL_VALUES_FROM: 
        	  OWLObjectAllValuesFrom all=(OWLObjectAllValuesFrom) o;
        	  property= ExpressionManager.getReducedObjectPropertyExpression(all.getProperty());
        	  filler = ExpressionManager.getReducedClassExpression(all.getFiller());
        	  break;
          case OBJECT_COMPLEMENT_OF:
        	  break;        	  
          case OBJECT_ONE_OF:
        	  OWLObjectOneOf oneOf = (OWLObjectOneOf) o;
        	  label = "one of(";
        	  for (OWLClass op :oneOf.getClassesInSignature()) {
				  label += ExpressionManager.getReducedClassExpression(op)+" "; 
			  }	 
        	  label +=")";
        	  break;
          case OBJECT_SOME_VALUES_FROM:
      	      OWLObjectSomeValuesFrom some=(OWLObjectSomeValuesFrom) o;        	  
        	  property= ExpressionManager.getReducedObjectPropertyExpression(some.getProperty());
        	  filler = ExpressionManager.getReducedClassExpression(some.getFiller());
        	  break;
          
          case OBJECT_HAS_VALUE:
      	      OWLObjectHasValue hasV=(OWLObjectHasValue) o;
        	  property= ExpressionManager.getReducedObjectPropertyExpression(hasV.getProperty()); 
        	  filler = hasV.getValue().asOWLNamedIndividual().getIRI().getFragment();
        	  break;
          
          case OBJECT_HAS_SELF:
      	      OWLObjectHasSelf hasS=(OWLObjectHasSelf) o;
        	  property= hasS.getProperty().asOWLObjectProperty().getIRI().getFragment();
        	  break;
          
          case OBJECT_EXACT_CARDINALITY:
      	      OWLObjectExactCardinality exact=(OWLObjectExactCardinality) o;
        	  property= ExpressionManager.getReducedObjectPropertyExpression(exact.getProperty());//      
        	  filler = ExpressionManager.getReducedClassExpression(exact.getFiller());
        	  break;
    	  
          case OBJECT_MAX_CARDINALITY:
      	      OWLObjectMaxCardinality max=(OWLObjectMaxCardinality) o;
         	  property= ExpressionManager.getReducedObjectPropertyExpression(max.getProperty());
//        	  filler = all.getFiller().asOWLClass().getIRI().getFragment();
        	  filler = ExpressionManager.getReducedClassExpression(max.getFiller());
        	  break;
          
    	  case OBJECT_MIN_CARDINALITY:
      	      OWLObjectMinCardinality min=(OWLObjectMinCardinality) o;
        	  property= ExpressionManager.getReducedObjectPropertyExpression(min.getProperty());
        	  filler = ExpressionManager.getReducedClassExpression(min.getFiller());

        	  break;
          
          case DATA_ALL_VALUES_FROM: 
      	      OWLDataAllValuesFrom dall=(OWLDataAllValuesFrom) o;
        	  property= ExpressionManager.getReducedDataPropertyExpression(dall.getProperty());
        	  filler = ExpressionManager.getReducedDataRange(dall.getFiller());
        	  break;
          
          case DATA_SOME_VALUES_FROM:
      	      OWLDataSomeValuesFrom dSome=(OWLDataSomeValuesFrom) o;
        	  property= ExpressionManager.getReducedDataPropertyExpression(dSome.getProperty());
        	  filler = ExpressionManager.getReducedDataRange(dSome.getFiller());

        	  break;
          
          case DATA_HAS_VALUE:
        	  OWLDataHasValue dHasV=(OWLDataHasValue) o;
        	  property= ExpressionManager.getReducedDataPropertyExpression(dHasV.getProperty());
        	  filler = "'"+dHasV.getValue().getLiteral()+"'";
        	  break;
          
          case DATA_EXACT_CARDINALITY:
      	      OWLDataExactCardinality dExact=(OWLDataExactCardinality) o;
        	  property= ExpressionManager.getReducedDataPropertyExpression(dExact.getProperty());
        	  break;
          
          case DATA_MAX_CARDINALITY:
      	      OWLDataMaxCardinality dMax=(OWLDataMaxCardinality) o;
      	      property= ExpressionManager.getReducedDataPropertyExpression(dMax.getProperty());
      	      break;
          
          case DATA_MIN_CARDINALITY:
      	      OWLDataMinCardinality dMin=(OWLDataMinCardinality) o;
        	  property= dMin.getProperty().asOWLDataProperty().getIRI().getFragment();
        	  break;
          default :
	    }
	}

	@Override
	public String getLabel() {
		return label;
	}
	
	
	@Override
	public void drawShape(GraphicsContext g) {
		int x = posx+1;
		int y = posy;
		String draw;
		draw = label;
		// TODO Auto-generated method stub
		
		ClassExpressionType type = linkedClassExpression.getClassExpressionType();
		if (!property.isEmpty()){
			draw = label+"("+property;
			if (!filler.isEmpty()){
				draw += ","+filler+")";
			}
			else {
				draw+=")";
			}	
		}

        if (visible) {
			Font prevFont = g.getFont();
			Color prevColor = (Color) g.getFill();

			// Set font and fill color for background oval
			g.setFont(Font.font("DejaVu Sans", FontWeight.NORMAL, 9));
			g.setFill(Color.BLACK);
			g.fillOval(x - (double) getWidth() / 2, y, getWidth(), getHeight());

			// Set color for text
			g.setFill(prevColor);

			String auxStr;
			switch (type) {

				case OBJECT_INTERSECTION_OF:
				case OBJECT_UNION_OF:
					g.setFont(Font.font("DejaVu Sans", FontWeight.BOLD, 14));
					g.fillText(draw,getPosX() - (double) getWidth() /4, getPosY() + (double) getHeight() /2+2);
					g.setFont(Font.font("DejaVu Sans", FontWeight.BOLD, 9));
					break;
				case OBJECT_HAS_VALUE :	
					g.fillText(property+" {"+filler+"}",getPosX() - (double) getWidth() /2, getPosY());
				case DATA_HAS_VALUE :
					g.fillText(property+"="+filler,getPosX() - (double) getWidth() /2, getPosY());
					break;
				case OBJECT_EXACT_CARDINALITY:
				case OBJECT_MAX_CARDINALITY:
				case OBJECT_ALL_VALUES_FROM:	
				case OBJECT_MIN_CARDINALITY:
				case DATA_MAX_CARDINALITY:
				case DATA_MIN_CARDINALITY:
				case DATA_ALL_VALUES_FROM:	
				case DATA_EXACT_CARDINALITY:	
					auxStr = "/ "+property+"/ "+label;
					g.fillText(auxStr,getPosX() - (double) getWidth() /2, getPosY());
					break;
				default:
					g.fillText(draw,getPosX() - (double) getWidth() /2, getPosY());
			}
			g.strokeOval(x-getWidth()/2.0, y, getWidth(), getHeight());
			g.setStroke(prevColor);
			g.setFont(prevFont);
		}	
	}

    @Override
    public void paintFocus(GraphicsContext g) {
    }

    @Override
	public Point2D getConnectionPoint(Point2D p,boolean left) {
	//return Closest connection point
		if (left){
			connectionPointsL = new Point2D(getPosX() - (double) getWidth() /2, getPosY() + (double) getHeight() /2);
			return connectionPointsL;
		}
     	else {
			connectionPointsR = new Point2D(getPosX() + (double) getWidth() /2, getPosY() + (double) getHeight() /2);
		    return connectionPointsR;
		}
	}
	
	
	@Override
	public String getToolTipInfo() {
	/*
	 * Renders html for class info
	 */
		String retVal = "<html><b>"+ExpressionManager.getReducedClassExpression(getLinkedClassExpression())+"</b>";
		retVal= retVal + "</html>";
		return retVal;
	}
	
	public OWLClassExpression getLinkedClassExpression (){
		return linkedClassExpression;
	}

	
}
