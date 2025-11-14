//Esguerra
package com.finquest.budget_manager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent; // <-- NEW IMPORT

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.function.Predicate;

public class ReportsController implements Initializable {

    // --- Key Metric Cards ---
    @FXML private Label totalIncomeLabel;
    @FXML private Label totalExpensesLabel;
    @FXML private Label netSavingsLabel;

    // --- Filters ---
    @FXML private ComboBox<String> transactionTypeFilter;

    // --- Charts ---
    @FXML private PieChart expenseBreakdownChart;
    @FXML private PieChart incomeBreakdownChart;
    @FXML private BarChart<String, Number> monthlyBarChart;

    // --- Table ---
    @FXML private TableView<Transaction> transactionReportTable;
    @FXML private TableColumn<Transaction, String> titleCol;
    @FXML private TableColumn<Transaction, String> categoryCol;
    @FXML private TableColumn<Transaction, LocalDate> dateCol;
    @FXML private TableColumn<Transaction, Double> amountCol;

    private BudgetApplication mainApp;

    // Chart data series
    private XYChart.Series<String, Number> incomeSeries;
    private XYChart.Series<String, Number> expenseSeries;

    // Full data list from DataStore
    private final ObservableList<Transaction> allTransactions = DataStore.getInstance().getTransactions();
    // Filtered list that backs all UI elements
    private FilteredList<Transaction> filteredData;

    private final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    /**
     * Public setter to inject the main application instance
     */
    public void setMainApplication(BudgetApplication app) {
        this.mainApp = app;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Initialize Filter ComboBox
        transactionTypeFilter.setItems(FXCollections.observableArrayList("All Transactions", "Income", "Expense"));
        transactionTypeFilter.setValue("All Transactions");

        // 2. Wrap the master data list in a FilteredList
        filteredData = new FilteredList<>(allTransactions, p -> true);

        // 3. Set up listener on the filter ComboBox
        transactionTypeFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());

        // 4. Set up the main TableView
        setupTable();

        // --- NEW: Add context menu and row factory ---
        setupContextMenu();
        setupRowFactory();

        // 5. Set up the charts
        setupCharts();

