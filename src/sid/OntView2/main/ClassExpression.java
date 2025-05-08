package sid.OntView2.main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sid.OntView2.common.Shape;
import sid.OntView2.common.VisClass;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ClassExpression extends Stage {
    private final Mine parent;
    private VBox parentListBox, childListBox;
    private VisClass selectedParent, selectedChild;
    private TextField parentSearchField, childSearchField;
    private ListView<CheckBox> parentCheckBoxList , childCheckBoxList;
    private Button submitButtonCE, helpButtonCE;

    public ClassExpression(Mine parent) {
        this.parent = parent;
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Query (Class Expression)");
        setMinWidth(800);
        setMinHeight(500);
        setMaxHeight(700);

        // Main layout container
        VBox classExpressionBox = new VBox(10);
        classExpressionBox.setPadding(new Insets(10));

        HBox bottomSection = new HBox(10);
        bottomSection.setPadding(new Insets(10, 0, 0, 0));

        VBox parentBoxContainer = selectParentCheckBox();
        VBox childBoxContainer = selectChildCheckBox();
        bottomSection.getChildren().addAll(parentBoxContainer, childBoxContainer);
        HBox.setHgrow(parentBoxContainer, Priority.ALWAYS);
        HBox.setHgrow(childBoxContainer, Priority.ALWAYS);

        HBox submitHelp = new HBox(5);
        Button helpButton = getHelpButtonCE();
        Button submitButton = submitCE();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        submitHelp.getChildren().addAll(helpButton, spacer, submitButton);
        submitHelp.setStyle("-fx-alignment: center-left;");

        classExpressionBox.getChildren().addAll(bottomSection, submitHelp);

        Scene scene = new Scene(classExpressionBox, 800, 500);
        ClassLoader c = Thread.currentThread().getContextClassLoader();
        scene.getStylesheets().add(Objects.requireNonNull(c.getResource("styles.css")).toExternalForm());
        setScene(scene);
        show();
    }

    private Button getHelpButtonCE(){
        if (helpButtonCE == null) {
            helpButtonCE = new Button("?");
            helpButtonCE.getStyleClass().add("button");
            helpButtonCE.setStyle(
                "-fx-font-size: 14px; -fx-shape: 'M 0 50 a 50 50 0 1 1 100 0 a 50 50 0 1 1 -100 0'; " +
                    "-fx-background-radius: 50%; -fx-min-width: 35px; -fx-min-height: 35px; " +
                    "-fx-pref-width: 35px; -fx-pref-height: 35px;"
            );
            //helpButtonCE.setOnAction(this::helpButtonCEActionActionPerformed);
        }
        return helpButtonCE;
    }

    private Button submitCE(){
        if (submitButtonCE == null) {
            submitButtonCE = new Button("Submit");
            submitButtonCE.getStyleClass().add("button");
            submitButtonCE.setCursor(Cursor.HAND);
            submitButtonCE.setMinWidth(40);

            submitButtonCE.setOnAction(this::submitButtonCEActionActionPerformed);
        }
        return submitButtonCE;
    }

    private VBox selectParentCheckBox() {
        parentListBox = new VBox(5);
        StackPane parentTitle = createTitlePane("Parent");
        parentSearchField = new TextField();
        parentSearchField.setPromptText("Search node...");

        parentCheckBoxList = new ListView<>();

        ObservableList<CheckBox> checkBoxList = toCheckBoxList(getAllShapeMap(), true);
        parentCheckBoxList.setItems(checkBoxList);

        parentSearchField.textProperty().addListener((observable, oldValue, newValue) -> filterCheckBoxes(checkBoxList, newValue, parentCheckBoxList));

        parentListBox.getChildren().addAll(parentTitle, parentSearchField, parentCheckBoxList);
        parentListBox.setMaxWidth(Double.MAX_VALUE);

        return parentListBox;
    }

    private VBox selectChildCheckBox() {
        childListBox = new VBox(5);
        StackPane childTitle = createTitlePane("Child");
        childSearchField = new TextField();
        childSearchField.setPromptText("Search node...");

        childCheckBoxList = new ListView<>();

        ObservableList<CheckBox> checkBoxList = toCheckBoxList(getAllShapeMap(), false);
        childCheckBoxList.setItems(checkBoxList);

        childSearchField.textProperty().addListener((observable, oldValue, newValue) -> filterCheckBoxes(checkBoxList, newValue, childCheckBoxList));

        childListBox.getChildren().addAll(childTitle, childSearchField, childCheckBoxList);
        childListBox.setMaxWidth(Double.MAX_VALUE);

        return childListBox;
    }

    private void handleParentCheckBoxSelection(CheckBox selectedCheckBox, ObservableList<CheckBox> checkBoxList) {
        boolean isSelected = selectedCheckBox.isSelected();
        for (CheckBox checkBox : checkBoxList) {
            if (checkBox != selectedCheckBox) {
                checkBox.setSelected(false);
            }
        }

        if (isSelected) {
            parentSearchField.setPromptText(selectedCheckBox.getText());
            selectedParent = getShapeByLabel(selectedCheckBox.getText());
            if (selectedChild != null) return;
            if (selectedParent != null) {
                Set<Shape> descendants = selectedParent.getDescendants();
                childCheckBoxList.setItems(toCheckBoxList(descendants, false));
            }
        } else {
            parentSearchField.setPromptText("Search node...");
            selectedParent = null;
            if (selectedChild == null) {
                childCheckBoxList.setItems(toCheckBoxList(getAllShapeMap(), false));
                parentCheckBoxList.setItems(toCheckBoxList(getAllShapeMap(), true));
            }
        }
    }

    private void handleChildCheckBoxSelection(CheckBox selectedCheckBox, ObservableList<CheckBox> checkBoxList) {
        boolean isSelected = selectedCheckBox.isSelected();
        for (CheckBox checkBox : checkBoxList) {
            if (checkBox != selectedCheckBox) {
                checkBox.setSelected(false);
            }
        }

        if (isSelected) {
            childSearchField.setPromptText(selectedCheckBox.getText());
            selectedChild = getShapeByLabel(selectedCheckBox.getText());
            if (selectedParent != null) return;
            if (selectedChild != null) {
                Set<Shape> ancestors = selectedChild.getAncestors();
                parentCheckBoxList.setItems(toCheckBoxList(ancestors, true));
            }
        } else {
            childSearchField.setPromptText("Search node...");
            selectedChild = null;
            if (selectedParent == null) {
                parentCheckBoxList.setItems(toCheckBoxList(getAllShapeMap(), true));
                childCheckBoxList.setItems(toCheckBoxList(getAllShapeMap(), false));
            }
        }
    }

    private Set<Shape> getAllShapeMap() {
        Set<Shape> allNodes = new HashSet<>();
        if (parent.artPanel.getVisGraph() != null) {
            Map<String, Shape> shapeMap = parent.artPanel.getVisGraph().shapeMap;
            for (Shape shape : shapeMap.values()) {
                if (shape instanceof VisClass) {
                    allNodes.add(shape);
                }
            }
        }
        return allNodes;
    }

    private ObservableList<CheckBox> toCheckBoxList(Set<Shape> nodeList, Boolean isParent) {
        ObservableList<CheckBox> checkBoxList = FXCollections.observableArrayList();
        for (Shape node : nodeList) {
            CheckBox checkBox = new CheckBox(node.getLabel());
            checkBoxList.add(checkBox);
            if (isParent){
                checkBox.setOnAction(event -> handleParentCheckBoxSelection(checkBox, checkBoxList));
            } else {
                checkBox.setOnAction(event -> handleChildCheckBoxSelection(checkBox, checkBoxList));
            }
        }
        return checkBoxList;
    }

    private VisClass getShapeByLabel(String label) {
        if (parent.artPanel.getVisGraph() != null) {
            Map<String, Shape> shapeMap = parent.artPanel.getVisGraph().shapeMap;
            for (Shape shape : shapeMap.values()) {
                if (shape instanceof VisClass && shape.getLabel().equals(label)) {
                    return (VisClass) shape;
                }
            }
        }
        return null;
    }

    private void filterCheckBoxes(ObservableList<CheckBox> checkBoxList, String searchText, ListView<CheckBox> childCheckBoxList) {
        ObservableList<CheckBox> filteredList = FXCollections.observableArrayList();
        for (CheckBox checkBox : checkBoxList) {
            if (checkBox.getText().toLowerCase().contains(searchText.toLowerCase())) {
                filteredList.add(checkBox);
            }
        }
        childCheckBoxList.setItems(filteredList);
    }

    private void submitButtonCEActionActionPerformed(ActionEvent event) {
        if ( selectedChild == null) {
            System.out.println("Please select a child node");
        }
        if ( selectedParent == null) {
            System.out.println("Please select a parent node");
        }
        if (selectedChild != null && selectedParent != null) {
            parent.artPanel.getVisGraph().rebuildGraphSelectedNodes(selectedParent.getLinkedClassExpression(), selectedChild.getLinkedClassExpression());
            System.out.println("selectedChild " + selectedChild.getLabel() + " selectedParent " + selectedParent.getLabel());
        }
    }

    // Auxiliary methods
    private StackPane createTitlePane(String title) {
        Text titledPane = new Text(title);
        titledPane.setFill(Color.BLUE);
        titledPane.setFont(Font.font("DejaVu Sans", FontWeight.BOLD, FontPosture.ITALIC, 12));

        StackPane titlePane = new StackPane();
        titlePane.getChildren().add(titledPane);

        return titlePane;
    }

    private VBox createContainerNoSize(Node... children) {
        VBox container = new VBox(7);
        container.getChildren().addAll(children);
        container.setPadding(new Insets(10, 10, 10, 10));
        container.getStyleClass().add("custom-container");
        container.setAlignment(Pos.CENTER);
        return container;
    }
}

