package sid.OntView.common;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.io.UTFDataFormatException;
import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;

import org.coode.owlapi.rdfxml.parser.ObjectOneOfTranslator;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.DataRangeType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLRestriction;
import org.semanticweb.owlapi.reasoner.Node;

import sid.OntView.utils.ExpressionManager;

public class VisConstraint extends Shape {
	public static final int RELATIVE_POS =0;
	String label;
	String qualifiedLabel = ""; 
    String property = "";
    String filler= "";
	OWLClassExpression linkedClassExpression; 
	
	public VisConstraint(VisLevel level,OWLClassExpression o, String plabel,VisGraph pgraph) {
	/*
	 * constructor for the VisConstraint class	
	 */
		super();
		
		setVisLevel(level);
		graph = pgraph;
		setPosX(0); 
		setPosY((int) (10 + 500*Math.random()));
		setHeight(20); 
		setWidth(20);
		linkedClassExpression=o;
		this.label= plabel;
		
		System.err.println("Constraint Label: "+this.label);
		
		connectionPointsL = new Point(posx-getWidth()/2,posy+getHeight()/2);
		connectionPointsR = new Point(posx-getWidth()/2,posy+getHeight()/2);
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
      	      OWLDataSomeValuesFrom dsome=(OWLDataSomeValuesFrom) o;
        	  property= ExpressionManager.getReducedDataPropertyExpression(dsome.getProperty());
        	  filler = ExpressionManager.getReducedDataRange(dsome.getFiller());

        	  break;
          
          case DATA_HAS_VALUE:
        	  OWLDataHasValue dhasv=(OWLDataHasValue) o;
        	  property= ExpressionManager.getReducedDataPropertyExpression(dhasv.getProperty());
        	  filler = "'"+dhasv.getValue().getLiteral()+"'";
        	  break;
          
          case DATA_EXACT_CARDINALITY:
      	      OWLDataExactCardinality dexact=(OWLDataExactCardinality) o;
        	  property= ExpressionManager.getReducedDataPropertyExpression(dexact.getProperty());
        	  break;
          
          case DATA_MAX_CARDINALITY:
      	      OWLDataMaxCardinality dmax=(OWLDataMaxCardinality) o;
      	      property= ExpressionManager.getReducedDataPropertyExpression(dmax.getProperty());
      	      break;
          
          case DATA_MIN_CARDINALITY:
      	      OWLDataMinCardinality dmin=(OWLDataMinCardinality) o;
        	  property= dmin.getProperty().asOWLDataProperty().getIRI().getFragment();
        	  break;
          default :
	    }
	}

	
	
	@Override
	 public void drawShape(Graphics g) {
		int x = posx+1;
		int y = posy;
		String draw;
		draw = label;
		// TODO Auto-generated method stub
		
		ClassExpressionType type = linkedClassExpression.getClassExpressionType();
		if (!property.equals("")){
			draw = label+"("+property;
			if (!filler.equals("")){
				draw += ","+filler+")";
			}
			else {
				draw+=")";
			}	
		}
		
		Graphics2D g2d = (Graphics2D)g;
		if (visible) {				
			FontMetrics f = g2d.getFontMetrics();
			Font prev = g2d.getFont();
			g2d.setFont(new Font(Font.DIALOG, Font.PLAIN,9));
			Color c = g2d.getColor();
			g2d.setColor(Color.lightGray);
			
			g2d.fillOval(x-getWidth()/2, y, getWidth(), getHeight());
			g2d.setColor(c);
			String auxStr;
			switch (type) {

				case OBJECT_INTERSECTION_OF:
				case OBJECT_UNION_OF:
					g2d.setFont(new Font(Font.DIALOG, Font.BOLD,14));
					g2d.drawString(draw,getPosX()-getWidth()/4, getPosY()+getHeight()/2+2);
					g2d.setFont(new Font(Font.DIALOG, Font.BOLD,9));
					break;	
				case OBJECT_HAS_VALUE :	
					g2d.drawString(property+" {"+filler+"}",getPosX()-getWidth()/2, getPosY());
				case DATA_HAS_VALUE :
					g2d.drawString(property+"="+filler,getPosX()-getWidth()/2, getPosY());
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
					g2d.drawString(auxStr,getPosX()-getWidth()/2, getPosY());
					break;
				default:
					f.getHeight();
					g2d.drawString(draw,getPosX()-getWidth()/2, getPosY());
			}
			g2d.drawOval(x-getWidth()/2, y, getWidth(), getHeight());
			g2d.setColor(c);
			g2d.setFont(prev);
		}	
	}

	@Override
	public Point getConnectionPoint(Point p,boolean left) {
	//return Closest conection point	
		if (left){
			connectionPointsL.x = getPosX()-getWidth()/2;
			connectionPointsL.y = getPosY()+getHeight()/2;
			return connectionPointsL;
		}
     	else {
			connectionPointsR.x = getPosX()+getWidth()/2;
			connectionPointsR.y = getPosY()+getHeight()/2;
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
	

	
	@Override
	public int getLevelRelativePos() {
		return RELATIVE_POS;
	}

	
	public OWLClassExpression getLinkedClassExpression (){
		return linkedClassExpression;
	}

	
}
