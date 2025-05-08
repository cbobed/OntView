[1mdiff --git a/src/sid/OntView2/main/ClassExpression.java b/src/sid/OntView2/main/ClassExpression.java[m
[1mindex 5c7db18..f850e71 100644[m
[1m--- a/src/sid/OntView2/main/ClassExpression.java[m
[1m+++ b/src/sid/OntView2/main/ClassExpression.java[m
[36m@@ -4,9 +4,7 @@[m [mimport javafx.collections.FXCollections;[m
 import javafx.collections.ObservableList;[m
 import javafx.event.ActionEvent;[m
 import javafx.geometry.Insets;[m
[31m-import javafx.geometry.Pos;[m
 import javafx.scene.Cursor;[m
[31m-import javafx.scene.Node;[m
 import javafx.scene.Scene;[m
 import javafx.scene.control.Button;[m
 import javafx.scene.control.CheckBox;[m
[36m@@ -34,6 +32,7 @@[m [mpublic class ClassExpression extends Stage {[m
     private VisClass selectedParent, selectedChild;[m
     private TextField parentSearchField, childSearchField;[m
     private ListView<CheckBox> parentCheckBoxList , childCheckBoxList;[m
[32m+[m[32m    private ObservableList<CheckBox> parentMasterList, childMasterList;[m
     private Button submitButtonCE, helpButtonCE;[m
 [m
     public ClassExpression(Mine parent) {[m
[36m@@ -108,12 +107,11 @@[m [mpublic class ClassExpression extends Stage {[m
         parentSearchField = new TextField();[m
         parentSearchField.setPromptText("Search node...");[m
 [m
[31m-        parentCheckBoxList = new ListView<>();[m
[32m+[m[32m        parentMasterList = FXCollections.observableArrayList(toCheckBoxList(getAllShapeMap(), true));[m
[32m+[m[32m        parentCheckBoxList = new ListView<>(parentMasterList);[m
 [m
[31m-        ObservableList<CheckBox> checkBoxList = toCheckBoxList(getAllShapeMap(), true);[m
[31m-        parentCheckBoxList.setItems(checkBoxList);[m
[31m-[m
[31m-        parentSearchField.textProperty().addListener((observable, oldValue, newValue) -> filterCheckBoxes(checkBoxList, newValue, parentCheckBoxList));[m
[32m+[m[32m        parentSearchField.textProperty().addListener((observable, oldValue, newValue) ->[m
[32m+[m[32m            filterCheckBoxes(parentMasterList, newValue, parentCheckBoxList));[m
 [m
         parentListBox.getChildren().addAll(parentTitle, parentSearchField, parentCheckBoxList);[m
         parentListBox.setMaxWidth(Double.MAX_VALUE);[m
[36m@@ -127,12 +125,11 @@[m [mpublic class ClassExpression extends Stage {[m
         childSearchField = new TextField();[m
         childSearchField.setPromptText("Search node...");[m
 [m
[31m-        childCheckBoxList = new ListView<>();[m
[31m-[m
[31m-        ObservableList<CheckBox> checkBoxList = toCheckBoxList(getAllShapeMap(), false);[m
[31m-        childCheckBoxList.setItems(checkBoxList);[m
[32m+[m[32m        childMasterList = FXCollections.observableArrayList(toCheckBoxList(getAllShapeMap(), true));[m
[32m+[m[32m        childCheckBoxList = new ListView<>(childMasterList);[m
 [m
[31m-        childSearchField.textProperty().addListener((observable, oldValue, newValue) -> filterCheckBoxes(checkBoxList, newValue, childCheckBoxList));[m
[32m+[m[32m        childSearchField.textProperty().addListener((observable, oldValue, newValue) ->[m
[32m+[m[32m            filterCheckBoxes(childMasterList, newValue, childCheckBoxList));[m
 [m
         childListBox.getChildren().addAll(childTitle, childSearchField, childCheckBoxList);[m
         childListBox.setMaxWidth(Double.MAX_VALUE);[m
[36m@@ -231,9 +228,9 @@[m [mpublic class ClassExpression extends Stage {[m
         return null;[m
     }[m
 [m
[31m-    private void filterCheckBoxes(ObservableList<CheckBox> checkBoxList, String searchText, ListView<CheckBox> childCheckBoxList) {[m
[32m+[m[32m    private void filterCheckBoxes(ObservableList<CheckBox> masterList, String searchText, ListView<CheckBox> childCheckBoxList) {[m
         ObservableList<CheckBox> filteredList = FXCollections.observableArrayList();[m
[31m-        for (CheckBox checkBox : checkBoxList) {[m
[32m+[m[32m        for (CheckBox checkBox : masterList) {[m
             if (checkBox.getText().toLowerCase().contains(searchText.toLowerCase())) {[m
                 filteredList.add(checkBox);[m
             }[m
[36m@@ -265,14 +262,5 @@[m [mpublic class ClassExpression extends Stage {[m
 [m
         return titlePane;[m
     }[m
[31m-[m
[31m-    private VBox createContainerNoSize(Node... children) {[m
[31m-        VBox container = new VBox(7);[m
[31m-        container.getChildren().addAll(children);[m
[31m-        container.setPadding(new Insets(10, 10, 10, 10));[m
[31m-        container.getStyleClass().add("custom-container");[m
[31m-        container.setAlignment(Pos.CENTER);[m
[31m-        return container;[m
[31m-    }[m
 }[m
 [m
