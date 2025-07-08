package sid.OntView2.main;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sid.OntView2.common.Shape;
import sid.OntView2.common.VisClass;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ClassExpression extends Stage {
    private static final Logger logger = LogManager.getLogger(ClassExpression.class);

    private final Mine parent;
    private VisClass selectedParent, selectedChild;
    private TextField parentSearchField, childSearchField;
    private ListView<CheckBox> parentCheckBoxList , childCheckBoxList;
    private Button submitButtonCE;

    public ClassExpression(Mine parent) {
        this.parent = parent;
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Query (Class Expression)");
        setResizable(false);

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
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        submitHelp.getChildren().addAll(spacer, submitCE());
        submitHelp.setStyle("-fx-alignment: center-left;");

        classExpressionBox.getChildren().addAll(bottomSection, submitHelp);

        Scene scene = new Scene(classExpressionBox, 800, 500);
        ClassLoader c = Thread.currentThread().getContextClassLoader();
        scene.getStylesheets().add(Objects.requireNonNull(c.getResource("styles.css")).toExternalForm());
        setScene(scene);
        show();
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
        VBox parentListBox = new VBox(5);
        StackPane parentTitle = createTitlePane("Parent");
        parentSearchField = new TextField();
        parentSearchField.setPromptText("Search node...");

        parentCheckBoxList = new ListView<>();

        ObservableList<CheckBox> checkBoxList = toCheckBoxList(getAllShapeMap(), true);
        parentCheckBoxList.setItems(checkBoxList);

        parentSearchField.textProperty().addListener((obs, oldV, newV) ->
            filterAndResetCheckboxes(true, newV)
        );

        parentListBox.getChildren().addAll(parentTitle, parentSearchField, parentCheckBoxList);
        parentListBox.setMaxWidth(Double.MAX_VALUE);

        return parentListBox;
    }

    private VBox selectChildCheckBox() {
        VBox childListBox = new VBox(5);
        StackPane childTitle = createTitlePane("Child");
        childSearchField = new TextField();
        childSearchField.setPromptText("Search node...");

        childCheckBoxList = new ListView<>();

        ObservableList<CheckBox> checkBoxList = toCheckBoxList(getAllShapeMap(), false);
        childCheckBoxList.setItems(checkBoxList);

        childSearchField.textProperty().addListener((obs, oldV, newV) ->
            filterAndResetCheckboxes(false, newV)
        );
        childListBox.getChildren().addAll(childTitle, childSearchField, childCheckBoxList);
        childListBox.setMaxWidth(Double.MAX_VALUE);

        return childListBox;
    }

    /**
     * Generic handler for checkbox selection in either panel.
     */
    private void handleCheckBoxSelection(CheckBox selectedCheckBox, boolean isParentPanel) {
        ListView<CheckBox> listView = isParentPanel ? parentCheckBoxList : childCheckBoxList;
        for (CheckBox cb : listView.getItems()) {
            if (cb != selectedCheckBox) {
                cb.setSelected(false);
            }
        }

        boolean isSelected = selectedCheckBox.isSelected();
        if (isParentPanel) {
            if (isSelected) {
                selectedParent = getShapeByLabel(selectedCheckBox.getText());
                parentSearchField.setPromptText(selectedCheckBox.getText());
            } else {
                selectedParent = null;
                parentSearchField.setPromptText("Search node...");
            }
        } else {
            if (isSelected) {
                selectedChild = getShapeByLabel(selectedCheckBox.getText());
                childSearchField.setPromptText(selectedCheckBox.getText());
            } else {
                selectedChild = null;
                childSearchField.setPromptText("Search node...");
            }
        }

        boolean otherHasSelection = isParentPanel ? selectedChild != null : selectedParent != null;
        boolean bothSearchEmpty = parentSearchField.getText().isEmpty() && childSearchField.getText().isEmpty();
        if (!isSelected && !otherHasSelection && bothSearchEmpty) {
            parentCheckBoxList.setItems(toCheckBoxList(getAllShapeMap(), true));
            childCheckBoxList.setItems(toCheckBoxList(getAllShapeMap(), false));
            return;
        }

        if (otherHasSelection) return;

        String filterText = (isParentPanel ? childSearchField : parentSearchField).getText();
        filterAndResetCheckboxes(!isParentPanel, filterText == null ? "" : filterText);
    }

    /** Handles the selection of a checkbox in the parent panel. */
    private void handleParentCheckBoxSelection(CheckBox cb, ObservableList<CheckBox> ignore) {
        handleCheckBoxSelection(cb, true);
    }

    /** Handles the selection of a checkbox in the child panel. */
    private void handleChildCheckBoxSelection(CheckBox cb, ObservableList<CheckBox> ignore) {
        handleCheckBoxSelection(cb, false);
    }

    /**
     * Returns the set of Shape objects to be used as the base list based on the current selection state.
     */
    private Set<Shape> getBaseSet() {
        Set<Shape> result = new HashSet<>();
        if (selectedParent != null) {
            for (Shape s : selectedParent.getDescendants()) {
                if (!s.asVisClass().isAnonymous() && s.isVisible()) {
                    result.add(s);
                }
            }
            result.add(getShapeByLabel("Nothing"));
        } else if (selectedChild != null) {
            for (Shape s : selectedChild.getAncestors()) {
                if (!s.asVisClass().isAnonymous() && s.isVisible()) {
                    result.add(s);
                }
            }
        } else {
            return getAllShapeMap();
        }
        return result;
    }


    /**
     * Repopulates the ListView with CheckBoxes from the base list (parents or children) that contain the search text.
     */
    private void filterAndResetCheckboxes(boolean isParentPanel, String searchText) {
        Set<Shape> baseSet = getBaseSet();
        ObservableList<CheckBox> filtered = FXCollections.observableArrayList();
        for (Shape node : baseSet) {
            String label = node.getLabel().toLowerCase();
            if (label.contains(searchText.toLowerCase())) {
                CheckBox cb = new CheckBox(node.getLabel());
                if (isParentPanel) {
                    cb.setOnAction(evt -> handleParentCheckBoxSelection(cb, null));
                } else {
                    cb.setOnAction(evt -> handleChildCheckBoxSelection(cb, null));
                }
                filtered.add(cb);
            }
        }
        if (isParentPanel) {
            parentCheckBoxList.setItems(filtered);
        } else {
            childCheckBoxList.setItems(filtered);
        }
    }

    /**
     * Retrieves all shapes
     */
    private Set<Shape> getAllShapeMap() {
        Set<Shape> allNodes = new HashSet<>();
        if (parent.artPanel.getVisGraph() != null) {
            Map<String, Shape> shapeMap = parent.artPanel.getVisGraph().shapeMap;
            for (Shape shape : shapeMap.values()) {
                if (!shape.asVisClass().isAnonymous() && shape.isVisible()) {
                    allNodes.add(shape);
                }
            }
        }
        return allNodes;
    }

    /**
     * Converts a collection of Shape nodes into an ObservableList of CheckBoxes,
     * attaching the appropriate selection handler for each.
     */
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

    /**
     * Finds and returns the VisClass whose label matches the given text.
     */
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

    private void submitButtonCEActionActionPerformed(ActionEvent event) {
        if ( selectedChild == null) {
            logger.warn("Please select a child node class expression query.");
        }
        if ( selectedParent == null) {
            logger.warn("Please select a parent node for class expression query.");
        }
        if (selectedChild != null && selectedParent != null) {
            parent.artPanel.cleanConnectors();
            parent.artPanel.getVisGraph().rebuildGraphSelectedNodes(selectedParent.getLinkedClassExpression(), selectedChild.getLinkedClassExpression());
            close();
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
}

