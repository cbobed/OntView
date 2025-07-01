package sid.OntView2.main;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Objects;

public class HelpUser {
    Stage helpStage;

    public HelpUser() { }

    public void showHelpStage() {
        if (helpStage != null && helpStage.isShowing()) {
            helpStage.requestFocus();
            return;
        }

        helpStage = new Stage();
        helpStage.setTitle("Help");

        TabPane tabPane = createHelpTabsPane(helpStage);
        BorderPane root = new BorderPane(tabPane);

        Scene scene = new Scene(root, 600, 300);
        ClassLoader c = Thread.currentThread().getContextClassLoader();
        scene.getStylesheets().add(Objects.requireNonNull(c.getResource("styles.css")).toExternalForm());
        helpStage.setScene(scene);
        helpStage.show();
    }

    private TabPane createHelpTabsPane(Stage helpStage) {
        TabPane tabPane = new TabPane();

        Tab helpTab = new Tab("Help");
        helpTab.setClosable(false);
        TextFlow helpContent = createHelpContent();
        helpTab.setContent(helpContent);

        Tab elementsInfoTab = new Tab("Legend");
        elementsInfoTab.setClosable(false);
        TextFlow elementsInfoContent = createElementsInfoContent();
        elementsInfoTab.setContent(elementsInfoContent);

        Tab tutorialTab = new Tab("Video tutorial");
        tutorialTab.setClosable(false);

        ClassLoader c = Thread.currentThread().getContextClassLoader();

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == tutorialTab) {
                helpStage.setWidth(1400);
                helpStage.setHeight(900);
                helpStage.centerOnScreen();
            } else {
                helpStage.setWidth(600);
                helpStage.setHeight(300);
                helpStage.centerOnScreen();
            }
        });

        tutorialTab.setOnSelectionChanged(event -> {
            if (tutorialTab.isSelected()) {
                if (!(tutorialTab.getContent() instanceof MediaView)) {
                    String videoUrl = Objects.requireNonNull(c.getResource("final.mp4")).toExternalForm();
                    BorderPane videoPane = getVideoPane(videoUrl);
                    tutorialTab.setContent(videoPane);
                }
            }
        });

        tabPane.getTabs().addAll(helpTab, elementsInfoTab, tutorialTab);

        return tabPane;
    }

    private static BorderPane getVideoPane(String videoUrl) {
        Media media = new Media(videoUrl);
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setAutoPlay(false);
        MediaView mediaView = new MediaView(mediaPlayer);
        mediaView.setPreserveRatio(true);

        Pane mediaPane = new Pane(mediaView);
        mediaView.fitWidthProperty().bind(mediaPane.widthProperty());
        mediaView.fitHeightProperty().bind(mediaPane.heightProperty());
        mediaView.setSmooth(true);

        Polygon playIcon = new Polygon(0, 0, 0, 60, 52, 30);
        playIcon.setFill(Color.rgb(70, 130, 180, 0.85));
        playIcon.setVisible(true);

        StackPane videoStack = new StackPane(mediaPane, playIcon);
        videoStack.setStyle("-fx-background-color: black;");
        videoStack.setCursor(Cursor.HAND);

        videoStack.setOnMouseClicked(evt -> {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.play();
            }
        });

        mediaPlayer.setOnPlaying(() -> playIcon.setVisible(false));
        mediaPlayer.setOnPaused(() -> playIcon.setVisible(true));
        mediaPlayer.setOnEndOfMedia(() -> {
            playIcon.setVisible(true);
            mediaPlayer.seek(Duration.ZERO);
            mediaPlayer.pause();
        });

        Slider progress = createProgressSlider(mediaPlayer);

        HBox controls = new HBox(10, progress);
        controls.setPadding(new Insets(5));
        controls.setAlignment(Pos.CENTER);
        controls.setStyle("-fx-background-color: rgba(0,0,0,0.6);");

        BorderPane videoPane = new BorderPane();
        videoPane.setCenter(videoStack);
        videoPane.setBottom(controls);

        return videoPane;
    }

    private static Slider createProgressSlider(MediaPlayer mediaPlayer) {
        Slider progress = new Slider(0, 100, 0);
        progress.setPrefWidth(400);
        progress.getStyleClass().add("video-slider");

        mediaPlayer.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            Duration total = mediaPlayer.getTotalDuration();
            if (total != null && !progress.isValueChanging()) {
                progress.setValue(newTime.toMillis() / total.toMillis() * 100.0);
            }
        });

        progress.valueChangingProperty().addListener((obs, wasChanging, isChanging) -> {
            if (!isChanging && mediaPlayer.getTotalDuration() != null) {
                double pct = progress.getValue() / 100;
                mediaPlayer.seek(mediaPlayer.getTotalDuration().multiply(pct));
            }
        });

        progress.setOnMouseClicked(evt -> {
            if (mediaPlayer.getTotalDuration() != null) {
                double pct = evt.getX() / progress.getWidth();
                mediaPlayer.seek(mediaPlayer.getTotalDuration().multiply(pct));
            }
        });
        return progress;
    }


    private TextFlow createHelpContent() {
        Text title = new Text("How to use the application:\n");
        title.setFill(Color.BLUE);
        title.setStyle("-fx-font-weight: bold;");

        Text step1 = new Text("1. Load an ontology and click the 'Load Ont' button.\n");
        Text step2 = new Text("2. Select a reasoner, then choose the 'KConceptExtractor', and finally click the 'Sync' button.\n");
        Text step3 = new Text("3. You are now ready to use the rest of the functionalities.");

        TextFlow helpContent = new TextFlow(title, step1, step2, step3);
        helpContent.setPadding(new Insets(10));

        helpContent.setLineSpacing(5);
        return helpContent;
    }
    private TextFlow createElementsInfoContent() {
        Text elementsInfoTitle = new Text("Legend:\n");
        elementsInfoTitle.setFill(Color.BLUE);
        elementsInfoTitle.setStyle("-fx-font-weight: bold;");

        Text elementsInfoText = new Text(
            """
            P: Indicates that this specific node has properties associated with it.
            D: Indicates that the class is disjoint with other classes.
            Named Classes: Gray
            Defined Classes: Light Green
            Anonymous Classes: White
            (1): Functional property
            """
        );

        TextFlow elementsInfoContent = new TextFlow(elementsInfoTitle, elementsInfoText);
        elementsInfoContent.setPadding(new Insets(10));
        elementsInfoContent.setLineSpacing(5);

        return elementsInfoContent;
    }
}
