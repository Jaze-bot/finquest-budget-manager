//Chiong
package com.finquest.budget_manager;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * A custom TableCell for editing LocalDate values using a DatePicker.
 */
public class DateEditingCell extends TableCell<Transaction, LocalDate> {

    private DatePicker datePicker;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private final Pos alignment;

    public DateEditingCell(Pos alignment) {
        super();
        this.alignment = alignment;
        setAlignment(alignment);
    }

    @Override
    public void startEdit() {
        if (!isEmpty()) {
            super.startEdit();
            createDatePicker();
            setText(null);
            setGraphic(datePicker);
            // Platform.runLater is needed to give the DatePicker focus
            Platform.runLater(() -> datePicker.requestFocus());
        }
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        // Check for null item before formatting
        if(getItem() != null) {
            setText(getItem().format(formatter));
        }
        setGraphic(null);
    }

    @Override
    public void updateItem(LocalDate item, boolean empty) {
        super.updateItem(item, empty);
        setAlignment(alignment); // Ensure alignment is always set

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (datePicker != null) {
                    datePicker.setValue(item); // Use the passed-in item
                }
                setText(null);
                setGraphic(datePicker);
            } else {
                setText(item.format(formatter));
                setGraphic(null);
            }
        }
    }

    private void createDatePicker() {
        // This now correctly calls the final getItem() method from the Cell superclass
        datePicker = new DatePicker(getItem());
        datePicker.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);

        // Commit edit when a new date is chosen
        datePicker.setOnAction((e) -> {
            commitEdit(datePicker.getValue());
        });

        // Cancel edit if focus is lost
        datePicker.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                // Check for null item before formatting
                if(getItem() != null) {
                    cancelEdit();
                }
            }
        });
    }
}