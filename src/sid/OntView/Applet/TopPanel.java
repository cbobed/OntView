package sid.OntView.Applet;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import org.dyno.visual.swing.layouts.Bilateral;
import org.dyno.visual.swing.layouts.Constraints;
import org.dyno.visual.swing.layouts.GroupLayout;
import org.dyno.visual.swing.layouts.Leading;
import org.semanticweb.owlapi.model.IRI;

import sid.OntView.common.AutoCompletion;
import sid.OntView.common.ControlPanelInterface;
import sid.OntView.common.Shape;
import sid.OntView.common.VisClass;
import sid.OntView.common.VisConstants;

//VS4E -- DO NOT REMOVE THIS LINE!
public class TopPanel extends JPanel implements ControlPanelInterface {

	private static final long serialVersionUID = 1L;
	private JButton loadOntologyButton;
	private JComboBox loadOntologyCombo;
	private JComboBox loadReasonerCombo;
	private JButton loadReasonerButton;
	private JPanel jPanelLoad;
	private JCheckBox expandCheckBox;
	private JPanel jPanel1;
	private JButton saveViewButton;
	private JButton restoreViewButton;
	private JPanel ViewPanel;
	private JButton saveImageButton;
	private JButton saveImagePartialButton;
	private JLabel jLabel0;
	private JComboBox jComboBox0;
	private JComboBox kceComboBox;
	private JPanel jPanel0;
	private Mine parent;
    static String  RESOURCE_BASE ;
	private JCheckBox Properties;
	private JButton fileSystemButton;
	private JSlider zoomSlider;
	private JCheckBox reduceCheckBox;
	private JLabel reduceLabel;
	public TopPanel(Mine pparent){
		parent = pparent;
		initComponents();
	}
	public TopPanel() {
		initComponents();
		
	}

	private void initComponents() {
		setBorder(BorderFactory.createCompoundBorder(null, null));
		setMaximumSize(new Dimension(2147483647, 90));
		setLayout(new GroupLayout());
		add(getJPanelLoad(), new Constraints(new Leading(12, 523, 12, 12), new Leading(10, 78, 12, 12)));
		add(getZoomSlider(), new Constraints(new Leading(750, 24, 12, 12), new Leading(2, 94, 12, 12)));
		add(getJPanel1(), new Constraints(new Leading(541, 197, 10, 10), new Leading(10, 78, 12, 12)));
     	add(getSnapshotPanel(), new Constraints(new Leading(968, 120, 10, 10), new Leading(2, 51, 12, 12)));
		add(getJPanel0(), new Constraints(new Leading(786, 300, 12, 12), new Leading(54, 34, 12, 12)));
		add(getViewPanel(), new Constraints(new Leading(786, 179, 10, 10), new Leading(2, 51, 12, 12)));
		setSize(1040, 108);
	}
	
	private JPanel getSnapshotPanel() {
		if (snapshotPanel == null) {
			snapshotPanel = new JPanel();
			snapshotPanel.setBorder(BorderFactory.createTitledBorder(null, "Snapshot", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION, new Font("Dialog",
					Font.ITALIC, 10), Color.blue));
			snapshotPanel.setLayout(new GroupLayout());
			snapshotPanel.add(getSaveImageButton(), new Constraints(new Leading(8, 30, 10, 10), new Leading(0, 26, 11, 12)));
			snapshotPanel.add(getSaveImagePartialButton(), new Constraints(new Leading(45, 30, 10, 10), new Leading(0, 26, 12, 12)));
			
		}
		return snapshotPanel;
	}
	
