package sid.OntView2.main;

import javafx.application.Application;
import javafx.stage.Stage;

public class JavaFXApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        // Your JavaFX setup, if you need anything specifically setup on the JavaFX stage
        // Otherwise, this can be minimal if you are just initializing JavaFX toolkit

        // Display or configure your primary stage if necessary
        primaryStage.setTitle("JavaFX and Swing Integration");
        primaryStage.close();
    }

    public static void main(String[] args) {
        launch(args); // Launch the JavaFX application
    }
}
