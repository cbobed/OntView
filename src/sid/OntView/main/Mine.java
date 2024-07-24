package sid.OntView.main;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
//import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashSet;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.*;

import openllet.owlapi.OpenlletReasonerFactory;

//import org.jdesktop.swingx.autocomplete.*;

import sid.OntView.common.Embedable;
import sid.OntView.common.PaintFrame;
import sid.OntView.common.VisGraph;
import sid.OntView.common.VisPositionConfig;
import sid.OntView.expressionNaming.SIDClassExpressionNamer;
import sid.OntView.utils.ExpressionManager;

import java.security.*;

public class Mine extends JPanel implements Embedable{
	
	private static final long serialVersionUID = 1L;
    boolean DEBUG=false;

    OWLOntology activeOntology;
    OWLReasoner reasoner;
	OWLOntologyManager manager; 
    HashSet<String> entityNameSet;
    
    PaintFrame   artPanel;
    JFileChooser selector;    
    JComboBox    ontologyCombo,reasonerCombo,searchCombo;
    TopPanel     nTopPanel;
    JScrollPane  scroll;
    boolean      firstItemStateChanged = false;
    Mine         self= this;
    boolean      check = true;
	
    /* Code for standalone app initialization */ 
    
    public static void main(String[] args) {

    	SwingUtilities.invokeLater(new Runnable(){
    		public void run() {
    			// TODO Auto-generated method stub
    			createAndShowGUI();
    		}
    	});

	}


	public static void createAndShowGUI() {
    	
    	JFrame frame = new JFrame("Viewer");

        frame.addWindowListener(new java.awt.event.WindowAdapter(){
           public void windowClosing(WindowEvent e){
             System.exit(0);
           }
        }
        );

        Mine viewer = new Mine();
        frame.add(viewer);
        frame.setSize(800,600);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
    	viewer.self = viewer;

    	viewer.entityNameSet = new HashSet<String>();
		viewer.setLayout(new BoxLayout(viewer, BoxLayout.Y_AXIS));
        viewer.artPanel = new PaintFrame();
        viewer.artPanel.setParentFrame(viewer);
        viewer.artPanel.setBorder(BorderFactory.createTitledBorder("Visor"));
        viewer.artPanel.setBackground(new Color(255,255,255));
        viewer.nTopPanel = new TopPanel(viewer);
        viewer.nTopPanel.setAlignmentX(RIGHT_ALIGNMENT);
        viewer.add(viewer.nTopPanel);
       // JScrollPane scroll      = new JScrollPane(artPanel);
        viewer.scroll = new JScrollPane(viewer.artPanel);
        viewer.add(viewer.scroll);
        viewer.setVisible(true);
        viewer.artPanel.scroll = viewer.scroll;
        viewer.validate();
    }
	
	/* Rest of methods */ 
	
    public void createButtonAction(){
    	if (reasoner!= null) {
    		artPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		      //cant cast to set<OWLClassExpression> from set<OWLClass>
		      HashSet<OWLClassExpression> set = new HashSet<OWLClassExpression>();
		      for (OWLClass d : reasoner.getTopClassNode().getEntities()) 
		    	  set.add(d);
			      try {
			    	//set reasoner and ontology before creating 
					artPanel.createReasonedGraph(set,check);
					artPanel.setCursor(Cursor.getDefaultCursor());
				      
				  } 
			      catch (Exception e1) {
					e1.printStackTrace();
					artPanel.setCursor(Cursor.getDefaultCursor());
			      }
			      
			      while (!artPanel.isStable()){
			    	  try {
						Thread.sleep(2000);
						System.err.println("wait");
					  } 
			    	  catch (InterruptedException e) {
						e.printStackTrace();
					  }  
			      }
			      
			      nTopPanel.restoreSliderValue();
	
         }

    	artPanel.start();
}
	
     

	protected void loadActiveOntology(IRI source){
        manager = OWLManager.createOWLOntologyManager();
        artPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
			activeOntology = manager.loadOntologyFromOntologyDocument(source);
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	        artPanel.setCursor(Cursor.getDefaultCursor());
	        activeOntology = null; 
	        manager = null; 
		}
        artPanel.setCursor(Cursor.getDefaultCursor());
        artPanel.setOntology(activeOntology);
        
        artPanel.setActiveOntolgySource(source.toString()); //this might FAIL
        
