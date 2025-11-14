//Esguerra
package com.finquest.budget_manager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent; // <-- NEW IMPORT
import javafx.scene.layout.StackPane;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.Callback;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class IncomeController implements Initializable {

    // FXML REFERENCES
    @FXML private StackPane donutChartPane;
    @FXML private PieChart budgetPieChart;
    @FXML private Label leftToSpendLabel;
    @FXML private Label monthlyBudgetLabel;
    @FXML private Label totalIncomeLabel;
    @FXML private Label totalExpensesLabel;
    @FXML private Label networkStatusLabel;

    @FXML private TableView<Transaction> incomeTableView;
    @FXML private TableColumn<Transaction, String> titleCol;
    @FXML private TableColumn<Transaction, String> categoryCol;
    @FXML private TableColumn<Transaction, LocalDate> dateCol;
    @FXML private TableColumn<Transaction, Double> amountCol;

    @FXML private TextField transactionTitleField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextField amountField;
    @FXML private TextField dateField;
    @FXML private Button addButton;

    private final FilteredList<Transaction> incomeFiltered =
            new FilteredList<>(DataStore.getInstance().getTransactions(), t -> "Income".equalsIgnoreCase(t.getType()));

    private final ObservableList<String> incomeCategories = FXCollections.observableArrayList(
            "Salary", "Bonus", "Freelance", "Interest", "Refund", "Other"
    );

    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private BudgetApplication mainApp;

    // --- Budget Listener ---
    private final BudgetManager.BudgetChangeListener budgetListener = (newBudget) -> {
        Platform.runLater(this::updateTotals);
    };

    // Currency listener
    private final CurrencyUtil.CurrencyChangeListener currencyListener = (code, sym) -> Platform.runLater(() -> {
        updateTotals();
        if (incomeTableView != null) incomeTableView.refresh();
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
        BudgetManager.addListener(budgetListener);

        setupColumns();
        setupForm();

        if (incomeTableView != null) {
            incomeTableView.setItems(incomeFiltered);
            incomeTableView.setEditable(true);

            setupContextMenu();
            setupRowFactory();
        }

        if (addButton != null) addButton.setOnAction(e -> handleAddTransaction());

        if (donutChartPane != null && budgetPieChart != null) {
            budgetPieChart.setLabelsVisible(false);
            donutChartPane.setOnMouseEntered(e -> budgetPieChart.setLabelsVisible(true));
            donutChartPane.setOnMouseExited(e -> budgetPieChart.setLabelsVisible(false));
        }

        Platform.runLater(() -> {
            if (dateField != null && (dateField.getText() == null || dateField.getText().isEmpty()))
                dateField.setText(LocalDate.now().format(DATE_FORMATTER));
        });

        updateTotals();
    }

    /**
     * Sets up the right-click context menu for the table.
     */
    private void setupContextMenu() {
        if (incomeTableView == null) return;
        ContextMenu contextMenu = new ContextMenu();

        MenuItem editItem = new MenuItem("✏️ Edit Transaction");
        editItem.setOnAction(e -> {
            Transaction selected = incomeTableView.getSelectionModel().getSelectedItem();
            if (selected != null && mainApp != null) {
                mainApp.editTransaction(selected);
            }
        });

        MenuItem deleteItem = new MenuItem("🗑️ Delete Transaction");
        deleteItem.setOnAction(e -> {
            Transaction selected = incomeTableView.getSelectionModel().getSelectedItem();
            if (selected != null && mainApp != null) {
                mainApp.deleteTransaction(selected);
            }
        });

        MenuItem duplicateItem = new MenuItem("📋 Duplicate Transaction");
        duplicateItem.setOnAction(e -> {
            Transaction selected = incomeTableView.getSelectionModel().getSelectedItem();
            if (selected != null && mainApp != null) {
                mainApp.duplicateTransaction(selected);
            }
        });

        contextMenu.getItems().addAll(editItem, new SeparatorMenuItem(), deleteItem, duplicateItem);
        incomeTableView.setContextMenu(contextMenu);
    }

    /**
     * Makes right-clicking on a row select it.
     */
    private void setupRowFactory() {
        incomeTableView.setRowFactory(tv -> {
            TableRow<Transaction> row = new TableRow<>();
            row.setOnMousePressed(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    Transaction selected = row.getItem();
                    if(mainApp != null) mainApp.editTransaction(selected);
                } else if (!row.isEmpty() && event.getButton() == MouseButton.SECONDARY) {
                    incomeTableView.getSelectionModel().select(row.getIndex());
                }
            });
            return row;
        });
    }

    private void setupColumns() {
        // Title Column (Editable)
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setCellFactory(TextFieldTableCell.forTableColumn());
        titleCol.setOnEditCommit(event -> {
            Transaction transaction = event.getRowValue();
            transaction.setTitle(event.getNewValue());
            saveAndRefresh();
        });
        titleCol.setCellFactory(createAlignedCellFactory(Pos.CENTER_LEFT));


        // Category Column (Editable)
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setCellFactory(ComboBoxTableCell.forTableColumn(incomeCategories));
        categoryCol.setOnEditCommit(event -> {
            Transaction transaction = event.getRowValue();
            transaction.setCategory(event.getNewValue());
            saveAndRefresh();
        });
        categoryCol.setCellFactory(createAlignedComboBoxCellFactory(Pos.CENTER_LEFT, incomeCategories));

        // Date Column (Editable)
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setCellFactory(createDateCellFactory(Pos.CENTER_LEFT));
        dateCol.setOnEditCommit(event -> {
            Transaction transaction = event.getRowValue();
            transaction.setDate(event.getNewValue());
            saveAndRefresh();
        });


        // Amount Column (Editable)
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setCellFactory(column -> new TextFieldTableCell<Transaction, Double>(new DoubleStringConverter()) {
            @Override
            public void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText("+" + CurrencyUtil.formatCurrency(item));
                    getStyleClass().removeAll("transaction-amount-out");
                    getStyleClass().add("transaction-amount-in");
                    setAlignment(Pos.CENTER_RIGHT); // Set alignment
                }
            }
        });
        amountCol.setOnEditCommit(event -> {
            Transaction transaction = event.getRowValue();
            transaction.setAmount(event.getNewValue());
            saveAndRefresh();
        });
    }

    // --- NEW HELPER METHODS FOR ALIGNED & EDITABLE CELLS ---

    private Callback<TableColumn<Transaction, String>, TableCell<Transaction, String>> createAlignedCellFactory(Pos alignment) {
        return tc -> {
            TextFieldTableCell<Transaction, String> cell = new TextFieldTableCell<>();
            cell.setAlignment(alignment);
            return cell;
        };
    }

    private Callback<TableColumn<Transaction, String>, TableCell<Transaction, String>> createAlignedComboBoxCellFactory(Pos alignment, ObservableList<String> items) {
        return tc -> {
            ComboBoxTableCell<Transaction, String> cell = new ComboBoxTableCell<>(items);
            cell.setAlignment(alignment);
            return cell;
        };
    }

    private Callback<TableColumn<Transaction, LocalDate>, TableCell<Transaction, LocalDate>> createDateCellFactory(Pos alignment) {
        return column -> new DateEditingCell(alignment);
    }

    private void setupForm() {
        if (categoryComboBox != null) {
            categoryComboBox.setItems(incomeCategories);
        }

        if (dateField != null) {
            dateField.setText(LocalDate.now().format(DATE_FORMATTER));
        }

        if (addButton != null && addButton.getOnAction() == null) {
            addButton.setOnAction(e -> handleAddTransaction());
        }
    }

    @FXML
    private void handleAddTransaction() {
        try {
            String title = transactionTitleField.getText().trim();
            String category = categoryComboBox.getValue();
            String amountText = amountField.getText().trim();
            String dateText = dateField.getText().trim();

            if (title.isEmpty() || category == null || amountText.isEmpty() || dateText.isEmpty()) {
                showAlert("Missing Information", "Please fill in all fields.");
                return;
            }

            double amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                showAlert("Invalid Amount", "Please enter a positive amount.");
                return;
            }

            LocalDate date = LocalDate.parse(dateText, DATE_FORMATTER);
            Transaction t = new Transaction(title, category, "Income", amount, date);

            DataStore.getInstance().addTransaction(t);
            saveAndRefresh();

            if (transactionTitleField != null) transactionTitleField.clear();
            if (categoryComboBox != null) categoryComboBox.setValue(null);
            if (amountField != null) amountField.clear();
            if (dateField != null) dateField.setText(LocalDate.now().format(DATE_FORMATTER));

            if (incomeTableView != null) {
                incomeTableView.getSelectionModel().select(t);
                incomeTableView.scrollTo(t);
            }

            showInfo("Success", "Income added and saved.");
        } catch (NumberFormatException nfe) {
            showAlert("Invalid Amount", "Amount must be a number.");
        } catch (Exception e) {
            showAlert("Error", "Failed to add transaction: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveAndRefresh() {
        DataStore.getInstance().save();
        updateTotals();
        if (incomeTableView != null) {
            incomeTableView.refresh();
        }
    }

    private void updateTotals() {
        double totalIncome = 0, totalExpenses = 0;
        for (Transaction tx : DataStore.getInstance().getTransactions()) {
            if ("Income".equalsIgnoreCase(tx.getType())) totalIncome += tx.getAmount();
            else if ("Expense".equalsIgnoreCase(tx.getType())) totalExpenses += tx.getAmount();
        }

        double currentBudget = BudgetManager.getMonthlyBudget();
        double left = currentBudget - totalExpenses;

        if (leftToSpendLabel != null) leftToSpendLabel.setText(CurrencyUtil.formatCurrency(left));
        if (monthlyBudgetLabel != null) monthlyBudgetLabel.setText(CurrencyUtil.formatCurrency(currentBudget));

        if (totalIncomeLabel != null) totalIncomeLabel.setText(CurrencyUtil.formatCurrency(totalIncome));
        if (totalExpensesLabel != null) totalExpensesLabel.setText(CurrencyUtil.formatCurrency(totalExpenses));
        if (networkStatusLabel != null) networkStatusLabel.setText("🟢 FINQUEST Connected");

        if (budgetPieChart != null) {
            double remaining = (left > 0) ? left : 0.0;
            double spent = totalExpenses;
            PieChart.Data spentSlice = new PieChart.Data("Spent", spent);
            PieChart.Data remainingSlice = new PieChart.Data("Remaining", remaining);
            budgetPieChart.setData(FXCollections.observableArrayList(spentSlice, remainingSlice));

            spentSlice.getNode().setStyle("-fx-pie-color: #E74C3C;");
            remainingSlice.getNode().setStyle("-fx-pie-color: #2ECC71;"); // Use new green

            double total = spent + remaining;
            if (total == 0) {
                spentSlice.setName("Spent (0.0%)");
                remainingSlice.setName("Remaining (0.0%)");
            } else {
                spentSlice.setName(String.format("Spent (%.1f%%)", (spent / total) * 100));
                remainingSlice.setName(String.format("Remaining (%.1f%%)", (remaining / total) * 100));
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }

    public void dispose() {
        try {
            CurrencyUtil.removeListener(currencyListener);
            BudgetManager.removeListener(budgetListener);
        } catch (Exception ignored) {}
    }

    // --- NEW: Called when the root VBox is clicked. ---
    @FXML
    private void clearSelection(MouseEvent event) {
        if (incomeTableView != null) {
            incomeTableView.getSelectionModel().clearSelection();
        }
    }

    // --- NEW: Called when interactive elements are clicked. ---
    @FXML
    private void consumeClick(MouseEvent event) {
        // This stops the click from bubbling up to the root VBox
        event.consume();
    }
}