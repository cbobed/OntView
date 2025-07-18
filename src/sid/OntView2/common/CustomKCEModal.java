package sid.OntView2.common;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sid.OntView2.kcExtractors.CustomConceptExtraction;

import java.util.*;
import java.util.stream.Collectors;

public class CustomKCEModal {
    private final Map<String, Shape> shapeMap;
    CustomConceptExtraction customConceptExtraction;
    private GraphViewSettings settings;
    private ObservableList<Shape> tempSelectedConceptsList = FXCollections.observableArrayList();

    public CustomKCEModal(Map<String, Shape> shapeMap, CustomConceptExtraction customCE) {
        this.shapeMap = shapeMap;
        this.customConceptExtraction = customCE;
    }

    public void showConceptSelectionPopup() {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Select Key Concepts");

        settings = GraphViewSettings.getInstance();
        checkSettingMode(settings);

        List<Shape> unique = shapeMap.values().stream().distinct().toList();
        ObservableList<Shape> allConcepts = FXCollections.observableArrayList(unique);
        ObservableList<Shape> selectedConceptsList = FXCollections.observableArrayList(customConceptExtraction.getSelectedConcepts());

        ListView<Shape> allConceptsView = createConceptListView(allConcepts);
        ListView<Shape> selectedConceptsView = createConceptListView(selectedConceptsList);

        setupDoubleClickActions(allConceptsView, selectedConceptsView, selectedConceptsList);

        TextField searchAllField = createSearchField(allConcepts, allConceptsView);
        TextField searchSelectedField = createSearchField(selectedConceptsList, selectedConceptsView);

        Button addButton = createAddButton(allConceptsView, selectedConceptsList);
        Button removeButton = createRemoveButton(selectedConceptsView, selectedConceptsList);
        Button saveButton = createSaveButton(selectedConceptsList, popupStage);
        Button resetButton = createResetButton(selectedConceptsList);

        CheckBox selectAllNodes = selectAllNodesCheckbox(unique, selectedConceptsList);

        HBox arrowBox = new HBox(5, addButton, removeButton);
        arrowBox.setAlignment(Pos.CENTER);
        VBox buttonBox = new VBox(15, arrowBox, resetButton);
        buttonBox.setAlignment(Pos.CENTER);

        VBox allConceptsBox = new VBox(5, searchAllField, allConceptsView);
        VBox selectedConceptsBox = new VBox(5, searchSelectedField, selectedConceptsView);

        HBox mainLayout = new HBox(10, allConceptsBox, buttonBox, selectedConceptsBox);
        VBox root = new VBox(10, selectAllNodes, mainLayout, saveButton);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 600, 400);
        ClassLoader c = Thread.currentThread().getContextClassLoader();
        scene.getStylesheets().add(Objects.requireNonNull(c.getResource("styles.css")).toExternalForm());
        popupStage.setScene(scene);

        popupStage.setOnCloseRequest(e -> {
            if (settings.getLastMode() == ViewMode.CUSTOM) {
                settings.setLastMode(ViewMode.CANCELLED_CUSTOM);
                Set<Shape> nonHiddenShapes = selectedConceptsList.stream()
                    .filter(Shape::isVisible)
                    .collect(Collectors.toSet());

                customConceptExtraction.getTempSelectedConcepts().clear();
                customConceptExtraction.getTempSelectedConcepts().addAll(selectedConceptsList);

                customConceptExtraction.getSelectedConcepts().clear();
                customConceptExtraction.getSelectedConcepts().addAll(nonHiddenShapes);
            } else {
                customConceptExtraction.getSelectedConcepts().clear();
                customConceptExtraction.getSelectedConcepts().addAll(allConcepts);
            }
        });
        popupStage.showAndWait();
    }

    private void checkSettingMode(GraphViewSettings settings) {
        if (settings.getLastMode() == ViewMode.ALL) {
            customConceptExtraction.getSelectedConcepts().clear();
        }
    }

    private void setupDoubleClickActions(ListView<Shape> allConceptsView, ListView<Shape> selectedConceptsView,
                                         ObservableList<Shape> selectedConceptsList) {
        allConceptsView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Shape selected = allConceptsView.getSelectionModel().getSelectedItem();
                if (selected != null && !selectedConceptsList.contains(selected)) {
                    selectedConceptsList.add(selected);
                }
            }
        });

        selectedConceptsView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Shape selected = selectedConceptsView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    selectedConceptsList.remove(selected);
                }
            }
        });
    }

    private ListView<Shape> createConceptListView(ObservableList<Shape> concepts) {
        ListView<Shape> listView = new ListView<>(concepts);
        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Shape item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getLabel());
                }
            }
        });
        return listView;
    }

    private TextField createSearchField(ObservableList<Shape> concepts, ListView<Shape> listView) {
        TextField searchField = new TextField();
        searchField.setPromptText("Search nodes");
        searchField.textProperty().addListener((observable, oldValue, newValue) ->
            listView.setItems(concepts.filtered(item -> item.getLabel().toLowerCase().contains(newValue.toLowerCase()))));
        return searchField;
    }

    private CheckBox selectAllNodesCheckbox(List<Shape> allConcepts, ObservableList<Shape> selectedConceptsList) {
        CheckBox selectAllNodes = new CheckBox("Select all nodes");
        selectAllNodes.setOnAction(e -> {
            if (selectAllNodes.isSelected()) {
                tempSelectedConceptsList = FXCollections.observableArrayList(selectedConceptsList);
                selectedConceptsList.clear();
                selectedConceptsList.addAll(allConcepts);
            } else {
                selectedConceptsList.clear();
                selectedConceptsList.addAll(tempSelectedConceptsList);
            }
        });
        return selectAllNodes;
    }

    private Button createAddButton(ListView<Shape> allConceptsView, ObservableList<Shape> selectedConceptsList) {
        Button addButton = new Button(OntViewConstants.RIGHT_ARROW);
        addButton.setOnAction(e -> {
            Shape selected = allConceptsView.getSelectionModel().getSelectedItem();
            if (selected != null && !selectedConceptsList.contains(selected)) {
                selectedConceptsList.add(selected);
            }
        });
        return addButton;
    }

    private Button createRemoveButton(ListView<Shape> selectedConceptsView, ObservableList<Shape> selectedConceptsList) {
        Button removeButton = new Button(OntViewConstants.LEFT_ARROW);
        removeButton.setOnAction(e -> {
            Shape selected = selectedConceptsView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                selectedConceptsList.remove(selected);
            }
        });
        return removeButton;
    }

    private Button createResetButton(ObservableList<Shape> selectedConceptsList) {
        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> selectedConceptsList.clear());
        return resetButton;
    }


    private Button createSaveButton(ObservableList<Shape> selectedConceptsList, Stage popupStage) {
        Button saveButton = new Button("Save");
        saveButton.setOnAction(e -> {
            customConceptExtraction.getSelectedConcepts().clear();
            customConceptExtraction.getSelectedConcepts().addAll(selectedConceptsList);
            tempSelectedConceptsList.clear();
            settings.setLastMode(ViewMode.CUSTOM);
            popupStage.close();
        });
        return saveButton;
    }
}