        // CBL expression manager 
        if (activeOntology != null && manager != null) {
        	ExpressionManager.setNamespaceManager(manager, activeOntology);
        	
        	for (String ns: ExpressionManager.getNamespaceManager().getNamespaces()) {
        		System.err.println("prefix: "+ExpressionManager.getNamespaceManager().getPrefixForNamespace(ns)); 
        		System.err.println("  ns: "+ns); 
        		
        	}
        	
        }
    }
	
	protected void loadActiveOntologyFromString(String ontology) {

		manager = OWLManager.createOWLOntologyManager(); 
		artPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
			activeOntology = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(ontology)); 
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	        artPanel.setCursor(Cursor.getDefaultCursor());
	        activeOntology = null; 
	        manager = null; 
		}
        artPanel.setCursor(Cursor.getDefaultCursor());
        artPanel.setOntology(activeOntology);
        artPanel.setActiveOntolgySource(ontology);
	}
	
	protected void loadActiveOntology(String source){
        manager = OWLManager.createOWLOntologyManager();
        artPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
			activeOntology = manager.loadOntologyFromOntologyDocument(IRI.create(source));
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	        artPanel.setCursor(Cursor.getDefaultCursor());
	        activeOntology = null;
	        manager = null; 
		}
        artPanel.setCursor(Cursor.getDefaultCursor());
        artPanel.setOntology(activeOntology);
	}
	
    protected void loadReasoner(String reasonerString){
    	if (activeOntology!=null) {

    		ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor();
	         // Specify the progress monitor via a configuration.  We could also specify other setup parameters in
	    	 // the configuration, and different reasoners may accept their own defined parameters this way.
	    	 OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor);
	    	 
	    	 // Create a reasoner that will reason over our ontology and its imports closure.  Pass in the configuration.
	         reasoner = getReasonerFactory(reasonerString).createReasoner(activeOntology,config);
	         
	         // between creating and precomputing 
	         applyRenaming();
	         
	         reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
             artPanel.setReasoner(reasoner);
		}
    }
    
    public void applyRenaming(){
		SIDClassExpressionNamer renamer = new SIDClassExpressionNamer(activeOntology, reasoner); 
		renamer.applyNaming(true); 
    }
    
    /*public void refreshOntology (String ontologyString, String reasonerString) {
    	
    	 AccessController.doPrivileged(new RefreshingAction(ontologyString, reasonerString));
    	
    }*/

    class RefreshingAction implements PrivilegedAction {
    	String ontologyString; 
    	String reasonerString; 
    	
    	public RefreshingAction(String ontologyString, String reasonerString) {
    		this.ontologyString = ontologyString; 
    		this.reasonerString = reasonerString; 
    	}
    	
    	public Object run() {
    	
    		loadActiveOntology(IRI.create(ontologyString)); 
	    	if (activeOntology != null) {
	    		nTopPanel.getOntologyCombo().setSelectedItem(ontologyString); 
	    		nTopPanel.getLoadReasonerButton().setEnabled(true);
				nTopPanel.getReasonerCombo().setEnabled(true);
				if (artPanel != null){
					artPanel.setActiveOntolgySource(ontologyString);
				}
	    		loadReasoner(reasonerString);
	    		createButtonAction(); 
	    		
	    	}
	    	else {
	    		System.err.println("Can't load ontology");
	    	}
 
    		return null;
    	}
    }
    /*
    public void refreshOntologyFromString(String ontologyString, String reasonerString) {
    	 AccessController.doPrivileged(new RefreshingFromStringAction(ontologyString, reasonerString));
    }*/
    
    class RefreshingFromStringAction implements PrivilegedAction {
    	String ontologyString; 
    	String reasonerString; 
    	
    	public RefreshingFromStringAction(String ontologyString, String reasonerString) {
    		this.ontologyString = ontologyString; 
    		this.reasonerString = reasonerString; 
    	}
    	
    	public Object run() {
    	
    		loadActiveOntologyFromString(ontologyString); 
	    	if (activeOntology != null) {
	    		nTopPanel.getOntologyCombo().setSelectedItem(activeOntology.getOntologyID().getOntologyIRI()); 
	    		nTopPanel.getLoadReasonerButton().setEnabled(true);
				nTopPanel.getReasonerCombo().setEnabled(true);
				if (artPanel != null){
					artPanel.setActiveOntolgySource(ontologyString);
				}
	    		
	    		loadReasoner(reasonerString);
	    	
	    		createButtonAction(); 
	    		
	    	}
	    	else {
	    		System.err.println("Can't load ontology");
	    	}
 
    		return null;
    	}
    }
    
    
    private OWLReasonerFactory getReasonerFactory(String r){
    	OWLReasonerFactory reasonerFactory = null;
    	if (r.equalsIgnoreCase("Openllet")) {
    		reasonerFactory = new OpenlletReasonerFactory();
    	}
//    	else if (r.equalsIgnoreCase("JFact")) {
//    		reasonerFactory = new JFactFactory();
//    	}
//    	else if (r.equalsIgnoreCase("Elk")) {
//    		reasonerFactory = new ElkReasonerFactory(); 
//    	}
//    	else if (r.equalsIgnoreCase("Jcel")) {
//    		reasonerFactory = new JcelReasonerFactory(); 
//    	}
		return reasonerFactory;
    }
    
	public void saveViewButtonAction(ActionEvent arg0) {
		// TODO Auto-generated method stub
		VisGraph graph = artPanel.getVisGraph();
		if (graph!= null){
			JFileChooser selector = new JFileChooser() ;
			if ((selector.showSaveDialog(self))==JFileChooser.APPROVE_OPTION) {
				String path = null;
				try {	
					path = selector.getSelectedFile().getCanonicalFile().toString();
					VisPositionConfig.saveState(path, graph);
				} catch (Exception e) {
					e.printStackTrace();
				} 				
            }
	     }
	}	
	
	public void restoreViewButtonAction(ActionEvent arg0) {
		// TODO Auto-generated method stub
		if (artPanel.getVisGraph()!= null){
			JFileChooser selector = new JFileChooser();
			if ((selector.showOpenDialog(self))==JFileChooser.APPROVE_OPTION) {
				String path = null;
				try {
					path = selector.getSelectedFile().getCanonicalFile().toString();
				} catch (IOException e) {
					e.printStackTrace();
				}
				VisPositionConfig.restoreState(path, artPanel.getVisGraph());
			}
		}
	}
	public void createImageButtonAction(ActionEvent e) {
		createImage(artPanel);
	}
		
	public void createImage(JPanel panel) {
	    int w = panel.getWidth();
	    int h = panel.getHeight();
	    imageDialog(panel, w, h);
	    
	}
	
	public void createImageFromVisibleRect(JPanel panel){
	    int w = (int) panel.getVisibleRect().getWidth();
	    int h = (int) panel.getVisibleRect().getHeight();
	    
	    // <CBL 25/9/13> 
	    // we have to check the position of the scroll bars
	    
	    int xIni = scroll.getHorizontalScrollBar().getValue();
	    int yIni = scroll.getVerticalScrollBar().getValue(); 
	    
	    System.out.println(w + " "+h+" "+xIni+" "+yIni); 
	    
	    imageDialog(panel, w, h, xIni, yIni);
	}
	
	// <CBL 25/9/13>
	// wrapper for the method
	private void imageDialog(JPanel panel,int w, int h){
		imageDialog(panel, w, h, 0, 0); 
	}
	
	// <CBL 25/9/13>
	// modified to take an offset as argument
		
	private void imageDialog(JPanel panel,int w, int h, int xIni, int yIni){
		File fl = null;
	    boolean done = false;
	    int answer;
	    BufferedImage bi = new BufferedImage(w+xIni,h+yIni, BufferedImage.TYPE_INT_RGB);
	    ArrayList<String> valid = new ArrayList<String>();
	    valid.add("jpg");
	    valid.add("png");
	    Graphics g = bi.createGraphics();
	    panel.paint(g);
	    bi = bi.getSubimage(xIni, yIni, w, h); 
	    
	 	JFileChooser selector = new JFileChooser();
	 	boolean hasSuffix= true;
	    selector.addChoosableFileFilter(new ImageFileFilter("jpg"));
	    selector.addChoosableFileFilter(new ImageFileFilter("png"));
	 	while (!done) {
		 	if ((selector.showSaveDialog(self))==JFileChooser.APPROVE_OPTION) {
		 		fl = selector.getSelectedFile();
		 		
			 	if (fl.exists()){
		           answer =  JOptionPane.showConfirmDialog(self,"Overwrite?","confirm", JOptionPane.OK_CANCEL_OPTION);
		           if (answer == JOptionPane.CANCEL_OPTION){
		        	   continue;
		           }	   
		        }
		        try {
		        	String description =  (valid.contains(selector.getFileFilter().getDescription()) ? 
		        										  selector.getFileFilter().getDescription() : "jpg"); 
		        	hasSuffix = hasValidSuffix(fl.getName().toLowerCase());
		        	if (!hasSuffix){
		        		String newName = fl.getPath()+"."+description;
		        		boolean exists = (new File(newName).exists());
		        		File fl2 = new File(newName);
		        		if (exists){
			        		answer = JOptionPane.showConfirmDialog(self,"Overwrite?","confirm", JOptionPane.OK_CANCEL_OPTION);
					           if (answer == JOptionPane.CANCEL_OPTION){
					        	   continue;
					        }	
		        		}   
		        		ImageIO.write(bi, description, fl2);
		        	}
		        	else {
		        		ImageIO.write(bi,description,fl);
		        	}
		        	done = true;
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		 	}
		 	else {
		 		break;
		 	}
	 	}	
	}
	
	public boolean hasValidSuffix(String in){
		if (in.endsWith("jpg")) return true;
		if (in.endsWith("png")) return true;
		return false;
	}
	public class ImageFileFilter extends FileFilter
	{
	  String fileType;	
	  public ImageFileFilter(String pextension){fileType = pextension;}	
	  public boolean accept(File file){
	      if (file.getName().toLowerCase().endsWith("."+fileType) || (file.isDirectory()))
	        return true;
	    return false;
	  }
	  @Override
	  public String getDescription() {return fileType;}
	}

	@Override
	public void loadSearchCombo() {
		// TODO Auto-generated method stub
	      nTopPanel.loadSearchCombo();
	}
	
}
 
    

