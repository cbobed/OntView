package sid.OntView.common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Window;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

//VS4E -- DO NOT REMOVE THIS LINE!
public class VisInstance extends JDialog {

	private static final long serialVersionUID = 1L;
	private JList jList0;
	private JScrollPane jScrollPane0;
	private static final String PREFERRED_LOOK_AND_FEEL = "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel";
	public VisInstance() {
		initComponents();
	}

	public VisInstance(Frame parent) {
		super(parent);
		initComponents();
	}

	public VisInstance(Frame parent, boolean modal) {
		super(parent, modal);
		initComponents();
	}

	public VisInstance(Frame parent, String title) {
		super(parent, title);
		initComponents();
	}

	public VisInstance(Frame parent, String title, boolean modal) {
		super(parent, title, modal);
		initComponents();
	}

	public VisInstance(Frame parent, String title, boolean modal,
			GraphicsConfiguration arg) {
		super(parent, title, modal, arg);
		initComponents();
	}

	public VisInstance(Dialog parent) {
		super(parent);
		initComponents();
	}

	public VisInstance(Dialog parent, boolean modal) {
		super(parent, modal);
		initComponents();
	}

	public VisInstance(Dialog parent, String title) {
		super(parent, title);
		initComponents();
	}

	public VisInstance(Dialog parent, String title, boolean modal) {
		super(parent, title, modal);
		initComponents();
	}

	public VisInstance(Dialog parent, String title, boolean modal,
			GraphicsConfiguration arg) {
		super(parent, title, modal, arg);
		initComponents();
	}

	public VisInstance(Window parent) {
		super(parent);
		initComponents();
	}

	public VisInstance(Window parent, ModalityType modalityType) {
		super(parent, modalityType);
		initComponents();
	}

	public VisInstance(Window parent, String title) {
		super(parent, title);
		initComponents();
	}

	public VisInstance(Window parent, String title, ModalityType modalityType) {
		super(parent, title, modalityType);
		initComponents();
	}

	public VisInstance(Window parent, String title, ModalityType modalityType,
			GraphicsConfiguration arg) {
		super(parent, title, modalityType, arg);
		initComponents();
	}

	private void initComponents() {
		setFont(new Font("Dialog", Font.PLAIN, 12));
		setBackground(new Color(223, 223, 223));
		setForeground(Color.black);
		add(getJScrollPane0(), BorderLayout.CENTER);
		setSize(221, 451);
	}

	private JScrollPane getJScrollPane0() {
		if (jScrollPane0 == null) {
			jScrollPane0 = new JScrollPane();
			jScrollPane0.setViewportView(getJList0());
		}
		return jScrollPane0;
	}

	public void setModel(ArrayList<String> model){
		DefaultListModel<String> listModel = new DefaultListModel<>();
		for (String s : model){
			listModel.addElement(s);
		}
		jList0.setModel(listModel);
	}
	
	private JList getJList0() {
		if (jList0 == null) {
			jList0 = new JList<>();
			DefaultListModel<String> listModel = new DefaultListModel<>();
			jList0.setModel(listModel);
			jList0.setSelectionBackground(new Color(100, 154, 191));
		}
		return jList0;
	}

	private static void installLnF() {
		try {
			String lnfClassname = PREFERRED_LOOK_AND_FEEL;
			if (lnfClassname == null)
				lnfClassname = UIManager.getCrossPlatformLookAndFeelClassName();
			UIManager.setLookAndFeel(lnfClassname);
		} catch (Exception e) {
			System.err.println("Cannot install " + PREFERRED_LOOK_AND_FEEL
					+ " on this platform:" + e.getMessage());
		}
	}

	/**
	 * Main entry of the class.
	 * Note: This class is only created so that you can easily preview the result at runtime.
	 * It is not expected to be managed by the designer.
	 * You can modify it as you like.
	 */
	public static void main(String[] args) {
		installLnF();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				VisInstance dialog = new VisInstance();
				dialog.setDefaultCloseOperation(VisInstance.DISPOSE_ON_CLOSE);
				dialog.setTitle("VisInstance");
				dialog.setLocationRelativeTo(null);
				dialog.getContentPane().setPreferredSize(dialog.getSize());
				dialog.pack();
				dialog.setVisible(true);
			}
		});
	}

}
