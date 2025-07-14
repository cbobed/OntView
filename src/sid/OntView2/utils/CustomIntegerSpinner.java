package sid.OntView2.utils;

import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;


public class CustomIntegerSpinner extends Spinner<Integer> {

    private final int min;
    private final int max;

    public CustomIntegerSpinner(int minValue, int maxValue, int initialValue) {
        super();
        this.min = minValue;
        this.max = maxValue;

        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initialValue) {
                @Override
                public void decrement(int steps) {
                    if (getEditor().getText().trim().isEmpty()) {
                        setValue(0);
                    }
                    super.decrement(steps);
                }

                @Override
                public void increment(int steps) {
                    if (getEditor().getText().trim().isEmpty()) {
                        setValue(0);
                    }
                    super.increment(steps);
                }
            };

        valueFactory.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "" : value.toString();
            }

            @Override
            public Integer fromString(String text) {
                if (text == null || text.trim().isEmpty()) {
                    return 0;
                }
                try {
                    int v = Integer.parseInt(text.trim());
                    if (v < min) return min;
                    return Math.min(v, max);
                } catch (NumberFormatException e) {
                    return getValue();
                }
            }
        });

        setValueFactory(valueFactory);
        setEditable(true);
        getStyleClass().add("spinner-custom");

        TextField editor = getEditor();
        editor.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                return;
            }
            if (!newValue.matches("\\d*")) {
                editor.setText(oldValue);
            } else {
                try {
                    int value = Integer.parseInt(newValue);
                    if (value < min || value > max) {
                        editor.setText(oldValue);
                    }
                } catch (NumberFormatException e) {
                    editor.setText(oldValue);
                }
            }
        });
    }
}
