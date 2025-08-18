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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class HelpUser {
    private static final Logger logger = LogManager.getLogger(HelpUser.class);

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
        scrollHelp.setPannable(true);
        helpTab.setContent(scrollHelp);

        Tab elementsInfoTab = new Tab("Legend");
        elementsInfoTab.setClosable(false);
        VBox elementsInfoContent = createElementsInfoContent();
        ScrollPane scroll = new ScrollPane(elementsInfoContent);
        scroll.setFitToWidth(true);
        scroll.setPannable(true);
        elementsInfoTab.setContent(scroll);

        Tab tutorialTab = new Tab("Video tutorial");
        tutorialTab.setClosable(false);

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == tutorialTab) {
                helpStage.setWidth(1400);
                helpStage.setHeight(900);
                helpStage.centerOnScreen();
            } else if (newTab == elementsInfoTab) {
                helpStage.setWidth(700);
                helpStage.setHeight(600);
                helpStage.centerOnScreen();

            }else {
                helpStage.setWidth(700);
                helpStage.setHeight(300);
                helpStage.centerOnScreen();
            }
        });

        tutorialTab.setOnSelectionChanged(event -> {
            if (tutorialTab.isSelected()) {
                if (!(tutorialTab.getContent() instanceof MediaView)) {
                    String videoUrl = Objects.requireNonNull(c.getResource("assets/video.mp4")).toExternalForm();
                    BorderPane videoPane = getVideoPane(videoUrl);
                    tutorialTab.setContent(videoPane);
                }
            }
        });

        tabPane.getTabs().addAll(helpTab, elementsInfoTab, tutorialTab);

        return tabPane;
    }
    private static BorderPane getVideoPane(String videoUrl) {
        System.out.println("Loading video from: " + videoUrl);

        Media media = new Media(videoUrl);
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setAutoPlay(false);

        MediaView mediaView = new MediaView(mediaPlayer);
        mediaView.setPreserveRatio(true);
        mediaView.setSmooth(true);

        StackPane mediaContainer = new StackPane(mediaView);
        mediaContainer.setStyle("-fx-background-color: black;");
        mediaView.fitWidthProperty().bind(mediaContainer.widthProperty());
        mediaView.fitHeightProperty().bind(mediaContainer.heightProperty());
        mediaContainer.setMinWidth(0);
        mediaContainer.setMinHeight(0);

        Polygon playIcon = new Polygon(0,0, 0,60, 52,30);
        playIcon.setFill(Color.rgb(70,130,180,0.85));
        playIcon.visibleProperty().bind(mediaPlayer.statusProperty().isNotEqualTo(MediaPlayer.Status.PLAYING));

        StackPane videoStack = new StackPane(mediaContainer, playIcon);
        videoStack.setCursor(Cursor.HAND);
        videoStack.setOnMouseClicked(evt -> {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.play();
            }
        });
        mediaPlayer.setOnEndOfMedia(() -> {
            mediaPlayer.seek(Duration.ZERO);
            mediaPlayer.pause();
        });

        Slider progress = createProgressSlider(mediaPlayer);
        HBox controls = new HBox(progress);
        controls.setPadding(new Insets(5));
        controls.setAlignment(Pos.CENTER);
        controls.setStyle("-fx-background-color: rgba(0,0,0,0.6);");

        BorderPane videoPane = new BorderPane();
        videoPane.setCenter(videoStack);
        videoPane.setBottom(controls);
        BorderPane.setAlignment(videoStack, Pos.CENTER);
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

            Label title = new Label("How to use the application:");
            title.setTextFill(Color.BLUE);
            title.setFont(Font.font("DejaVu Sans", FontWeight.BOLD, FontPosture.ITALIC, 14));
            containerLegend.getChildren().add(title);

            List<Item> items = getLegendItems();

            for (Item item : items) {
                VBox row = new VBox(8);
                row.setAlignment(Pos.CENTER_LEFT);

                Label txt = new Label(item.text);
                txt.setWrapText(true);
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

            Label title = new Label("Legend:");
            title.setTextFill(Color.BLUE);
            title.setFont(Font.font("DejaVu Sans", FontWeight.BOLD, FontPosture.ITALIC, 14));
            containerElementsInfo.getChildren().add(title);

            List<Item> items = getGraphElementsItems();

            for (Item item : items) {
                VBox row = new VBox(8);
                row.setAlignment(Pos.CENTER_LEFT);

                Label txt = new Label(item.text);
                txt.setWrapText(true);
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
            new Item("2. Select a reasoner, then choose the 'KConceptExtractor', and finally click the 'Sync' " +
                "button.",null, 0),
            new Item("3. You are now ready to use the rest of the functionalities.",null, 0)
        );
    }

    private List<Item> getGraphElementsItems() {
        return List.of(
            new Item("· Named Classes: Basic concepts forming the ontology's vocabulary (gray).",
                "assets/named.png", 170),
            new Item("· Defined and Equivalent Classes: Represent concepts specified with necessary and sufficient" +
                " conditions for membership (light green).", "assets/definedEquivalent.png", 170),
            new Item("· Anonymous Classes: Concepts without explicit names in the schema, used as part of complex" +
                " expressions in the ontology (white).", "assets/anonymous.png", 170),
            new Item("· D : Indicates that the class is disjoint with other classes.",
                "assets/disjoint.png", 130),
            new Item("· P : Indicates that this specific node has properties associated with it.",
                "assets/properties.png", 170),
            new Item("· (1) : Functional property",null, 0),
            new Item("· Graph Levels : The graph is organized into horizontal levels corresponding to the maximum " +
                "hierarchical distance of a concept from OwlThing (light gray vertical lines).",
                "assets/3levels.png", 500),
            new Item("· IsA Connectors : Represent direct hierarchical relationships between concepts in the graph.",
                "assets/isA.png", 500),
            new Item("· Dashed Connectors : A special type of IsA connector. Represents an indirect hierarchical relationship.",
                "assets/dashed.png", 400),
            new Item("· Range Connectors : Are defined by a link to a particular node (light blue).\n\n" +
                "· Property Hierarchy Connectors : Object Property hierarchies are also rendered " +
                "in OntView via inheritance connectors showing their subProperty relationship (black).",
                "assets/rangeInheritance.png", 550)
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
