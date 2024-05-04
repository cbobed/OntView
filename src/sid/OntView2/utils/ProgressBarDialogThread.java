package sid.OntView2.utils;

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.RepaintManager;

import sid.OntView2.common.PaintFrame;
import sid.OntView2.common.VisGraphObserver;

public class ProgressBarDialogThread extends VisGraphObserver{

	JDialog progressDialog;
	JProgressBar progressBar;
	PaintFrame paintframe;
	//	int mock = 0;
	final static int BARWIDTH  = 100;
	final static int BARHEIGHT = 20 ;

	public ProgressBarDialogThread(PaintFrame ppaintframe){
		super(ppaintframe.getVisGraph());
		paintframe = ppaintframe;
		progressDialog = new JDialog();

		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);

		progressDialog.add(progressBar);

		progressBar.setPreferredSize(new Dimension(BARWIDTH,BARHEIGHT));
		progressBar.setString(Integer.toString(getProgress()));
		progressBar.setVisible(true);

		progressDialog.setSize(new Dimension(BARWIDTH,BARHEIGHT));
		progressDialog.pack();
		progressDialog.setLocation((int)paintframe.getVisibleRect().getWidth()/ 2-BARWIDTH,
				(int)paintframe.getVisibleRect().getHeight()/2+BARHEIGHT);
		progressDialog.setVisible(true);
		progressDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		progressDialog.setAlwaysOnTop(true);
		progressDialog.setTitle("Progress");

	}

	private int getProgress() {
		return paintframe.getVisGraph() == null ? 0 :  paintframe.getVisGraph().getProgress();
	}

	@Override
	public void update() {
		int progress;
		progress = getProgress();
		progressBar.setValue(progress);
		progressBar.setString(Integer.toString(progress));
		if (progress == 100) {
			progressDialog.dispose();
		}

	}
}
