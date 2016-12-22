package sid.OntView.common;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Map.Entry;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.coode.owlapi.functionalparser.Node;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.reasoner.NodeSet;

public class VisShapeContext extends JPopupMenu{
	
	JMenu objPropMenu,datPropMenu;
	JMenuItem hideItem;
	JMenuItem hideProperties;
	JMenuItem hideRanges;
	JMenuItem showInstances;
	ArrayList<JMenuItem> objectPropertiesList,dataPropertiesList;
	Shape shape;
	PaintFrame parent;
	OWLClassExpression expression;
	
	int posx,posy;
	public VisShapeContext (Shape s,PaintFrame parentFrame,MouseEvent e){
		
		super();
		shape =s;
		posx =e.getXOnScreen();
		posy =e.getYOnScreen();
		parent= parentFrame;
		boolean visc = shape instanceof VisClass;
		expression = shape.asVisClass().getLinkedClassExpression();
       
		hideProperties = new JMenuItem("Hide/Show Properties");
		
		
		if ((!visc) || (visc && shape.asVisClass().getPropertyBox() ==null))
			hideProperties.setEnabled(false);
		hideProperties.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				boolean b =shape.asVisClass().getPropertyBox().visible;
				shape.asVisClass().getPropertyBox().setVisible(!b);			}
		});
		
		hideItem = new JMenuItem("Hide");
		
		
		if (!shape.asVisClass().allSubHidden()) {
			hideItem.setEnabled(false);
		}
		hideItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				shape.hide();
			}
		});
		
		
		this.add(getShowInstancesItem());
		this.add(hideProperties);
		this.add(hideItem);
		
	}
	
	private JMenuItem getShowInstancesItem(){
		if (showInstances == null) {
			showInstances = new JMenuItem("show instances");
			showInstances.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					showInstancesAction();
				}
			});
		}
		return showInstances;
	}
	
	private void showInstancesAction(){
		ArrayList<String> instanceArray = new ArrayList<String>();
		if ((shape instanceof VisClass)){
			NodeSet<OWLNamedIndividual> instanceSet = ((VisClass) shape).getInstances();
			for (org.semanticweb.owlapi.reasoner.Node<OWLNamedIndividual>  instanceNode : instanceSet.getNodes() ){
				for (OWLNamedIndividual instance : instanceNode.getEntities()){
					instanceArray.add(instance.getIRI().getFragment());
				}
				
			}
			VisInstance c = new VisInstance();
			c.setTitle("instances of "+shape.asVisClass().label);
			c.setModel(instanceArray);
			c.setLocation(posx,posy);
			c.setVisible(true);
		
		}
	}
	
}