//package sid.OntView.Protege;
//
//import java.awt.Color;
//import java.awt.Dimension;
//import java.awt.FlowLayout;
//import java.awt.Font;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.awt.event.ItemEvent;
//import java.awt.event.ItemListener;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Map.Entry;
//import java.util.Set;
//
//import javax.swing.BorderFactory;
//import javax.swing.DefaultComboBoxModel;
//import javax.swing.ImageIcon;
//import javax.swing.JButton;
//import javax.swing.JCheckBox;
//import javax.swing.JComboBox;
//import javax.swing.JLabel;
//import javax.swing.JPanel;
//import javax.swing.JSlider;
//import javax.swing.border.EtchedBorder;
//import javax.swing.event.ChangeEvent;
//import javax.swing.event.ChangeListener;
//
//import sid.OntView.common.AutoCompletion;
//import sid.OntView.common.ControlPanelInterface;
//import sid.OntView.common.Shape;
//import sid.OntView.common.VisClass;
//
//
//
////VS4E -- DO NOT REMOVE THIS LINE!
//public class TopPanel extends JPanel implements ControlPanelInterface {
//
//	private static final long serialVersionUID = 1L;
//	private JButton createButton;
//	private JCheckBox expandCheckBox,renderLabel;
//	private JPanel jPanel1;
//	private JButton saveViewButton;
//	private JButton restoreViewButton;
//	private JPanel ViewPanel;
//	private JButton saveImageButton;
//	private JLabel jLabel0;
//	private JComboBox jComboBox0;
//	private JPanel jPanel0;
//	private OWLClassMainViewComponent parent;
//
//	private JLabel jLabel1;
//	private JSlider zoomSlider;
//	public TopPanel(OWLClassMainViewComponent pparent){
//		parent = pparent;
//		initComponents();
//	}
//	public TopPanel() {
//		initComponents();
//		
//	}
//
//	private void initComponents() {
//		setMaximumSize(new Dimension(2147483647, 70));
//		setAutoscrolls(true);
//		setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
//		add(getJPanel1());
//		add(getViewPanel());
//		add(getJPanel0());
//		add(getSaveImageButton());
//		add(getZoomSlider());
//		setSize(1069, 41);
//	}
//	private JCheckBox getQualifiedNames() {
//		if (qualifiedNames == null) {
//			qualifiedNames = new JCheckBox();
//			qualifiedNames.setFont(new Font("Dialog", Font.PLAIN, 10));
//			qualifiedNames.setSelected(false);
//			qualifiedNames.setText("qNames");
//			
//			qualifiedNames.addActionListener(new ActionListener() {
//				
//				public void actionPerformed(ActionEvent event) {
//					qualifiedNamesActionActionPerformed(event);
//				}
//			});
//		}
//		return qualifiedNames;
//	}
//	private JCheckBox getJCheckBox1() {
//		if (qualifiedNames == null) {
//			qualifiedNames = new JCheckBox();
//			qualifiedNames.setText("jCheckBox1");
//		}
//		return qualifiedNames;
//	}
//	private JCheckBox getPropertiesCheckBox() {
//		if (propertiesCheckBox == null) {
//			propertiesCheckBox = new JCheckBox();
//			propertiesCheckBox.setFont(new Font("Dialog", Font.PLAIN, 10));
//			propertiesCheckBox.setText("properties");
//			
//			propertiesCheckBox.addActionListener(new ActionListener() {
//				
//				public void actionPerformed(ActionEvent event) {
//					propertiesActionActionPerformed(event);
//				}
//			});
//		}
//		return propertiesCheckBox;
//	}
//	public void restoreSliderValue(){
//		getZoomSlider().setValue(5);
//	}
//	private JSlider getZoomSlider() {
//		if (zoomSlider == null) {
//			zoomSlider = new JSlider();
//			zoomSlider.setMaximum(25);
//			zoomSlider.setMinorTickSpacing(3);
//			zoomSlider.setPaintTicks(true);
//			zoomSlider.setToolTipText("zoom");
//			zoomSlider.setValue(5);
//			zoomSlider.addChangeListener(new ChangeListener() {
//	
//				public void stateChanged(ChangeEvent event) {
//					zoomSliderChangeStateChanged(event);
//				}
//			});
//		}
//		return zoomSlider;
//	}
//	
//
//	
//	
//	Dimension size= null;
//	double factor = 1.0;
//	private JCheckBox propertiesCheckBox;
//	private JCheckBox qualifiedNames;
//	private void zoomSliderChangeStateChanged(ChangeEvent event) {
//		double factor = (0.5+getZoomSlider().getValue()/10.0);
//		if (size == null) {
//			size= parent.artPanel.getSize();
//			parent.artPanel.setOriginalSize(size);
//		}
//		parent.artPanel.getVisGraph().setZoomLevel(getZoomSlider().getValue());
//		parent.artPanel.setFactor(factor);	
//		parent.artPanel.scale(factor, size);
//	}
//	
//	private JLabel getJLabel1() {
//		if (jLabel1 == null) {
//			jLabel1 = new JLabel();
//			jLabel1.setFont(new Font("Dialog", Font.PLAIN, 10));
//			jLabel1.setText("View");
//		}
//		return jLabel1;
//	}
//	private JPanel getJPanel0() {
//		if (jPanel0 == null) {
//			jPanel0 = new JPanel();
//			jPanel0.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED, null, null));
//			jPanel0.setMinimumSize(new Dimension(30, 25));
//			jPanel0.setMaximumSize(new Dimension(32767, 25));
//			jPanel0.add(getJLabel0());
//			jPanel0.add(getJComboBox0());
//		}
//		return jPanel0;
//	}
//	private JComboBox getJComboBox0() {
//		if (jComboBox0 == null) {
//			jComboBox0 = new JComboBox();
//			jComboBox0.setEditable(true);
//			jComboBox0.setFont(new Font("Dialog", Font.BOLD, 10));
//			jComboBox0.setForeground(Color.blue);
//			jComboBox0.setModel(new DefaultComboBoxModel(new Object[] {}));
//			jComboBox0.setDoubleBuffered(false);
//			jComboBox0.setBorder(null);
//			jComboBox0.setRequestFocusEnabled(false);
//			jComboBox0.addItemListener(new ItemListener() {
//	
//				public void itemStateChanged(ItemEvent event) {
//					jComboBox0ItemItemStateChanged(event);
//				}
//			});
//		}
//		return jComboBox0;
//	}
//	private JLabel getJLabel0(){
//	if(jLabel0==null){
//		jLabel0 = new JLabel();
//		ClassLoader c;
//	     if (getClass().getClassLoader() == null)
//	    	  c = ClassLoader.getSystemClassLoader();
//	      else 
//	    	  c = getClass().getClassLoader();
//		if (c!=null)
//			jLabel0.setIcon(new ImageIcon(c.getResource("search.JPG")));
//		
//	}
//	return jLabel0;
//	}
//
//	private JButton getCreateButton() {
//		if (createButton == null) {
//			createButton = new JButton();
//			createButton.setFont(new Font("Dialog", Font.PLAIN, 10));
//			createButton.setText("create");
//			createButton.addActionListener(new ActionListener() {
//	
//				public void actionPerformed(ActionEvent event) {
//					createButtonActionActionPerformed(event);
//				}
//			});
//		}
//		return createButton;
//	}
//	private JCheckBox getExpandCheckBox() {
//		if (expandCheckBox == null) {
//			expandCheckBox = new JCheckBox();
//			expandCheckBox.setFont(new Font("Dialog", Font.PLAIN, 10));
//			expandCheckBox.setText("expand");
//			expandCheckBox.addActionListener(new ActionListener() {
//	
//				public void actionPerformed(ActionEvent event) {
//					expandCheckBoxActionActionPerformed(event);
//				}
//			});
//		}
//		return expandCheckBox;
//	}
//	private JCheckBox getRenderLabel() {
//		if (renderLabel == null) {
//			renderLabel = new JCheckBox();
//			renderLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
//			renderLabel.setSelected(false);
//			renderLabel.setText("label");
//			renderLabel.addActionListener(new ActionListener() {
//	
//				public void actionPerformed(ActionEvent event) {
//					renderLabelActionPerformed(event);
//				}
//			});
//		}
//		return renderLabel;
//	}
//	public void renderLabelActionPerformed(ActionEvent event) {
//		if (parent.artPanel!= null){
//			parent.artPanel.renderLabel = !parent.artPanel.renderLabel;
//			if (parent.artPanel.getVisGraph()!= null)
//				parent.artPanel.getVisGraph().changeRenderMethod(parent.artPanel.renderLabel, parent.artPanel.qualifiedNames);
//		}
//	}
//	
//	public void qualifiedNamesActionActionPerformed(ActionEvent event) {
//		if (parent.artPanel!= null){
//			parent.artPanel.qualifiedNames = !parent.artPanel.qualifiedNames;
//			if (parent.artPanel.getVisGraph()!= null)
//				parent.artPanel.getVisGraph().changeRenderMethod(parent.artPanel.renderLabel, parent.artPanel.qualifiedNames);
//		}
//	}
//	
//	private JButton getSaveImageButton() {
//		if (saveImageButton == null) {
//			saveImageButton = new JButton();
//			saveImageButton.setPreferredSize(new Dimension(25, 25));
//			ClassLoader c;
//		     if (getClass().getClassLoader() == null)
//		    	  c = ClassLoader.getSystemClassLoader();
//		     else 
//		    	  c = getClass().getClassLoader();
//			 if (c!=null)
//				 try{
//			saveImageButton.setIcon(new ImageIcon(c.getResource("saveImage.JPG")));
//				 }
//			 catch (Exception e ){
//				 e.printStackTrace();
//			 }
//			saveImageButton.addActionListener(new ActionListener() {
//	
//				public void actionPerformed(ActionEvent event) {
//					saveImageButtonActionActionPerformed(event);
//				}
//			});
//		}
//		return saveImageButton;
//	}
//	private JPanel getViewPanel() {
//		if (ViewPanel == null) {
//			ViewPanel = new JPanel();
//			ViewPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED, null, null));
//			ViewPanel.setFont(new Font("Dialog", Font.BOLD | java.awt.Font.ITALIC, 10));
//			ViewPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
//			ViewPanel.add(getJLabel1());
//			ViewPanel.add(getSaveViewButton());
//			ViewPanel.add(getRestoreViewButton());
//		}
//		return ViewPanel;
//	}
//	private JButton getRestoreViewButton() {
//		if (restoreViewButton == null) {
//			restoreViewButton = new JButton();
//			restoreViewButton.setFont(new Font("Dialog", Font.PLAIN, 10));
//			restoreViewButton.setForeground(Color.blue);
//			restoreViewButton.setText("Restore");
//			restoreViewButton.addActionListener(new ActionListener() {
//	
//				public void actionPerformed(ActionEvent event) {
//					restoreViewButtonActionActionPerformed(event);
//				}
//			});
//		}
//		return restoreViewButton;
//	}
//	private JButton getSaveViewButton() {
//		if (saveViewButton == null) {
//			saveViewButton = new JButton();
//			saveViewButton.setFont(new Font("Dialog", Font.PLAIN, 10));
//			saveViewButton.setForeground(Color.blue);
//			saveViewButton.setText("Save");
//			saveViewButton.addActionListener(new ActionListener() {
//	
//				public void actionPerformed(ActionEvent event) {
//					saveViewButtonActionActionPerformed(event);
//				}
//			});
//		}
//		return saveViewButton;
//	}
//	private JPanel getJPanel1() {
//		if (jPanel1 == null) {
//			jPanel1 = new JPanel();
//			jPanel1.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED, null, null));
//			jPanel1.add(getCreateButton());
//			jPanel1.add(getPropertiesCheckBox());
//			jPanel1.add(getExpandCheckBox());
//			jPanel1.add(getQualifiedNames());
//			jPanel1.add(getRenderLabel());
//		}
//		return jPanel1;
//	}
//	private void saveImageButtonActionActionPerformed(ActionEvent event) {
//		parent.createImage(parent.artPanel);
//	}
//
//	private void expandCheckBoxActionActionPerformed(ActionEvent event) {
//		parent.check = !parent.check;
//		parent.createButtonAction();
//	}
//
//	private void createButtonActionActionPerformed(ActionEvent event) {
//		parent.createButtonAction();
//	}
//	private void restoreViewButtonActionActionPerformed(ActionEvent event) {
//		parent.restoreViewButtonAction(event);
//	}
//	
//	private void jComboBox0ItemItemStateChanged(ItemEvent event) {
//		if (event.getID()==ItemEvent.ITEM_STATE_CHANGED) {
//			if (parent.firstItemStateChanged) {
//			    parent.artPanel.focusOnShape((String)getJComboBox0().getSelectedItem(),null); }
//			else {
//				parent.firstItemStateChanged = true; }
//		}
//	}
//	private void saveViewButtonActionActionPerformed(ActionEvent event) {
//		parent.saveViewButtonAction(event);
//	}
//	
//	
//	 public void loadSearchCombo(){
//		    /*
//		     * Fills up the search combo with all the non-anonymous entities
//		     */
//			    ArrayList<String> temp;	
//			    
//			    Set<Entry<String,Shape>> classesInGraph = parent.artPanel.getVisGraph().getClassesInGraph();	
//			    temp = new ArrayList<String>();
//			    for (Entry<String,Shape> entry : classesInGraph) {
//						 Shape s = entry.getValue();
//						 //load non-anonymous classes
//						 if ((s instanceof VisClass) && (!s.asVisClass().isAnonymous())){
//							 if (!temp.contains(entry.getKey())) {
//							 	temp.add(entry.getKey());
//							 }
//						}
//				 }		 
//				Collections.sort(temp);
//				getJComboBox0().removeAllItems();
//				for (Object item : temp) {
//					getJComboBox0().addItem(item);
//				}
//				AutoCompletion.enable(getJComboBox0());
//     }
//	 
//	 private void propertiesActionActionPerformed(ActionEvent event) {
//		 
//		 if (parent.artPanel.getVisGraph() != null) {
//		    Set<Entry<String,Shape>> classesInGraph = parent.artPanel.getVisGraph().getClassesInGraph();	
//			if (!getPropertiesCheckBox().isSelected()) {
//			    for (Entry<String,Shape> entry : classesInGraph) {
//			    	if ((entry.getValue() instanceof VisClass) && (entry.getValue().asVisClass().getPropertyBox()!=null)){
//			    		entry.getValue().asVisClass().getPropertyBox().setVisible(false);
//			    	}
//			    }
//			}
//			else {
//				for (Entry<String,Shape> entry : classesInGraph) {
//			    	if ((entry.getValue() instanceof VisClass) && (entry.getValue().asVisClass().getPropertyBox()!=null)){
//			    		entry.getValue().asVisClass().getPropertyBox().setVisible(true);
//			    	}
//			    }
//			}
//		}
//	 }
//	 
//	 
//}
//	
//	 