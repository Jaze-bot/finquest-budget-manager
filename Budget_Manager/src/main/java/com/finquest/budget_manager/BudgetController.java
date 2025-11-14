//Esguerra
package com.finquest.budget_manager;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class BudgetController implements Initializable {

    @FXML private Label leftToSpendLabel;
    @FXML private Label monthlyBudgetLabel;
    @FXML private TableView<Transaction> transactionsTable;
    @FXML private TextField transactionTitleField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextField amountField;
    @FXML private TextField dateField;

    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (transactionsTable != null) {
            transactionsTable.setItems(DataStore.getInstance().getTransactions());
        }

        if (categoryComboBox != null) {
            categoryComboBox.setItems(FXCollections.observableArrayList(
                    "Food & Dining", "Entertainment", "Shopping", "Income", "Other"
            ));
        }
        if (typeComboBox != null) {
            typeComboBox.setItems(FXCollections.observableArrayList("Income", "Expense"));
        }

        if (dateField != null) {
            dateField.setText(LocalDate.now().format(DATE_FORMATTER));
        }

        updateLabels();
    }

    private void updateLabels() {
        double totalExpenses = 0;
        for(Transaction tx : DataStore.getInstance().getTransactions()) {
            if (!"Income".equalsIgnoreCase(tx.getType())) {
                totalExpenses += tx.getAmount();
            }
        }
        double budget = 2000.00; // Placeholder
        double left = budget - totalExpenses;

        if (leftToSpendLabel != null) leftToSpendLabel.setText(CurrencyUtil.formatCurrency(left));
        if (monthlyBudgetLabel != null) monthlyBudgetLabel.setText(CurrencyUtil.formatCurrency(budget));
    }

    @FXML
    private void handleAddTransaction() {
        System.out.println("Add transaction from budget-view (not fully implemented)");
    }

    @FXML private void handleHome() { System.out.println("Home clicked"); }
    @FXML private void handleExpenses() { System.out.println("Expenses clicked"); }
    @FXML private void handleIncome() { System.out.println("Income clicked"); }
    @FXML private void handleTransactions() { System.out.println("Transactions clicked"); }
    @FXML private void handleSummary() { System.out.println("Summary clicked"); }
}