	private Component getSaveImagePartialButton() {
		if (saveImagePartialButton == null) {
			saveImagePartialButton = new JButton();
			ClassLoader c = Thread.currentThread().getContextClassLoader();
			saveImagePartialButton.setIcon(new ImageIcon(c.getResource("saveImageParcial.JPG")));
			saveImagePartialButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					saveImageButtonPartialActionActionPerformed(event);
				}
			});
		}
		return saveImagePartialButton;
	}
	
	private JCheckBox getQualifiedNames() {
		if (qualifiedNames == null) {
			qualifiedNames = new JCheckBox();
			qualifiedNames.setFont(new Font("Dialog", Font.PLAIN, 10));
			qualifiedNames.setText("qualified names");
			qualifiedNames.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					// TODO Auto-generated method stub
					qualifiedNamesActionActionPerformed(event);
				}
			}); 
		}
		return qualifiedNames;
	}
	
	public void qualifiedNamesActionActionPerformed(ActionEvent event) {
		if (parent.artPanel!= null){
			parent.artPanel.qualifiedNames = !parent.artPanel.qualifiedNames;
			parent.artPanel.getVisGraph().changeRenderMethod(parent.artPanel.renderLabel, parent.artPanel.qualifiedNames);
		}
	}
	
	private JCheckBox getRenderLabel() {
		if (renderLabel == null) {
			renderLabel = new JCheckBox();
			renderLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
			renderLabel.setText("label");
			renderLabel.addActionListener(new ActionListener() {
	
				public void actionPerformed(ActionEvent event) {
					renderLabelActionActionPerformed(event);
				}
			});
		}
		return renderLabel;
	}
	public void renderLabelActionActionPerformed(ActionEvent event) {
		if (parent.artPanel!= null){
			parent.artPanel.renderLabel = !parent.artPanel.renderLabel;
			parent.artPanel.getVisGraph().changeRenderMethod(parent.artPanel.renderLabel, parent.artPanel.qualifiedNames);
		}
	}

	private JSlider getZoomSlider() {
		if (zoomSlider == null) {
			zoomSlider = new JSlider();
			zoomSlider.setMaximum(25);
			zoomSlider.setMinorTickSpacing(3);
			zoomSlider.setOrientation(SwingConstants.VERTICAL);
			zoomSlider.setValue(5);
			zoomSlider.setFocusCycleRoot(true);
			zoomSlider.addChangeListener(new ChangeListener() {
	
				public void stateChanged(ChangeEvent event) {
					zoomSliderChangeStateChanged(event);
				}
			});
		}
		return zoomSlider;
	}
	private JButton getfileSystemButton() {
		if (fileSystemButton == null) {
			fileSystemButton = new JButton();

			ClassLoader c = Thread.currentThread().getContextClassLoader();
			ImageIcon icono = new ImageIcon(c.getResource("folder.png"));		
			Image img = icono.getImage();
			Image newimg = img.getScaledInstance(17,17, Image.SCALE_SMOOTH);  
			icono = new ImageIcon(newimg); 
			fileSystemButton.setIcon(icono);

			fileSystemButton.addActionListener(new ActionListener() {
	
				public void actionPerformed(ActionEvent event) {
					fileSystemButtonActionActionPerformed(event);
				}
			});
		}
		return fileSystemButton;
	}
	private JCheckBox getPropertiesCheckBox() {
		if (Properties == null) {
			Properties = new JCheckBox();
			Properties.setFont(new Font("Dialog", Font.PLAIN, 10));
			Properties.setText("properties");
			Properties.addActionListener(new ActionListener() {
	
				public void actionPerformed(ActionEvent event) {
					PropertiesActionActionPerformed(event);
				}
			});
		}
		return Properties;
	}
	private JPanel getJPanel0() {
		if (jPanel0 == null) {
			jPanel0 = new JPanel();
			jPanel0.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED, null, null));
			jPanel0.setLayout(new GroupLayout());
			jPanel0.add(getJLabel0(), new Constraints(new Leading(8, 22, 10, 10), new Leading(1, 24, 12, 12)));
			jPanel0.add(getJComboBox0(), new Constraints(new Bilateral(39, 12, 126), new Leading(4, 21, 12, 12)));
		}
		return jPanel0;
	}
	private JComboBox getJComboBox0() {
		if (jComboBox0 == null) {
			jComboBox0 = new JComboBox();
			AutoCompletion.enable(jComboBox0);
			jComboBox0.setEditable(true);
			jComboBox0.setFont(new Font("Dialog", Font.BOLD, 10));
			jComboBox0.setForeground(Color.blue);
			jComboBox0.setModel(new DefaultComboBoxModel(new Object[] {}));
			jComboBox0.setDoubleBuffered(false);
			jComboBox0.setBorder(null);
//			jComboBox0.setRequestFocusEnabled(FALSE);
		
//			jComboBox0.addActionListener(new ActionListener() {
//				
//				@Override
//				public void actionPerformed(ActionEvent arg0) {
//					if (parent.artPanel.getVisGraph() != null){
//						System.out.println("action");
//						loadSearchCombo();
//					}
//					
//				}
//			});
			jComboBox0.addItemListener(new ItemListener() {
	
				public void itemStateChanged(ItemEvent event) {
					jComboBox0ItemItemStateChanged(event);
				}
			});
		}
		return jComboBox0;
	}
	
	
	private JComboBox getKceComboBox() {
		if (kceComboBox == null) {
			kceComboBox = new JComboBox();
			AutoCompletion.enable(kceComboBox);
			kceComboBox.setEditable(true);
			kceComboBox.setFont(new Font("Dialog", Font.BOLD, 10));
			kceComboBox.setForeground(Color.blue);
			kceComboBox.setModel(new DefaultComboBoxModel(new Object[] {}));
			kceComboBox.setDoubleBuffered(false);
			kceComboBox.setBorder(null);
			kceComboBox.setRequestFocusEnabled(false);
			fillKceComboBox();
			if (parent.artPanel!=null) {
				parent.artPanel.setKceOption((String)getKceComboBox().getItemAt(0));
			}
			
			kceComboBox.addItemListener(new ItemListener() {
	        	public void itemStateChanged(ItemEvent event) {
					kceItemItemStateChanged(event);
				}
			});
		}
		return kceComboBox;
	}
	
	private void fillKceComboBox(){
		getKceComboBox().addItem(VisConstants.KCECOMBOOPTION1);
		getKceComboBox().addItem(VisConstants.KCECOMBOOPTION2);
		getKceComboBox().addItem(VisConstants.KCECOMBOOPTION3);

	}
	
	protected void kceItemItemStateChanged(ItemEvent event) {
		// TODO Auto-generated method stub
		if (parent.artPanel!=null) {
			parent.artPanel.setKceOption((String)getKceComboBox().getSelectedItem());
			parent.artPanel.doKceOptionAction();
		}
		
	}
	private JLabel getJLabel0(){
	if(jLabel0==null){
		jLabel0 = new JLabel();
		ClassLoader c = Thread.currentThread().getContextClassLoader();
		jLabel0.setIcon(new ImageIcon(c.getResource("search.JPG")));

	}
	return jLabel0;
	}

	private JCheckBox getExpandCheckBox() {
		if (expandCheckBox == null) {
			expandCheckBox = new JCheckBox();
			expandCheckBox.setFont(new Font("Dialog", Font.PLAIN, 10));
			expandCheckBox.setText("expand");
			expandCheckBox.addActionListener(new ActionListener() {
	
				public void actionPerformed(ActionEvent event) {
					expandCheckBoxActionActionPerformed(event);
				}
			});
		}
		return expandCheckBox;
	}
	public JButton getLoadReasonerButton() {
		if (loadReasonerButton == null) {
			loadReasonerButton = new JButton();
			loadReasonerButton.setFont(new Font("Dialog", Font.PLAIN, 10));
			loadReasonerButton.setForeground(Color.blue);
			loadReasonerButton.setText("Sync");
			loadReasonerButton.addActionListener(new ActionListener() {
	
				public void actionPerformed(ActionEvent event) {
					loadReasonerButtonActionActionPerformed(event);
				}
			});
		}
		return loadReasonerButton;
	}
	
	
	public JLabel getReduceLabel() {
		if (reduceLabel == null) {
			reduceLabel = new JLabel("reduce");
			reduceLabel.setText("reduce");
			reduceLabel.setFont(new Font("Dialog", Font.PLAIN, 9));
		}
		return reduceLabel;
	}
	
	
	private JPanel getJPanelLoad() {
		if (jPanelLoad == null) {
			jPanelLoad = new JPanel();
			jPanelLoad.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED, null, null));
			jPanelLoad.setLayout(new GroupLayout());
			jPanelLoad.add(getfileSystemButton(), new Constraints(new Leading(404, 24, 10, 10), new Leading(12, 23, 12, 12)));
			jPanelLoad.add(getOntologyCombo(), new Constraints(new Leading(10, 388, 12, 12), new Leading(12, 12, 12)));
			jPanelLoad.add(getLoadOntologyButton(), new Constraints(new Leading(434, 10, 10), new Leading(12, 23, 12, 12)));
			jPanelLoad.add(getLoadReasonerButton(), new Constraints(new Leading(440, 71, 10, 10), new Leading(45, 23, 12, 12)));
			jPanelLoad.add(getReasonerCombo(), new Constraints(new Leading(10, 358, 12, 12), new Leading(45, 12, 12)));
