package sid.OntView2.main;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.Objects;

public class HelpUser {
    Stage helpStage;
    ClassLoader c = Thread.currentThread().getContextClassLoader();
    VBox containerElementsInfo, containerLegend;

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

        Scene scene = new Scene(root, 700, 300);
        ClassLoader c = Thread.currentThread().getContextClassLoader();
        scene.getStylesheets().add(Objects.requireNonNull(c.getResource("styles.css")).toExternalForm());
        helpStage.setScene(scene);
        helpStage.setMinHeight(150);
        helpStage.setMinWidth(350);
        helpStage.show();
    }

    private TabPane createHelpTabsPane(Stage helpStage) {
        TabPane tabPane = new TabPane();

        Tab helpTab = new Tab("Help");
        helpTab.setClosable(false);
        VBox helpContent = createHelpContent();
        ScrollPane scrollHelp = new ScrollPane(helpContent);
        scrollHelp.setFitToWidth(true);
        scrollHelp.setFitToHeight(true);
        scrollHelp.setPannable(true);
        helpTab.setContent(scrollHelp);

        Tab elementsInfoTab = new Tab("Legend");
        elementsInfoTab.setClosable(false);
        VBox elementsInfoContent = createElementsInfoContent();
        ScrollPane scroll = new ScrollPane(elementsInfoContent);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setPannable(true);
        elementsInfoTab.setContent(scroll);

        Tab tutorialTab = new Tab("Video tutorial");
        tutorialTab.setClosable(false);

        ClassLoader c = Thread.currentThread().getContextClassLoader();

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == tutorialTab) {
                helpStage.setWidth(1400);
                helpStage.setHeight(900);
                helpStage.centerOnScreen();
            } else {
                helpStage.setWidth(700);
                helpStage.setHeight(300);
                helpStage.centerOnScreen();
            }
        });

        tutorialTab.setOnSelectionChanged(event -> {
            if (tutorialTab.isSelected()) {
                if (!(tutorialTab.getContent() instanceof MediaView)) {
                    String videoUrl = Objects.requireNonNull(c.getResource("tutorial.mp4")).toExternalForm();
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


    private VBox createHelpContent() {
        if (containerLegend == null) {
            containerLegend = new VBox(12);
            containerLegend.setPadding(new Insets(15));

            Text title = new Text("How to use the application:");
            title.setFill(Color.BLUE);
            title.setFont(Font.font("DejaVu Sans", FontWeight.BOLD, FontPosture.ITALIC, 14));
            containerLegend.getChildren().add(title);

            List<Item> items = getLegendItems();

            for (Item item : items) {
                VBox row = new VBox(8);
                row.setAlignment(Pos.CENTER_LEFT);

                Text txt = new Text(item.text);
                row.getChildren().add(txt);
                if (item.imagePath != null) {
                    row.getChildren().add(createIcon(item.imagePath, item.size));
                }

                containerLegend.getChildren().add(createContainerNoSize(row));
            }

        }
        return containerLegend;
    }

    private VBox createElementsInfoContent() {
        if (containerElementsInfo == null) {
            containerElementsInfo = new VBox(12);
            containerElementsInfo.setPadding(new Insets(15));

            Text title = new Text("Legend:");
            title.setFill(Color.BLUE);
            title.setFont(Font.font("DejaVu Sans", FontWeight.BOLD, FontPosture.ITALIC, 14));
            containerElementsInfo.getChildren().add(title);

            List<Item> items = getGraphElementsItems();

            for (Item item : items) {
                VBox row = new VBox(8);
                row.setAlignment(Pos.CENTER_LEFT);

                Text txt = new Text(item.text);
                row.getChildren().add(txt);
                if (item.imagePath != null) {
                    row.getChildren().add(createIcon(item.imagePath, item.size));
                }

                containerElementsInfo.getChildren().add(createContainerNoSize(row));
            }
        }

        return containerElementsInfo;
    }

    private List<Item> getLegendItems() {
        return List.of(
            new Item("1. Load an ontology and click the 'Load Ont' button.",null, 0),
            new Item("2. Select a reasoner, then choose the 'KConceptExtractor', and finally click the 'Sync' button.",null, 0),
            new Item("3. You are now ready to use the rest of the functionalities.",null, 0)
        );
    }

    private List<Item> getGraphElementsItems() {
        return List.of(
            new Item("· Named Classes : Gray","assets/named.png", 170),
            new Item("· Defined Classes : Light Green", "assets/definedEquivalent.png", 170),
            new Item("· Anonymous Classes : White", "assets/anonymous.png", 170),
            new Item("· P : Indicates that this specific node has properties associated with it.", "assets/properties.png", 170),
            new Item("· D : Indicates that the class is disjoint with other classes.", "assets/dDisjoint.png", 130),
            new Item("· (1) : Functional property",null, 0)
        );
    }

    private ImageView createIcon(String resourcePath, double width) {
        Image img = new Image(Objects.requireNonNull(c.getResourceAsStream(resourcePath)));
        ImageView iv = new ImageView(img);
        iv.setFitWidth(width);
        iv.setPreserveRatio(true);
        return iv;
    }

    private VBox createContainerNoSize(Node... children) {
        VBox container = new VBox(7);
        container.getChildren().addAll(children);
        container.setPadding(new Insets(10));
        container.getStyleClass().add("tab-info-container");
        container.setAlignment(Pos.CENTER_LEFT);
        return container;
    }

    private record Item(String text, String imagePath, double size) { }
}
