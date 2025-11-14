**FINQUEST Project - Task Distribution**

**Justin John Chiong
Role: Core Application & Front-End UI/UX**
BudgetApplication.java (Main application class, window setup, dashboard UI logic)
DateEditingCell.java (Custom JavaFX component for editable dates)
All Front-End Layout & Styling:
budget-view.fxml
expenses-veiw.fxml
income-view.fxml
reports-view.fxml
settings-view.fxml
transactions-view.fxml
styles.css (Main application theme)
dark-theme.css (Dark mode overrides)

**John Timothy Esguerra (Esguerra)
Role: Backend Logic & Feature Controllers**
BudgetManager.java (Singleton for handling budget data)
BudgetController.java (Controller for the old budget view)
CurrencyUtil.java (Singleton for handling currency formatting)
DataStore.java (Singleton for managing the master transaction list)
ExpensesController.java (Controller for the Expenses tab)
IncomeController.java (Controller for the Income tab)
ReportsController.java (Controller for the Reports tab)
SettingsController.java (Controller for the Settings tab)
Transaction.java (The core data model for a transaction)
TransactionsController.java (Controller for the "All Transactions" tab)