        // 6. Run the initial data load to populate everything
        loadReportData();
    }

    /**
     * NEW: Sets up the right-click context menu for the table.
     */
    private void setupContextMenu() {
        if (transactionReportTable == null) return;
        ContextMenu contextMenu = new ContextMenu();

        MenuItem editItem = new MenuItem("✏️ Edit Transaction");
        editItem.setOnAction(e -> {
            Transaction selected = transactionReportTable.getSelectionModel().getSelectedItem();
            if (selected != null && mainApp != null) {
                mainApp.editTransaction(selected);
            }
        });

        MenuItem deleteItem = new MenuItem("🗑️ Delete Transaction");
        deleteItem.setOnAction(e -> {
            Transaction selected = transactionReportTable.getSelectionModel().getSelectedItem();
            if (selected != null && mainApp != null) {
                mainApp.deleteTransaction(selected);
            }
        });

        MenuItem duplicateItem = new MenuItem("📋 Duplicate Transaction");
        duplicateItem.setOnAction(e -> {
            Transaction selected = transactionReportTable.getSelectionModel().getSelectedItem();
            if (selected != null && mainApp != null) {
                mainApp.duplicateTransaction(selected);
            }
        });

        contextMenu.getItems().addAll(editItem, new SeparatorMenuItem(), deleteItem, duplicateItem);
        transactionReportTable.setContextMenu(contextMenu);
    }

    /**
     * NEW: Makes right-clicking on a row select it.
     */
    private void setupRowFactory() {
        transactionReportTable.setRowFactory(tv -> {
            TableRow<Transaction> row = new TableRow<>();
            row.setOnMousePressed(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    Transaction selected = row.getItem();
                    if(mainApp != null) mainApp.editTransaction(selected);
                } else if (!row.isEmpty() && event.getButton() == MouseButton.SECONDARY) {
                    transactionReportTable.getSelectionModel().select(row.getIndex());
                }
            });
            return row;
        });
    }

    /**
     * Sets up the columns for the main data table
     */
    private void setupTable() {
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));

        // --- FIX: Apply alignment to all cells ---

        titleCol.setCellFactory(column -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        categoryCol.setCellFactory(column -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                setAlignment(Pos.CENTER_LEFT);
            }
        });

        // Format Date Column
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

        // Format Amount Column
        amountCol.setCellFactory(column -> new TableCell<Transaction, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().removeAll("transaction-amount-in", "transaction-amount-out");
                } else {
                    setText(CurrencyUtil.formatCurrency(item));
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        Transaction tx = getTableRow().getItem();
                        if ("Income".equalsIgnoreCase(tx.getType())) {
                            setText("+" + CurrencyUtil.formatCurrency(item));
                            getStyleClass().add("transaction-amount-in");
                            getStyleClass().removeAll("transaction-amount-out");
                        } else {
                            setText("-" + CurrencyUtil.formatCurrency(item));
                            getStyleClass().add("transaction-amount-out");
                            getStyleClass().removeAll("transaction-amount-in");
                        }
                    }
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        });

        // Bind the filtered data list to the table
        transactionReportTable.setItems(filteredData);
    }

    /**
     * Initializes the data series for the charts
     */
    private void setupCharts() {
        incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("Income");

        expenseSeries = new XYChart.Series<>();
        expenseSeries.setName("Expenses");

        monthlyBarChart.getData().addAll(incomeSeries, expenseSeries);
    }

    /**
     * Called when the filter ComboBox changes value
     */
    private void applyFilter() {
        String filterType = transactionTypeFilter.getValue();

        if (filterType == null || "All Transactions".equals(filterType)) {
            filteredData.setPredicate(p -> true); // Show all
        } else {
            // Show only "Income" or "Expense"
            filteredData.setPredicate(tx -> filterType.equalsIgnoreCase(tx.getType()));
        }

        // After filtering, reload all dashboard metrics and charts
        loadReportData();
    }

    /**
     * Main method to process data and populate all UI elements
     */
    private void loadReportData() {
        Map<String, Double> expenseCategoryData = new HashMap<>();
        Map<String, Double> incomeCategoryData = new HashMap<>();
        Map<String, double[]> monthlyData = new TreeMap<>(); // Use TreeMap to sort months
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        double totalIncome = 0.0;
        double totalExpenses = 0.0;

        // Clear charts before repopulating
        incomeSeries.getData().clear();
        expenseSeries.getData().clear();

        // Iterate through the *filtered* list
        for (Transaction tx : filteredData) {
            String monthKey = tx.getDate().format(monthFormatter);
            double[] monthValues = monthlyData.getOrDefault(monthKey, new double[2]); // [0]=Income, [1]=Expense

            if ("Income".equalsIgnoreCase(tx.getType())) {
                totalIncome += tx.getAmount();
                monthValues[0] += tx.getAmount();
                incomeCategoryData.put(tx.getCategory(), incomeCategoryData.getOrDefault(tx.getCategory(), 0.0) + tx.getAmount());

            } else if ("Expense".equalsIgnoreCase(tx.getType())) {
                totalExpenses += tx.getAmount();
                monthValues[1] += tx.getAmount();
                expenseCategoryData.put(tx.getCategory(), expenseCategoryData.getOrDefault(tx.getCategory(), 0.0) + tx.getAmount());
            }
            monthlyData.put(monthKey, monthValues);
        }

        // Update the top metric cards
        populateKeyMetrics(totalIncome, totalExpenses);

        // Populate the Pie Charts
        populatePieChart(expenseBreakdownChart, expenseCategoryData);
        populatePieChart(incomeBreakdownChart, incomeCategoryData);

        // Populate the Bar Chart
        for (Map.Entry<String, double[]> entry : monthlyData.entrySet()) {
            String month = entry.getKey();
            double income = entry.getValue()[0];
            double expense = entry.getValue()[1];

            incomeSeries.getData().add(new XYChart.Data<>(month, income));
            expenseSeries.getData().add(new XYChart.Data<>(month, expense));
        }
    }

    /**
     * Generic helper method to populate a PieChart
     */
    private void populatePieChart(PieChart chart, Map<String, Double> data) {
        if (chart == null) return;

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }
        chart.setData(pieData);
    }

    /**
     * Updates the 3 big labels at the top
     */
    private void populateKeyMetrics(double totalIncome, double totalExpenses) {
        double netSavings = totalIncome - totalExpenses;

        if (totalIncomeLabel != null) {
            totalIncomeLabel.setText(CurrencyUtil.formatCurrency(totalIncome));
        }
        if (totalExpensesLabel != null) {
            totalExpensesLabel.setText(CurrencyUtil.formatCurrency(totalExpenses));
        }
        if (netSavingsLabel != null) {
            netSavingsLabel.setText(CurrencyUtil.formatCurrency(netSavings));

            netSavingsLabel.getStyleClass().removeAll("transaction-amount-in", "transaction-amount-out");
            if (netSavings >= 0) {
                netSavingsLabel.getStyleClass().add("transaction-amount-in");
            } else {
                netSavingsLabel.getStyleClass().add("transaction-amount-out");
            }
        }
    }

    /**
     * Placeholder action for the export button
     */
    @FXML
    private void handleExportReport() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export Report");
        alert.setHeaderText("Feature Coming Soon!");
        alert.setContentText("This button will one day export your financial report.");
        alert.showAndWait();
    }

    // --- NEW: Called when the root VBox is clicked. ---
    @FXML
    private void clearSelection(MouseEvent event) {
        if (transactionReportTable != null) {
            transactionReportTable.getSelectionModel().clearSelection();
        }
    }

    // --- NEW: Called when interactive elements are clicked. ---
    @FXML
    private void consumeClick(MouseEvent event) {
        // This stops the click from bubbling up to the root VBox
        event.consume();
    }
}