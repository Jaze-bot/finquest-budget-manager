//Esguerra
package com.finquest.budget_manager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent; // <-- NEW IMPORT

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class TransactionsController implements Initializable {

    @FXML
    private TableView<Transaction> transactionTableView;
    @FXML private TableColumn<Transaction, String> iconCol;
    @FXML private TableColumn<Transaction, String> titleCol;
    @FXML private TableColumn<Transaction, String> categoryCol;
    @FXML private TableColumn<Transaction, LocalDate> dateCol;
    @FXML private TableColumn<Transaction, Double> amountCol;

    @FXML
    private ComboBox<String> filterComboBox;

    // Get the master list of all transactions from the DataStore
    private final ObservableList<Transaction> masterList = DataStore.getInstance().getTransactions();

    // A filtered list that will be displayed
    private FilteredList<Transaction> filteredList;

    private BudgetApplication mainApp;
    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    // Listen for currency changes to refresh the table
    private final CurrencyUtil.CurrencyChangeListener currencyListener = (code, sym) -> Platform.runLater(() -> {
        if (transactionTableView != null) transactionTableView.refresh();
    });

    /**
     * Public setter to inject the main application instance
     */
    public void setMainApplication(BudgetApplication app) {
        this.mainApp = app;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        CurrencyUtil.addListener(currencyListener);

        // 1. Set up the filter ComboBox
        filterComboBox.setItems(FXCollections.observableArrayList("All Transactions", "Income", "Expense"));
        filterComboBox.setValue("All Transactions");

        // 2. Wrap the master list in a FilteredList
        filteredList = new FilteredList<>(masterList, p -> true); // Initially show all

        // 3. Bind the filter ComboBox to the FilteredList
        filterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            applyFilter(newVal);
        });

        // 4. Bind the TableView to the FilteredList
        transactionTableView.setItems(filteredList);

        // 5. Set up the columns with alignment and color
        setupColumns();

        // 6. Add the right-click context menu
        setupContextMenu();

        // 7. Add right-click to select
        setupRowFactory();

        // 8. Add a placeholder for when the table is empty
        transactionTableView.setPlaceholder(new Label("No transactions found."));
    }

    /**
     * Applies the filter based on the ComboBox selection.
     */
    private void applyFilter(String filterValue) {
        if (filterValue == null || "All Transactions".equals(filterValue)) {
            filteredList.setPredicate(p -> true); // Show all
        } else {
            // Show only "Income" or "Expense"
            filteredList.setPredicate(tx -> filterValue.equalsIgnoreCase(tx.getType()));
        }
    }

    private void setupColumns() {
        // --- NEW "CARTOON" ICON COLUMN ---
        iconCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        iconCol.setCellFactory(column -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String category, boolean empty) {
                super.updateItem(category, empty);
                if (empty || category == null) {
                    setText(null);
                } else {
                    setText(getEmojiForCategory(category));
                    getStyleClass().add("transaction-icon");
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Title Column
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setCellFactory(column -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        // Category Column
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setCellFactory(column -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        // Date Column
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setCellFactory(column -> new TableCell<Transaction, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(DATE_FORMATTER.format(item));
                    setAlignment(Pos.CENTER_LEFT);
                }
            }
        });

        // Amount Column
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setCellFactory(column -> new TableCell<Transaction, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("transaction-amount-in", "transaction-amount-out");

                if (empty || item == null) {
                    setText(null);
                } else {
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        Transaction tx = getTableRow().getItem();
                        if ("Income".equalsIgnoreCase(tx.getType())) {
                            setText("+" + CurrencyUtil.formatCurrency(item));
                            getStyleClass().add("transaction-amount-in");
                        } else {
                            setText("-" + CurrencyUtil.formatCurrency(item));
                            getStyleClass().add("transaction-amount-out");
                        }
                    }
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        });
    }

    /**
     * Sets up the right-click context menu for the TableView.
     */
    private void setupContextMenu() {
        if (transactionTableView == null) return;
        ContextMenu contextMenu = new ContextMenu();

        MenuItem editItem = new MenuItem("✏️ Edit Transaction");
        editItem.setOnAction(e -> {
            Transaction selected = transactionTableView.getSelectionModel().getSelectedItem();
            if (selected != null && mainApp != null) {
                mainApp.editTransaction(selected);
            }
        });

        MenuItem deleteItem = new MenuItem("🗑️ Delete Transaction");
        deleteItem.setOnAction(e -> {
            Transaction selected = transactionTableView.getSelectionModel().getSelectedItem();
            if (selected != null && mainApp != null) {
                mainApp.deleteTransaction(selected);
            }
        });

        MenuItem duplicateItem = new MenuItem("📋 Duplicate Transaction");
        duplicateItem.setOnAction(e -> {
            Transaction selected = transactionTableView.getSelectionModel().getSelectedItem();
            if (selected != null && mainApp != null) {
                mainApp.duplicateTransaction(selected);
            }
        });

        contextMenu.getItems().addAll(editItem, new SeparatorMenuItem(), deleteItem, duplicateItem);
        transactionTableView.setContextMenu(contextMenu);
    }

    /**
     * Makes right-clicking on a row select it before showing the context menu.
     */
    private void setupRowFactory() {
        transactionTableView.setRowFactory(tv -> {
            TableRow<Transaction> row = new TableRow<>();
            row.setOnMousePressed(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    // Double-click to edit
                    Transaction selected = row.getItem();
                    if(mainApp != null) mainApp.editTransaction(selected);
                } else if (!row.isEmpty() && event.getButton() == MouseButton.SECONDARY) {
                    // Right-click to select
                    transactionTableView.getSelectionModel().select(row.getIndex());
                }
            });
            return row;
        });
    }

    /**
     * Helper method to get an emoji for a category.
     */
    private String getEmojiForCategory(String category) {
        if (category == null) return "🧾"; // Default receipt
        switch (category.toLowerCase()) {
            case "food & dining": return "🍔";
            case "shopping": return "🛍️";
            case "transportation": return "🚗";
            case "bills & utilities": return "💡";
            case "entertainment": return "🎬";
            case "healthcare": return "❤️‍🩹";
            case "education": return "🎓";
            case "business": return "💼";
            case "income": case "salary": case "bonus": case "freelance": return "💰";
            default: return "🧾";
        }
    }

    public void dispose() {
        CurrencyUtil.removeListener(currencyListener);
    }

    /**
     * NEW: Called when the root VBox is clicked. Clears the table selection.
     */
    @FXML
    private void clearSelection(MouseEvent event) {
        if (transactionTableView != null) {
            transactionTableView.getSelectionModel().clearSelection();
        }
    }

    /**
     * NEW: Called when the TableView is clicked. Consumes the event to stop
     * it from bubbling up to the root VBox and clearing the selection.
     */
    @FXML
    private void consumeClick(MouseEvent event) {
        event.consume();
    }
}