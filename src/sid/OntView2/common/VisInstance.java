package sid.OntView2.common;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Cursor;
import javafx.scene.control.*;

import java.io.Serializable;
import java.util.ArrayList;

//VS4E -- DO NOT REMOVE THIS LINE!
public class VisInstance extends Dialog<Void> implements Serializable {

	private ListView<String> list0;
	//private static final String PREFERRED_LOOK_AND_FEEL = "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel";
	public VisInstance() {
		initComponents();
	}

	private void initComponents() {
		setResizable(false);
		//getDialogPane().setStyle("-fx-background-color: rgb(233, 233, 233); -fx-text-fill: black; -fx-font-size: 12px;");
		list0 = getList0();

		ScrollPane scrollPane0 = new ScrollPane(list0);
		getDialogPane().setContent(scrollPane0);
		scrollPane0.setFitToWidth(true);
		scrollPane0.setFitToHeight(true);
		getDialogPane().setPrefSize(221, 451);
	}

	public void setModel(ArrayList<String> model){
		ObservableList<String> items = FXCollections.observableArrayList(model);
		list0.setItems(items);
	}

	private ListView<String> getList0() {
		if (list0 == null) {
			list0 = new ListView<>();
			ObservableList<String> items = FXCollections.observableArrayList();
			list0.setItems(items);
			//list0.setStyle("-fx-selection-bar: rgb(100, 154, 191);");
			if ( items.isEmpty() ){
				list0.setCursor(Cursor.DEFAULT);
			} else {
				list0.setCursor(Cursor.HAND);
			}
		}
		return list0;
	}

	/**
	 * Main entry of the class.
	 * Note: This class is only created so that you can easily preview the result at runtime.
	 * It is not expected to be managed by the designer.
	 * You can modify it as you like.
	 */
	public static void main(String[] args) {
		//installLnF();
		Platform.runLater(() -> {
			VisInstance dialog = new VisInstance();
			ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
			dialog.getDialogPane().getButtonTypes().add(cancelButtonType);
			dialog.getDialogPane().lookupButton(cancelButtonType).setVisible(false);
			dialog.setTitle("VisInstance");
			dialog.showAndWait();
		});
	}

}
