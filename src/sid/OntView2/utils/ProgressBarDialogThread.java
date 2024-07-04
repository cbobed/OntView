package sid.OntView2.utils;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sid.OntView2.common.PaintFrame;
import sid.OntView2.common.VisGraphObserver;

public class ProgressBarDialogThread extends VisGraphObserver{

	Stage progressDialog;
	ProgressBar progressBar;
	PaintFrame paintframe;
	//	int mock = 0;
	final static int BARWIDTH  = 100;
	final static int BARHEIGHT = 20 ;

	final static int DIALOGWIDTH  = 200;
	final static int DIALOGHEIGHT = 100;

	public ProgressBarDialogThread(PaintFrame ppaintframe){
		super(ppaintframe.getVisGraph());
		paintframe = ppaintframe;
		progressDialog = new Stage();
		progressDialog.setTitle("Progress");
		progressDialog.setResizable(false);
		progressDialog.setAlwaysOnTop(true);
		progressDialog.initModality(Modality.APPLICATION_MODAL);

		progressBar = new ProgressBar(0);
		progressBar.setPrefSize(BARWIDTH, BARHEIGHT);
		progressBar.setProgress(0);

		VBox vbox = new VBox(10, progressBar);
		vbox.setAlignment(Pos.CENTER);

		Scene scene = new Scene(vbox, DIALOGWIDTH, DIALOGHEIGHT);
		progressDialog.setScene(scene);
		progressDialog.show();
	}

	private int getProgress() {
		return paintframe.getVisGraph() == null ? 0 :  paintframe.getVisGraph().getProgress();
	}

	@Override
	public void update() {
		int progress = getProgress();
		Platform.runLater(() -> {
			progressBar.setProgress(progress / 100.0);
			if (progress == 100) {
				progressDialog.close();
			}
		});

	}
}