//			jPanelLoad.add(getReduceCheckBox(), new Constraints(new Leading(407, 24, 10, 10), new Leading(44, 15, 12, 12)));
//			jPanelLoad.add(getReduceLabel(), new Constraints(new Leading(403, 40, 10, 10), new Leading(52, 23, 12, 12)));
			jPanelLoad.add(getKceComboBox(),new Constraints(new Leading(373, 60, 10, 10), new Leading(45, 23, 12, 12)) );


		
		}
		return jPanelLoad;
	}
	private JCheckBox getReduceCheckBox() {
		if (reduceCheckBox == null) {
			reduceCheckBox = new JCheckBox();
			reduceCheckBox.setFont(new Font("Dialog", Font.PLAIN, 10));
			reduceCheckBox.addActionListener(new ActionListener() {
	
				public void actionPerformed(ActionEvent event) {
					reduceActionActionPerformed(event);
				}
			});
		}
		return reduceCheckBox;
	}
	
	private JButton getSaveImageButton() {
		if (saveImageButton == null) {
			saveImageButton = new JButton();
			ClassLoader c = Thread.currentThread().getContextClassLoader();
			saveImageButton.setIcon(new ImageIcon(c.getResource("saveImage.JPG")));
			saveImageButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					saveImageButtonActionActionPerformed(event);
				}
			});
		}
		return saveImageButton;
	}

	private JPanel getViewPanel() {
		if (ViewPanel == null) {
			ViewPanel = new JPanel();
			ViewPanel.setBorder(BorderFactory.createTitledBorder(null, "View", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD
					| java.awt.Font.ITALIC, 10), Color.blue));
			ViewPanel.setFont(new Font("Dialog", Font.BOLD | java.awt.Font.ITALIC, 10));
			ViewPanel.setLayout(new GroupLayout());
			ViewPanel.add(getSaveViewButton(), new Constraints(new Leading(6, 76, 12, 12), new Leading(-4, 28, 10, 10)));
			ViewPanel.add(getRestoreViewButton(), new Constraints(new Leading(88, 76, 12, 12), new Leading(-4, 28, 12, 12)));
		}
		return ViewPanel;
	}
	private JButton getRestoreViewButton() {
		if (restoreViewButton == null) {
			restoreViewButton = new JButton();
			restoreViewButton.setFont(new Font("Dialog", Font.PLAIN, 10));
			restoreViewButton.setForeground(Color.blue);
			restoreViewButton.setText("Restore");
			restoreViewButton.addActionListener(new ActionListener() {
	
				public void actionPerformed(ActionEvent event) {
					restoreViewButtonActionActionPerformed(event);
				}
			});
		}
		return restoreViewButton;
	}
	private JButton getSaveViewButton() {
		if (saveViewButton == null) {
			saveViewButton = new JButton();
			saveViewButton.setFont(new Font("Dialog", Font.PLAIN, 10));
			saveViewButton.setForeground(Color.blue);
			saveViewButton.setText("Save");
			saveViewButton.addActionListener(new ActionListener() {
	
				public void actionPerformed(ActionEvent event) {
					saveViewButtonActionActionPerformed(event);
				}
			});
		}
		return saveViewButton;
	}
	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			jPanel1 = new JPanel();
			jPanel1.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED, null, null));
			jPanel1.setLayout(new GroupLayout());
			jPanel1.add(getPropertiesCheckBox(), new Constraints(new Leading(8, 8, 8), new Leading(6, 10, 10)));
			jPanel1.add(getExpandCheckBox(), new Constraints(new Leading(8, 8, 8), new Leading(27, 18, 8, 8)));
			jPanel1.add(getRenderLabel(), new Constraints(new Leading(8, 8, 8), new Leading(45, 8, 8)));
			jPanel1.add(getQualifiedNames(), new Constraints(new Leading(86, 10, 10), new Leading(45, 8, 8)));
		}
		return jPanel1;
	}
	private JButton getLoadOntologyButton() {
		if (loadOntologyButton == null) {
			loadOntologyButton = new JButton();
			loadOntologyButton.setFont(new Font("Dialog", Font.PLAIN, 10));
			loadOntologyButton.setForeground(Color.blue);
			loadOntologyButton.setText("Load Ont");
			loadOntologyButton.addActionListener(new ActionListener() {
	
				public void actionPerformed(ActionEvent event) {
					OntologyButtonActionActionPerformed(event);
				}
			});
		}
		return loadOntologyButton;
	}
	public JComboBox getReasonerCombo() {
		if (loadReasonerCombo == null) {
			loadReasonerCombo = new JComboBox();
			loadReasonerCombo.setFont(new Font("Dialog", Font.PLAIN, 10));
			loadReasonerCombo.setModel(new DefaultComboBoxModel(new Object[] { "Pellet", "JFact", "Elk", "Jcel" }));
			loadReasonerCombo.setDoubleBuffered(false);
			loadReasonerCombo.setBorder(null);
			loadReasonerCombo.setEnabled(false);
		}
		return loadReasonerCombo;
	}
	public JComboBox getOntologyCombo() {
		if (loadOntologyCombo == null) {
			loadOntologyCombo = new JComboBox();
			loadOntologyCombo.setEditable(true);
			loadOntologyCombo.setFont(new Font("Dialog", Font.PLAIN, 10));
			loadRecent();

			loadOntologyCombo.setDoubleBuffered(false);
			loadOntologyCombo.setBorder(null);
			loadOntologyCombo.setRequestFocusEnabled(false);
		}
		return loadOntologyCombo;
	}
	
	private void loadRecent(){
		try {
			ClassLoader c = Thread.currentThread().getContextClassLoader();
			BufferedReader in;
			if (new File("recent.txt").exists()){
				in = new BufferedReader(new FileReader(new File("recent.txt")));
			}
			else {
				String path = c.getResource("recent.txt").getPath();
				in = new BufferedReader(new InputStreamReader(c.getResourceAsStream("recent.txt")));
			}
			String line = "";
			int no = 0;
			while ((line!=null)&&(no<15)){
				line = in.readLine();	
				if ((line!=null)&&(!line.equals(""))){
					loadOntologyCombo.addItem((Object)line);
					no++;
				}
			}
			in.close();
				
		}
		catch(IOException e ){
			e.printStackTrace();
		}	
		
	}
	
	
	private void OntologyButtonActionActionPerformed(ActionEvent event) {
		String x = (String)getOntologyCombo().getSelectedItem();
		if ((x!= null) && (!x.equals(""))){
			parent.loadActiveOntology(IRI.create(x));
			loadReasonerButton.setEnabled(true);
			getReasonerCombo().setEnabled(true);
		}	
	}

	private void saveImageButtonActionActionPerformed(ActionEvent event) {
		parent.createImage(parent.artPanel);
	}
	
	private void saveImageButtonPartialActionActionPerformed(ActionEvent event) {
		parent.createImageFromVisibleRect(parent.artPanel);
	}

	private void loadReasonerButtonActionActionPerformed(ActionEvent event) {
		String x = (String)getReasonerCombo().getSelectedItem();
		if ((x!= null) && (!x.equals(""))) {
			try{
				parent.loadReasoner(x);
				createButtonActionActionPerformed(event);
				ArrayList<String> recent = new ArrayList<String>();
				String selected = (String)getOntologyCombo().getSelectedItem();
			  	recent.add((String)selected);
				
				for (int i=0;i<getOntologyCombo().getItemCount();i++){
					recent.add((String)getOntologyCombo().getItemAt(i));
				}
				FileOutputStream fout = new FileOutputStream ("recent.txt");
				PrintStream pstream = new PrintStream(fout);
				pstream.println(selected);
				for (String str : recent){
					if (!str.equals(selected)){
						pstream.println(str);
					}
				}
				pstream.flush();
				pstream.close();
			}
			catch(Exception e ){
				e.printStackTrace();
			}
		
		}	
	}
	private void expandCheckBoxActionActionPerformed(ActionEvent event) {
		if (!getExpandCheckBox().isSelected()) {
			parent.check=true;
			parent.createButtonAction();
		}
		else {
			parent.check = false;
			parent.createButtonAction();
		}
	}

	private void createButtonActionActionPerformed(ActionEvent event) {
		parent.createButtonAction();
	}
	private void restoreViewButtonActionActionPerformed(ActionEvent event) {
		parent.restoreViewButtonAction(event);
	}
	
	private void jComboBox0ItemItemStateChanged(ItemEvent event) {
		if (event.getID()==ItemEvent.ITEM_STATE_CHANGED) {
			if (parent.firstItemStateChanged) {
				String selItem = (String)getJComboBox0().getSelectedItem();
				String key = parent.artPanel.getVisGraph().getQualifiedLabelMap().get(selItem);
			    parent.artPanel.focusOnShape(key,null); }
			else {
				parent.firstItemStateChanged = true; }
		}
	}
	private void saveViewButtonActionActionPerformed(ActionEvent event) {
		parent.saveViewButtonAction(event);
	}
	
	
	 public void loadSearchCombo(){
		    /*
		     * Fills up the search combo with all the non-anonymous entities
		     */
		    ArrayList<String> temp;
		    Set<Entry<String,Shape>> classesInGraph = parent.artPanel.getVisGraph().getClassesInGraph();

		    if (getJComboBox0().getModel().getSize()!=0){
		    	getJComboBox0().removeAllItems();
		    }
		    temp = new ArrayList<String>();
		    
		    
//		    for (Entry<String,Shape> entry : classesInGraph) {
//				 Shape s = entry.getValue();
//				 //load non-anonymous classes
//				 if ((s instanceof VisClass) && (!s.asVisClass().isAnonymous())){
//					 if (!temp.contains(entry.getKey())) {
//					 	temp.add(entry.getKey());
//					 }
//				}
//			 }
		    
		    for ( Entry<String,String> s : parent.artPanel.getVisGraph().getQualifiedLabelMap().entrySet()){
		    	temp.add(s.getKey());
		    }
		    
		    
			Collections.sort(temp);
			for (Object item : temp) {
				getJComboBox0().addItem(item);
			}
     }
	 

	private void PropertiesActionActionPerformed(ActionEvent event) {
	    Set<Entry<String,Shape>> classesInGraph = parent.artPanel.getVisGraph().getClassesInGraph();	
		if (!getPropertiesCheckBox().isSelected()) {
		    for (Entry<String,Shape> entry : classesInGraph) {
		    	if ((entry.getValue() instanceof VisClass) && (entry.getValue().asVisClass().getPropertyBox()!=null)){
		    		entry.getValue().asVisClass().getPropertyBox().setVisible(false);
		    	}
		    }
		}
		else {
			for (Entry<String,Shape> entry : classesInGraph) {
		    	if ((entry.getValue() instanceof VisClass) && (entry.getValue().asVisClass().getPropertyBox()!=null)){
		    		entry.getValue().asVisClass().getPropertyBox().setVisible(true);
		    	}
		    }
		}
	}
	
	private void reduceActionActionPerformed(ActionEvent event) {	
		int answer;
		if (getReduceCheckBox().isSelected()) {
			if (parent.artPanel.getOntology() != null){
		           answer =  JOptionPane.showConfirmDialog(parent,"Warning! this could modify source ontology","proceed", JOptionPane.OK_CANCEL_OPTION);
		           if (answer == JOptionPane.OK_OPTION){
//		        	   parent.artPanel.applyStructuralReduction();
		           }	  
		           else{
		        	   getReduceCheckBox().setSelected(false);
		           }
			}
			else {
				JOptionPane.showMessageDialog(parent, "Load ontology first");
			}
		}

	}
	
	private void fileSystemButtonActionActionPerformed(ActionEvent event) {
		JFileChooser selector = new JFileChooser();
		selector.addChoosableFileFilter(new owlFileFilter("owl"));
		selector.showOpenDialog(this);
		String x = null;
		try{
			x = selector.getSelectedFile().toURI().toURL().toString();
		}catch(Exception e){}
		if ((x!= null) && (!x.equals(""))){
			getOntologyCombo().getModel().setSelectedItem(x);
			parent.loadActiveOntology(x);
			loadReasonerButton.setEnabled(true);
			getReasonerCombo().setEnabled(true);
		}	
	}
	
	public void restoreSliderValue(){
		getZoomSlider().setValue(5);
	}
	
	public class owlFileFilter extends FileFilter
	{
	  String fileType;	
	  public owlFileFilter(String pextension){fileType = pextension;}	
	  public boolean accept(File file){
	      if (file.getName().toLowerCase().endsWith("."+fileType) || (file.isDirectory()))
	        return true;
	    return false;
	  }
	  @Override
	  public String getDescription() {return fileType;}
	}


	Dimension size = null;
	private JCheckBox renderLabel;
	private JCheckBox qualifiedNames;
	private JPanel snapshotPanel;

	private void zoomSliderChangeStateChanged(ChangeEvent event) {
		if (parent.artPanel!=null) {
			double factor = (0.5+getZoomSlider().getValue()/10.0);
			if (size == null) {
				size= parent.artPanel.getSize();
				parent.artPanel.setOriginalSize(size);
			}
			parent.artPanel.getVisGraph().setZoomLevel(getZoomSlider().getValue());
			parent.artPanel.setFactor(factor);	
			parent.artPanel.scale(factor, size);
		}
	}
	
	
	
}
	
	 
