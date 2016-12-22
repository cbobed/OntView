//package sid.OntView.Protege;
//
////import java.awt.BorderLayout;
////import java.awt.Color;
////import java.awt.Dimension;
////import java.awt.FlowLayout;
////import java.awt.event.ActionEvent;
////import java.awt.event.HierarchyEvent;
////import java.awt.event.HierarchyListener;
//
////import javax.swing.AbstractAction;
////import javax.swing.BorderFactory;
////import javax.swing.Box;
////import javax.swing.BoxLayout;
////import javax.swing.JButton;
////import javax.swing.JCheckBox;
////import javax.swing.JComponent;
////import javax.swing.JPanel;
////import javax.swing.JSplitPane;
////import javax.swing.JTextField;
//import java.awt.*;
//import java.awt.event.*;
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.Set;
//import java.util.Map.Entry;
//
//import javax.imageio.ImageIO;
//import javax.swing.*;
//import javax.swing.filechooser.FileFilter;
//import javax.xml.parsers.ParserConfigurationException;
//import javax.xml.xpath.XPathExpressionException;
//
//import org.protege.editor.owl.model.event.EventType;
//import org.protege.editor.owl.model.event.OWLModelManagerChangeEvent;
//import org.protege.editor.owl.model.event.OWLModelManagerListener;
//import org.protege.editor.owl.model.inference.ReasonerStatus;
//import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;
//import org.semanticweb.owlapi.apibinding.OWLManager;
//import org.semanticweb.owlapi.model.IRI;
//import org.semanticweb.owlapi.model.OWLClass;
//import org.semanticweb.owlapi.model.OWLClassExpression;
//import org.semanticweb.owlapi.model.OWLOntology;
//import org.semanticweb.owlapi.model.OWLOntologyCreationException;
//import org.semanticweb.owlapi.reasoner.*;
//import org.xml.sax.SAXException;
//
//import sid.OntView.Protege.util.ExpressionManager;
//import sid.OntView.common.Embedable;
//import sid.OntView.common.PaintFrame;
//import sid.OntView.common.VisGraph;
//import sid.OntView.common.VisPositionConfig;
//import sid.OntView.expressionNaming.SIDClassExpressionNamer;
//
//
//
//
//
//public class OWLClassMainViewComponent extends AbstractOWLViewComponent  implements Embedable{
//    
//	private static final long serialVersionUID = 8268241587271333587L;
//	
//    private OWLModelManagerListener listener;
//    private boolean requiresRefresh = false;
//    OWLClassMainViewComponent mainViewComponent; 
//    OWLOntology activeOntology;
//    OWLReasoner reasoner;
//    PaintFrame artPanel;
//    TopPanel nTopPanel;
//    boolean  DEBUG=false;
//    boolean  check=true;
//    boolean  firstItemStateChanged = false;
//    OWLClassMainViewComponent self= this;
//    
//    protected void initialiseOWLView() throws Exception {
//          
//    	  nTopPanel = new TopPanel(this);
//          this.add(nTopPanel);
//    	  setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
//          mainViewComponent=this;
//          artPanel = new PaintFrame();
//    	  this.loadActiveOntology();
//    	  this.loadReasoner();
//          artPanel.setBorder(BorderFactory.createTitledBorder("Visor"));
//          artPanel.setBackground(new Color(255,255,255));
//          artPanel.setPreferredSize(new Dimension(3000,3000));
//          JScrollPane scroll = new JScrollPane(artPanel);          
//
//          this.add(scroll);
//          this.setVisible(true);
//          
//
//         	
//	 		listener = new OWLModelManagerListener() {
//		        public void handleChange(OWLModelManagerChangeEvent event) {
//		            if (event.isType(EventType.REASONER_CHANGED )) {
//		            	loadReasoner();                	
//		            }
//		        }};
//		    getOWLModelManager().addListener(listener);
//		    
//		    this.addHierarchyListener(new HierarchyListener(){
//	            public void hierarchyChanged(HierarchyEvent event) {
//	                if (requiresRefresh && isShowing()){
//	                //    doQuery();
//	                }
//	            }});
//    } 
//    
//    
//	public void createImage(JPanel panel) {
//	    int w = panel.getWidth();
//	    int h = panel.getHeight();
//	    File fl = null;
//	    boolean done = false;
//	    int answer;
//	    BufferedImage bi = new BufferedImage(w,h, BufferedImage.TYPE_INT_RGB);
//	    ArrayList<String> valid = new ArrayList<String>();
//	    valid.add("jpg");
//	    valid.add("png");
//	    Graphics g = bi.createGraphics();
//	    panel.paint(g);
//	 	JFileChooser selector = new JFileChooser();
//	 	boolean hasSuffix= true;
//	    selector.addChoosableFileFilter(new ImageFileFilter("jpg"));
//	    selector.addChoosableFileFilter(new ImageFileFilter("png"));
//	 	while (!done) {
//		 	if ((selector.showSaveDialog(self))==JFileChooser.APPROVE_OPTION) {
//		 		fl = selector.getSelectedFile();
//		 		
//			 	if (fl.exists()){
//		           answer =  JOptionPane.showConfirmDialog(self,"Overwrite?","confirm", JOptionPane.OK_CANCEL_OPTION);
//		           if (answer == JOptionPane.CANCEL_OPTION){
//		        	   continue;
//		           }	   
//		        }
//		        try {
//		        	String description =  (valid.contains(selector.getFileFilter().getDescription()) ? 
//		        										  selector.getFileFilter().getDescription() : "jpg"); 
//		        	hasSuffix = hasValidSuffix(fl.getName().toLowerCase());
//		        	if (!hasSuffix){
//		        		String newName = fl.getPath()+"."+description;
//		        		boolean exists = (new File(newName).exists());
//		        		File fl2 = new File(newName);
//		        		if (exists){
//			        		answer = JOptionPane.showConfirmDialog(self,"Overwrite?","confirm", JOptionPane.OK_CANCEL_OPTION);
//					           if (answer == JOptionPane.CANCEL_OPTION){
//					        	   continue;
//					        }	
//		        		}   
//		        		ImageIO.write(bi, description, fl2);
//		        	}
//		        	else {
//		        		ImageIO.write(bi,description,fl);
//		        	}
//		        	done = true;
//				} catch (IOException e1) {
//					e1.printStackTrace();
//				}
//		 	}
//		 	else {
//		 		break;
//		 	}
//	 	}	
//	}
//	
//	private boolean hasValidSuffix(String in){
//		if (in.endsWith("jpg")) return true;
//		if (in.endsWith("png")) return true;
//		return false;
//	}
//	public class ImageFileFilter extends FileFilter
//	{
//	  String fileType;	
//	  public ImageFileFilter(String pextension){fileType = pextension;}	
//	  public boolean accept(File file){
//	      if (file.getName().toLowerCase().endsWith("."+fileType) || (file.isDirectory()))
//	        return true;
//	    return false;
//	  }
//	  @Override
//	  public String getDescription() {return fileType;}
//	}
//
//    protected void disposeOWLView() {getOWLModelManager().removeListener(listener);}
//    protected void loadActiveOntology(){
//    	activeOntology=getOWLModelManager().getActiveOntology(); 
//    	artPanel.setOntology(activeOntology);
//    	ExpressionManager.setNamespaceManager(getOWLModelManager().getOWLOntologyManager(), activeOntology); 
//		
//    	System.err.println("activeOntology:"+activeOntology); 
//    	System.err.println("manager:"+getOWLModelManager().getOWLOntologyManager()); 
//    }
//    protected void loadReasoner(){
//    	reasoner=getOWLModelManager().getReasoner(); 
//    	applyRenaming();
//    	artPanel.setReasoner(reasoner);
//    	
//    }
//    
//    
//    
//    public void applyRenaming(){
//		SIDClassExpressionNamer renamer = new SIDClassExpressionNamer(activeOntology, reasoner); 
//		renamer.applyNaming(true); 
//    }
//    
//    public void createButtonAction(){
//    	
//    	// CBL
//		
//    	if (reasoner!= null) {
//    		ExpressionManager.setNamespaceManager(getOWLModelManager().getOWLOntologyManager(), getOWLModelManager().getActiveOntology()); 
//    		  artPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//		      //cant cast to set<OWLClassExpression> from set<OWLClass>
//		      HashSet<OWLClassExpression> set = new HashSet<OWLClassExpression>();
//		      for (OWLClass d : reasoner.getTopClassNode().getEntities()) 
//		    	  set.add(d);
//			      try {
//			    	//set reasoner and ontology before creating 
//					artPanel.createReasonedGraph(set,check);
//					artPanel.setCursor(Cursor.getDefaultCursor());
//				      
//				  } 
//			      catch (Exception e1) {
//					// e1.printStackTrace();
//					artPanel.setCursor(Cursor.getDefaultCursor());
//			      }
//			      nTopPanel.loadSearchCombo();
//			      nTopPanel.restoreSliderValue();
//
//         }
//    	
//	   //start the relaxer thread
//     artPanel.start();
//     artPanel.run();
//}
//    
//
//public void saveViewButtonAction(ActionEvent arg0) {
//	// TODO Auto-generated method stub
//	VisGraph graph = artPanel.getVisGraph();
//	if (graph!= null){
//		JFileChooser selector = new JFileChooser() ;
//		if ((selector.showSaveDialog(self))==JFileChooser.APPROVE_OPTION) {
//			String path = null;
//			try {	
//				path = selector.getSelectedFile().getCanonicalFile().toString();
//				VisPositionConfig.saveState(path, graph);
//			} catch (Exception e) {
//				e.printStackTrace();
//			} 				
//        }
//     }
//}	
//
//public void restoreViewButtonAction(ActionEvent arg0) {
//	// TODO Auto-generated method stub
//	if (artPanel.getVisGraph()!= null){
//		JFileChooser selector = new JFileChooser();
//		if ((selector.showOpenDialog(self))==JFileChooser.APPROVE_OPTION) {
//			String path = null;
//			try {
//				path = selector.getSelectedFile().getCanonicalFile().toString();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			VisPositionConfig.restoreState(path, artPanel.getVisGraph());
//		}
//	}
//}
//public void createImageButtonAction(ActionEvent e) {
//		   createImage(artPanel);
//	    }
//
//
//@Override
//public void loadSearchCombo() {
//	// TODO Auto-generated method stub
//	
//}
//	
//
//}
//
