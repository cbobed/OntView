package sid.OntView2.common;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.media.AudioClip;

import java.io.File;


/* This work is hereby released into the Public Domain.
 * To view a copy of the public domain dedication, visit
 * http://creativecommons.org/licenses/publicdomain/
 */
public class AutoCompletion {
    ComboBox<String> comboBox;
    TextField editor;
    // flag to indicate if setSelectedItem has been called
    // subsequent calls to remove/insertString should be ignored
    boolean selecting=false;
    boolean hidePopupOnFocusLoss;
    boolean hitBackspace=false;
    boolean hitBackspaceOnSelection;

    EventHandler<KeyEvent> editorKeyListener;
    ChangeListener<Boolean> editorFocusListener;
    
    public AutoCompletion(final ComboBox<String> comboBox) {
        this.comboBox = comboBox;
        this.editor = comboBox.getEditor();
        this.comboBox.setEditable(true);

        this.comboBox.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String oldValue, String newValue) {
                if (!selecting) highlightCompletedText(0);
            }
        });

        /*editor.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (comboBox.isShowing()) comboBox.show();
            hitBackspace = false;
            if (e.getCode() == KeyCode.BACK_SPACE) {
                hitBackspace = true;
                hitBackspaceOnSelection = editor.getSelection().getStart() != editor.getSelection().getEnd();
            } else if (e.getCode() == KeyCode.DELETE) {
                e.consume();
                playBeepSound();
            }
        });*/

        editorKeyListener = e -> {
            if (comboBox.isShowing()) comboBox.show();
            hitBackspace = false;
            if (e.getCode() == KeyCode.BACK_SPACE) {
                hitBackspace = true;
                hitBackspaceOnSelection = editor.getSelection().getStart() != editor.getSelection().getEnd();
            } else if (e.getCode() == KeyCode.DELETE) {
                e.consume();
                //playBeepSound();
            }
        };

        hidePopupOnFocusLoss = System.getProperty("java.version").startsWith("1.5");

        /*editor.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                highlightCompletedText(0);
            } else if (hidePopupOnFocusLoss) {
                comboBox.hide();
            }
        });*/

        editorFocusListener = (obs, wasFocused, isFocused) -> {
            if (isFocused) {
                highlightCompletedText(0);
            } else if (hidePopupOnFocusLoss) {
                comboBox.hide();
            }
        };

        editor.focusedProperty().addListener(editorFocusListener);

        configureEditor(comboBox.getEditor());

        Object selected = comboBox.getSelectionModel().getSelectedItem();
        if (selected != null) setText(selected.toString());
        highlightCompletedText(0);
    }
    
    public static void enable(ComboBox<String> comboBox) {
        // has to be editable
        comboBox.setEditable(true);
        // change the editor's document
        new AutoCompletion(comboBox);
    }

    /*private void playBeepSound() {
        String filePath = "sounds/beep.mp3";
        File soundFile = new File(filePath);
        AudioClip beep = new AudioClip(soundFile.toURI().toString());
        beep.play();
    }*/
    
    void configureEditor(TextField newEditor) {
        if (editor != null) {
            editor.removeEventHandler(KeyEvent.KEY_PRESSED, editorKeyListener);
            editor.focusedProperty().removeListener(editorFocusListener);        }
        
        if (newEditor != null) {
            editor = newEditor;
            editor.addEventHandler(KeyEvent.KEY_PRESSED, editorKeyListener);
            editor.focusedProperty().addListener(editorFocusListener);
        }
    }
    
    public void remove(int offs, int len) throws Exception {
        // return immediately when selecting an item
        if (selecting) return;
        if (hitBackspace) {
            // user hit backspace => move the selection backwards
            // old item keeps being selected
            if (offs>0) {
                if (hitBackspaceOnSelection) offs--;
            } else {
                // User hit backspace with the cursor positioned on the start => beep
                //playBeepSound();
            }
            highlightCompletedText(offs);
        } /*else {
            super.remove(offs, len);
        }*/
    }
    
    public void insertString(int offs, String str, Object a) throws Exception {
        // return immediately when selecting an item
        if (selecting) return;
        // insert the string into the document
        // super.insertString(offs, str, a);
        // lookup and select a matching item
        TextField editor = comboBox.getEditor();
        String currentText = editor.getText();
        String newText = currentText.substring(0, offs) + str + currentText.substring(offs);
        Object item = lookupItem(newText);

        if (item != null) {
            setSelectedItem(item);
        } else {
            // keep old item selected if there is no match
            item = comboBox.getSelectionModel().getSelectedItem();
            // imitate no insert (later on offs will be incremented by str.length(): selection won't move forward)
            offs = offs-str.length();
            // provide feedbaOntology(<http://protege.stanford.edu/plugins/owl/owl-library/koala.owl> [Axioms: 70] [Logical axioms: 42])ck to the user that his input has been received but can not be accepted
            //playBeepSound();
        }
        setText(item.toString());
        // select the completed part
        highlightCompletedText(offs+str.length());
    }
    
    private void setText(String text) {
        TextField editor = comboBox.getEditor();
        Platform.runLater(() -> {
            editor.setText(text);
            editor.positionCaret(text.length());
        });
    }
    
    private void highlightCompletedText(int start) {
        TextField editor = comboBox.getEditor();
        Platform.runLater(() -> {
            editor.selectRange(start, editor.getText().length());
        });
    }
    
    private void setSelectedItem(Object item) {
        selecting = true;
        comboBox.getSelectionModel().select(item.toString());
        selecting = false;
    }
    
    private Object lookupItem(String pattern) {
        Object selectedItem = comboBox.getSelectionModel().getSelectedItem();
        if (selectedItem != null && startsWithIgnoreCase(selectedItem.toString(), pattern)) {
            return selectedItem;
        } else {
            for (String currentItem : comboBox.getItems()) {
                if (startsWithIgnoreCase(currentItem, pattern)) {
                    return currentItem;
                }
            }
        }
        return null;
    }
    
    // checks if str1 starts with str2 - ignores case
    private boolean startsWithIgnoreCase(String str1, String str2) {
        return str1.toUpperCase().startsWith(str2.toUpperCase());
    }
    

}
