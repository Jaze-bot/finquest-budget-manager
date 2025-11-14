//Chiong
package com.finquest.budget_manager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.PieChart;
import javafx.scene.shape.Circle;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import java.io.*;
import java.net.URL;
import java.time.LocalDate;
import java.util.LinkedHashMap; // Using LinkedHashMap to maintain order
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;
import javafx.geometry.Side;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;

// Imports for all controllers
import com.finquest.budget_manager.SettingsController;
import com.finquest.budget_manager.TransactionsController;
import com.finquest.budget_manager.IncomeController;
import com.finquest.budget_manager.ExpensesController;
import com.finquest.budget_manager.ReportsController;

// Removed Image and ImageView imports


public class BudgetApplication extends Application {

    // FINQUEST Color Theme (Constants for reference)
    private final String FINQUEST_ACCENT = "#2ECC71"; // New Green
    private final String FINQUEST_WARNING = "#E74C3C"; // Red

    // Theming Variables
    private final String LIGHT_BACKGROUND = "#ECF0F1";
    private final String DARK_BACKGROUND = "#1A1A1A";
    private String currentBackground = LIGHT_BACKGROUND;

    // Stylesheet Paths
    private final String MAIN_STYLESHEET = "/com/finquest/budget_manager/styles.css";
    private final String DARK_STYLESHEET = "/com/finquest/budget_manager/dark-theme.css";


    // UI references for dashboard
    private Label leftToSpendLabel;
    private Label monthlyBudgetLabel;
    private Label totalIncomeLabel;
    private Label totalExpensesLabel;

    private ListView<Transaction> transactionsListView;
    private PieChart budgetPieChart;

    private TextField transactionTitleField;
    private ComboBox<String> categoryComboBox;
    private ComboBox<String> typeComboBox;
    private TextField amountField;
    private TextField dateField;
    private Button addButton;

    private ObservableList<Transaction> transactions;
    private double totalExpenses = 0.0;
    private double totalIncome = 0.0;

    // Network simulation
    private boolean networkAvailable = true;
    private Label networkStatusLabel;
    private Label appTitleLabel;

    // Persistence files
    private final String DATA_FILE = "finquest_data.dat";
    private final String SETTINGS_FILE = "finquest_settings.txt";

    // Dashboard container reference (so we can restore it)
    private VBox dashboardMainContent;

    // Scroll and Zoom
    private ScrollPane mainScrollPane;
    private double currentZoomFactor = 1.0;
    private Scene mainScene;

    // Budget Listener instance
    private final BudgetManager.BudgetChangeListener budgetListener = (newBudget) -> {
        Platform.runLater(this::updateBudgetDisplay);
    };

    // Currency Listener (already here)
    private final CurrencyUtil.CurrencyChangeListener currencyListener = (code, sym) -> Platform.runLater(() -> {
        updateBudgetDisplay();
    });


    @Override
    public void start(Stage primaryStage) {
        try {
            // Build UI
            BorderPane root = createRootLayout();

            setupUI();
            loadData();

            // Load theme preference on startup
            String savedTheme = loadSettings(); // This no longer loads the budget

            setupAllEventHandlers(primaryStage);
            updateBudgetDisplay(); // This will pull the budget from BudgetManager

            // Add BOTH listeners
            CurrencyUtil.addListener(currencyListener);
            BudgetManager.addListener(budgetListener);

            // Setup Scene and apply theme
            mainScene = new Scene(root, 1000, 700);
            applyThemeToScene(savedTheme);

            setupKeyboardEvents(mainScene);

            primaryStage.setTitle("FINQUEST - Smart Budget Manager");
            primaryStage.setScene(mainScene);
            setupWindowEvents(primaryStage);
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(650);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            createFallbackUI(primaryStage);
        }
    }

    // PUBLIC METHODS FOR CONTROLLERS

    /**
     * Applies the selected theme (Light/Dark) to the entire application scene.
     */
    public void applyThemeToScene(String theme) {
        if (mainScene == null) return;

        mainScene.getStylesheets().clear();

        URL mainCssUrl = getClass().getResource(MAIN_STYLESHEET);
        if (mainCssUrl != null) {
            mainScene.getStylesheets().add(mainCssUrl.toExternalForm());
        } else {
            System.err.println("CRITICAL ERROR: Main stylesheet not found: " + MAIN_STYLESHEET);
            return;
        }

        if ("Dark".equalsIgnoreCase(theme)) {
            URL darkCssUrl = getClass().getResource(DARK_STYLESHEET);
            if (darkCssUrl != null) {
                mainScene.getStylesheets().add(darkCssUrl.toExternalForm());
                currentBackground = DARK_BACKGROUND;
            } else {
                System.err.println("Warning: Dark theme stylesheet not found: " + DARK_STYLESHEET);
                currentBackground = LIGHT_BACKGROUND;
            }
        } else {
            currentBackground = LIGHT_BACKGROUND;
        }

        // Update the donut chart hole color to match the new background
        Node donutHoleNode = mainScene.lookup("#donutHole");
        if (donutHoleNode instanceof Circle) {
            ((Circle) donutHoleNode).setFill(Paint.valueOf(currentBackground));
        }

        // Update dashboard background and ListViews
        if (dashboardMainContent != null) {
            dashboardMainContent.setStyle("-fx-background-color: " + currentBackground + ";");
        }
        if (transactionsListView != null) {
            transactionsListView.refresh();
        }

        System.out.println("FINQUEST: Theme switched to " + theme);
    }

    /**
     * Gets the current monthly budget (for SettingsController to read).
     */
    public double getMonthlyBudget() {
        return BudgetManager.getMonthlyBudget();
    }

    /**
     * Sets the new monthly budget (called by SettingsController) and updates UI.
     */
    public void setMonthlyBudget(double newBudget) {
        // This method is now the central point for updating the budget from settings
        BudgetManager.setMonthlyBudget(newBudget);

        // We must also update the labels on other FXML tabs
        if (mainScene != null) {
            updateLabelText(mainScene, "#monthlyBudgetLabel", CurrencyUtil.formatCurrency(newBudget));
        }
    }

    // =================================================================
    // SETTINGS / CURRENCY LOADING
    // =================================================================

    // Returns the theme string for application startup
    private String loadSettings() {
        String savedTheme = "Light";
        try {
            File file = new File(SETTINGS_FILE);
            if (!file.exists()) return savedTheme;

            // Budget is loaded by BudgetManager, we only need theme
            List<String> lines = java.nio.file.Files.readAllLines(file.toPath());

            for (String line : lines) {
                if (line.startsWith("THEME=")) {
                    savedTheme = line.substring("THEME=".length()).trim();
                    break; // Found what we need
                }
            }
        } catch (Exception ignored) {}
        return savedTheme;
    }

    private void saveSettings() {
        try {
            java.util.Map<String, String> map = new java.util.HashMap<>();
            java.nio.file.Path p = java.nio.file.Path.of(SETTINGS_FILE);
            if (java.nio.file.Files.exists(p)) {
                for (String line : java.nio.file.Files.readAllLines(p)) {
                    if (line == null || line.isBlank()) continue;
                    int idx = line.indexOf('=');
                    if (idx > 0) map.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
                }
            }

            // All other settings are saved by their respective managers
            // This method is now just a placeholder for other non-managed settings.

            try (PrintWriter pw = new PrintWriter(new FileWriter(SETTINGS_FILE))) {
                map.forEach((k, v) -> pw.println(k + "=" + v));
            }
        } catch (Exception ignored) {}
    }

    // =================================================================
    // DATA PERSISTENCE
    // =================================================================

    @SuppressWarnings("unchecked")
    private void loadData() {
        try {
            File file = new File(DATA_FILE);
            if (file.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                java.util.List<Transaction> saved = (java.util.List<Transaction>) ois.readObject();
                DataStore.getInstance().getTransactions().setAll(saved);
                ois.close();
                System.out.println("FINQUEST: Data loaded from " + DATA_FILE);
            } else {
                setupPlaceholderData();
            }
        } catch (Exception e) {
            System.out.println("FINQUEST: Could not load data, using placeholder data");
            setupPlaceholderData();
        }
    }

    public void saveData() {
        try {
            DataStore.getInstance().save();
            saveSettings(); // Saves any other persistent settings
            System.out.println("FINQUEST: Data saved to " + DATA_FILE);
            showTemporaryNotification("FINQUEST Data Saved Successfully! 💾");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Save Error", "Could not save data: " + e.getMessage());
        }
    }

    // =================================================================
    // EVENT HANDLERS
    // =================================================================

    private void setupAllEventHandlers(Stage primaryStage) {
        setupMouseEvents();
        setupNetworkEvents();
    }

    private void setupMouseEvents() {
        if (transactionsListView != null) {
            transactionsListView.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2) {
                    Transaction selected = transactionsListView.getSelectionModel().getSelectedItem();
                    if (selected != null) editTransaction(selected);
                }
            });
            setupContextMenu();
        }
        setupBudgetEditing();
    }

    private void setupContextMenu() {
        if (transactionsListView == null) return;
        ContextMenu contextMenu = new ContextMenu();

        MenuItem editItem = new MenuItem("✏️ Edit Transaction");
        editItem.setOnAction(e -> {
            Transaction selected = transactionsListView.getSelectionModel().getSelectedItem();
            if (selected != null) editTransaction(selected);
        });

        MenuItem deleteItem = new MenuItem("🗑️ Delete Transaction");
        deleteItem.setOnAction(e -> {
            Transaction selected = transactionsListView.getSelectionModel().getSelectedItem();
            if (selected != null) deleteTransaction(selected);
        });

        MenuItem duplicateItem = new MenuItem("📋 Duplicate Transaction");
        duplicateItem.setOnAction(e -> {
            Transaction selected = transactionsListView.getSelectionModel().getSelectedItem();
            if (selected != null) duplicateTransaction(selected);
        });

        contextMenu.getItems().addAll(editItem, deleteItem, duplicateItem);
        transactionsListView.setContextMenu(contextMenu);
    }

    private void setupBudgetEditing() {
        if (monthlyBudgetLabel != null) {
            Node budgetCard = monthlyBudgetLabel.getParent();
            if (budgetCard != null) {
                budgetCard = budgetCard.getParent();
            }
            if (budgetCard != null) {
                budgetCard.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2) editMonthlyBudget();
                });
                budgetCard.getStyleClass().add("clickable-card"); // --- CSS ---
            }
        }
    }

    private void setupNetworkEvents() {
        checkNetworkStatus(); // This is example 1 (Simulated)
        // Example 2 (Real) is now built in createProTipCard()
    }

    private void checkNetworkStatus() {
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                Platform.runLater(() -> {
                    networkAvailable = Math.random() > 0.3;
                    updateNetworkStatus();
                });
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void setupKeyboardEvents(Scene scene) {
        if (scene == null) return;
        KeyCombination saveCombination = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
        scene.getAccelerators().put(saveCombination, this::saveData);

        KeyCombination zoomIn = new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN);
        KeyCombination zoomInNumpad = new KeyCodeCombination(KeyCode.ADD, KeyCombination.CONTROL_DOWN);
        KeyCombination zoomOut = new KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN);
        KeyCombination zoomOutNumpad = new KeyCodeCombination(KeyCode.SUBTRACT, KeyCombination.CONTROL_DOWN);
        KeyCombination zoomReset = new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN);
        KeyCombination zoomResetNumpad = new KeyCodeCombination(KeyCode.NUMPAD0, KeyCombination.CONTROL_DOWN);

        scene.getAccelerators().put(zoomIn, () -> zoom(0.1));
        scene.getAccelerators().put(zoomInNumpad, () -> zoom(0.1));
        scene.getAccelerators().put(zoomOut, () -> zoom(-0.1));
        scene.getAccelerators().put(zoomOutNumpad, () -> zoom(-0.1));
        scene.getAccelerators().put(zoomReset, () -> zoom(0.0));
        scene.getAccelerators().put(zoomResetNumpad, () -> zoom(0.0));

        scene.setOnScroll(event -> {
            if (event.isControlDown()) {
                event.consume();
                double delta = event.getDeltaY() > 0 ? 0.05 : -0.05;
                zoom(delta);
            }
        });

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && addButton != null && addButton.isFocused()) handleAddTransaction();
            if (e.getCode() == KeyCode.ESCAPE) clearForm();
            if (e.getCode() == KeyCode.DELETE) {
                Transaction sel = transactionsListView.getSelectionModel().getSelectedItem();
                if (sel != null) deleteTransaction(sel);
            }
        });
    }

    private void zoom(double delta) {
        if (delta == 0.0) {
            currentZoomFactor = 1.0;
        } else {
            currentZoomFactor += delta;
            if (currentZoomFactor < 0.5) currentZoomFactor = 0.5;
            if (currentZoomFactor > 2.0) currentZoomFactor = 2.0;
        }

        if (mainScrollPane != null) {
            mainScrollPane.setScaleX(currentZoomFactor);
            mainScrollPane.setScaleY(currentZoomFactor);
        }

        showTemporaryNotification(String.format("Zoom: %d%%", (int)(currentZoomFactor * 100)));
    }


    private void setupWindowEvents(Stage primaryStage) {
        if (primaryStage == null) return;
        primaryStage.setOnCloseRequest(e -> {
            if (!confirmExit()) e.consume();
        });
        primaryStage.setOnShown(e -> showTemporaryNotification("FINQUEST Ready - Welcome back!"));
        primaryStage.setOnHidden(e -> {
            saveData();
            CurrencyUtil.removeListener(currencyListener);
            BudgetManager.removeListener(budgetListener);
            System.out.println("FINQUEST closed");
        });
    }

    // =================================================================
    // ===== UI CREATION (DASHBOARD) ===================================
    // =================================================================

    private BorderPane createRootLayout() {
        BorderPane root = new BorderPane();
        VBox sidebar = createSidebar();
        root.setLeft(sidebar);

        this.dashboardMainContent = createMainContent();

        mainScrollPane = new ScrollPane(dashboardMainContent);
        mainScrollPane.setFitToWidth(true);
        mainScrollPane.setFitToHeight(false);

        mainScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        root.setCenter(mainScrollPane);

        return root;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(20);
        sidebar.setPadding(new Insets(25, 20, 20, 20));
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(200);

        VBox logoSection = new VBox(10);
        logoSection.setAlignment(Pos.CENTER_LEFT);
        appTitleLabel = new Label("FINQUEST");
        appTitleLabel.getStyleClass().add("sidebar-title");
        Label subtitleLabel = new Label("Budget Manager");
        subtitleLabel.getStyleClass().add("sidebar-subtitle");

        logoSection.getChildren().addAll(appTitleLabel, subtitleLabel);
        VBox menu = new VBox(8);

        // --- Map of menu items to their SVG icon paths ---
        Map<String, String> menuItems = new LinkedHashMap<>();
        menuItems.put("Dashboard", "M3 3h8v8H3zM13 3h8v8h-8zM3 13h8v8H3zM13 13h8v8h-8z"); // Grid
        menuItems.put("Expenses", "M12 2a10 10 0 1 0 0 20 10 10 0 1 0 0-20zM7 11h10v2H7z"); // Minus Circle
        menuItems.put("Income", "M12 2a10 10 0 1 0 0 20 10 10 0 1 0 0-20zM11 7h2v4h4v2h-4v4h-2v-4H7v-2h4z"); // Plus Circle
        menuItems.put("Transactions", "M3 5h18v2H3zM3 11h18v2H3zM3 17h18v2H3z"); // List
        menuItems.put("Reports", "M3 7h4v14H3zM9 11h4v10H9zM15 4h4v17h-4zM21 14h4v7h-4z"); // Bar Chart
        menuItems.put("Settings", "M19.4 12.9c.1-.3.1-.6.1-.9s0-.6-.1-.9l2.1-1.6c.2-.1.2-.4.1-.6l-2-3.5c-.1-.2-.4-.2-.6-.1l-2.5 1c-.5-.4-1.1-.7-1.7-.9l-.4-2.7C14.2 2.2 14 2 13.7 2h-3.4c-.3 0-.5.2-.5.5l-.4 2.7c-.6.2-1.2.5-1.7.9l-2.5-1c-.2-.1-.5 0-.6.1l-2 3.5c-.1.2 0 .5.1.6l2.1 1.6c0 .3-.1.6-.1.9s0 .6.1.9l-2.1 1.6c-.2.1-.2.4-.1.6l2 3.5c.1.2.4.2.6.1l2.5-1c.5.4 1.1.7 1.7.9l.4 2.7c0 .3.2.5.5.5h3.4c.3 0 .5-.2.5.5l.4-2.7c.6-.2 1.2-.5 1.7-.9l2.5 1c.2.1.5 0 .6-.1l2-3.5c.1-.2 0-.5-.1-.6l-2.1-1.6zM12 15.5c-1.9 0-3.5-1.6-3.5-3.5s1.6-3.5 3.5-3.5 3.5 1.6 3.5 3.5-1.6 3.5-3.5 3.5z"); // Gear

        for (Map.Entry<String, String> item : menuItems.entrySet()) {
            menu.getChildren().add(createMenuButton(item.getKey(), item.getValue()));
        }

        sidebar.getChildren().addAll(logoSection, menu);
        VBox.setVgrow(menu, Priority.ALWAYS);
        return sidebar;
    }

    private Button createMenuButton(String text, String svgPath) {
        Button button = new Button(text);

        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        icon.getStyleClass().add("sidebar-icon"); // Style for fill color

        button.setGraphic(icon);
        button.setGraphicTextGap(10);
        button.getStyleClass().add("sidebar-button");
        button.setOnAction(e -> handleMenuAction(e, text));
        return button;
    }

    // --- THIS IS THE UPDATED METHOD ---
    private void handleMenuAction(ActionEvent event, String itemText) {
        try {
            String fxmlPath = null;
            Parent viewToLoad = null;

            switch (itemText) {
                case "Expenses":
                    fxmlPath = "/com/finquest/budget_manager/expenses-veiw.fxml";
                    break;
                case "Income":
                    fxmlPath = "/com/finquest/budget_manager/income-view.fxml";
                    break;
                case "Settings":
                    fxmlPath = "/com/finquest/budget_manager/settings-view.fxml";
                    break;
                case "Reports":
                    fxmlPath = "/com/finquest/budget_manager/reports-view.fxml";
                    break;
                case "Transactions":
                    fxmlPath = "/com/finquest/budget_manager/transactions-view.fxml";
                    break;
                case "Dashboard":
                    viewToLoad = dashboardMainContent;
                    break;
                default:
                    System.out.println("Menu clicked: " + itemText);
                    return;
            }

            if (fxmlPath != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                viewToLoad = loader.load();
                Object controller = loader.getController();

                // Pass the main application instance to any controller that needs it.
                if (controller instanceof SettingsController) {
                    ((SettingsController) controller).setMainApplication(this);
                } else if (controller instanceof TransactionsController) {
                    ((TransactionsController) controller).setMainApplication(this);
                } else if (controller instanceof IncomeController) {
                    ((IncomeController) controller).setMainApplication(this);
                } else if (controller instanceof ExpensesController) {
                    ((ExpensesController) controller).setMainApplication(this);
                } else if (controller instanceof ReportsController) {
                    ((ReportsController) controller).setMainApplication(this);
                }
            }

            if (viewToLoad != null) {
                mainScrollPane.setContent(viewToLoad);
                mainScrollPane.setVvalue(0.0);
                mainScrollPane.setHvalue(0.0);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Navigation Error", "Failed to load view for " + itemText + ":\n" + ex.getMessage());
        }
    }

    private Parent loadView(String... resourcePaths) throws Exception {
        for (String path : resourcePaths) {
            if (path == null) continue;
            URL url = getClass().getResource(path);
            if (url != null) return FXMLLoader.load(url);
        }
        throw new FileNotFoundException("None of the resources exist: " + String.join(", ", resourcePaths));
    }

    private VBox createMainContent() {
        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(25));
        mainContent.getStyleClass().add("main-view");
        mainContent.setStyle("-fx-background-color: " + currentBackground + ";"); // Set initial background

        // --- NEW: Add click listener to clear selection ---
        mainContent.setOnMousePressed(e -> {
            if (transactionsListView != null) {
                Node target = (Node) e.getTarget();
                if (!isNodeInside(target, transactionsListView)) {
                    transactionsListView.getSelectionModel().clearSelection();
                }
            }
        });

        HBox header = createHeader();
        HBox columnsContainer = new HBox(20);
        VBox.setVgrow(columnsContainer, Priority.ALWAYS);
        VBox leftColumn = createLeftColumn();
        VBox rightColumn = createRightColumn();
        HBox.setHgrow(leftColumn, Priority.ALWAYS);
        rightColumn.setMinWidth(350);
        rightColumn.setMaxWidth(350);
        columnsContainer.getChildren().addAll(leftColumn, rightColumn);
        mainContent.getChildren().addAll(header, columnsContainer);
        return mainContent;
    }

    // --- NEW: Helper method for click-off logic ---
    private boolean isNodeInside(Node node, Node parent) {
        if (node == null) return false;
        if (node == parent) return true;
        return isNodeInside(node.getParent(), parent);
    }

    private VBox createLeftColumn() {
        VBox leftColumn = new VBox(20);
        VBox transactionsSection = createTransactionsSection();
        VBox.setVgrow(transactionsSection, Priority.ALWAYS);
        leftColumn.getChildren().add(transactionsSection);
        return leftColumn;
    }

    private VBox createRightColumn() {
        VBox rightColumn = new VBox(20);
        VBox statCardsVBox = new VBox(15);

        leftToSpendLabel = createAmountLabel();
        monthlyBudgetLabel = createAmountLabel();
        totalIncomeLabel = createAmountLabel();
        totalExpensesLabel = createAmountLabel();

        leftToSpendLabel.getStyleClass().add("transaction-amount-in");
        totalExpensesLabel.getStyleClass().add("transaction-amount-out");

        statCardsVBox.getChildren().addAll(
                createStatCard("Left to Spend", "💰", leftToSpendLabel),
                createStatCard("Monthly Budget", "📅", monthlyBudgetLabel),
                createStatCard("Total Income", "📈", totalIncomeLabel),
                createStatCard("Total Expenses", "📉", totalExpensesLabel)
        );

        VBox addTransactionSection = createAddTransactionSection();
        VBox donutCard = createDonutChartPane();

        // --- NEW: Create and add the Pro Tip card ---
        VBox tipCard = createProTipCard();

        rightColumn.getChildren().addAll(donutCard, statCardsVBox, tipCard, addTransactionSection);
        return rightColumn;
    }

    /**
     * --- MODIFIED: Replaced failing network card with a static pro-tip card ---
     */
    private VBox createProTipCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("budget-card");

        Label title = new Label("💡 Pro Tip");
        title.getStyleClass().add("section-title");

        Label tipText = new Label("Double-click on any transaction in your lists or tables to edit it quickly.");
        tipText.getStyleClass().add("card-label");
        tipText.setWrapText(true);

        card.getChildren().addAll(title, tipText);
        return card;
    }

    private VBox createDonutChartPane() {
        VBox card = new VBox(10);
        card.getStyleClass().add("budget-card");
        card.setAlignment(Pos.CENTER);

        Label title = new Label("Budget Status");
        title.getStyleClass().add("section-title");

        budgetPieChart = new PieChart();
        budgetPieChart.setPrefHeight(160);
        budgetPieChart.setPrefWidth(160);
        budgetPieChart.setLabelsVisible(false);
        budgetPieChart.setLegendVisible(false);
        budgetPieChart.setTitleSide(Side.BOTTOM);

        Circle donutHole = new Circle(40);
        donutHole.setId("donutHole"); // ID for CSS and lookup
        donutHole.getStyleClass().add("chart-pane-hole"); // --- CSS ---
        donutHole.setFill(Paint.valueOf(currentBackground));
        donutHole.setMouseTransparent(true);

        StackPane donutChartPane = new StackPane(budgetPieChart, donutHole);
        donutChartPane.setAlignment(Pos.CENTER);

        donutChartPane.setOnMouseEntered(e -> budgetPieChart.setLabelsVisible(true));
        donutChartPane.setOnMouseExited(e -> budgetPieChart.setLabelsVisible(false));

        card.getChildren().addAll(title, donutChartPane);
        return card;
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));

        Label titleLabel = new Label("Financial Dashboard");
        titleLabel.getStyleClass().add("header-title");

        networkStatusLabel = new Label("🟢 FINQUEST Connected");
        networkStatusLabel.getStyleClass().add("network-status-label");

        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        header.getChildren().addAll(titleLabel, networkStatusLabel);
        return header;
    }

    // --- MODIFIED: Removed event handlers, CSS now handles hover effects ---
    private HBox createStatCard(String title, String emoji, Label dataLabel) {
        HBox card = new HBox(15);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("budget-card");
        // --- NEW: Add clickable class only to the budget card ---
        if ("Monthly Budget".equals(title)) {
            card.getStyleClass().add("clickable-card");
        }
        card.setAlignment(Pos.CENTER_LEFT);
        Label emojiLabel = new Label(emoji);
        emojiLabel.getStyleClass().add("transaction-icon"); // Use icon style

        VBox textContainer = new VBox(5);
        textContainer.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-label");
        dataLabel.getStyleClass().add("card-amount");
        textContainer.getChildren().addAll(titleLabel, dataLabel);
        card.getChildren().addAll(emojiLabel, textContainer);

        return card;
    }

    private Label createAmountLabel() {
        Label label = new Label(CurrencyUtil.formatCurrency(0.0));
        return label;
    }

    private VBox createTransactionsSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(20));
        section.getStyleClass().add("budget-card");
        VBox.setVgrow(section, Priority.ALWAYS);

        HBox sectionHeader = new HBox();
        sectionHeader.setAlignment(Pos.CENTER_LEFT);
        Label sectionTitle = new Label("Recent Transactions");
        sectionTitle.getStyleClass().add("section-title");
        HBox.setHgrow(sectionTitle, Priority.ALWAYS);
        sectionHeader.getChildren().addAll(sectionTitle);

        transactionsListView = new ListView<>();
        transactionsListView.setId("transactionsListView");
        transactionsListView.getStyleClass().addAll("transaction-history-view", "budget-card");
        VBox.setVgrow(transactionsListView, Priority.ALWAYS);

        setupTransactionsListView();

        section.getChildren().addAll(sectionHeader, transactionsListView);
        return section;
    }

    private void setupTransactionsListView() {
        transactionsListView.setCellFactory(listView -> new ListCell<Transaction>() {
            private final HBox cardLayout = new HBox(15);
            private final Label iconLabel = new Label();
            private final VBox titleDateVBox = new VBox(0);
            private final Label titleLabel = new Label();
            private final Label dateLabel = new Label();
            private final Region spacer = new Region();
            private final Label amountLabel = new Label();

            {
                cardLayout.setAlignment(Pos.CENTER_LEFT);
                cardLayout.getStyleClass().add("transaction-list-card");

                titleLabel.getStyleClass().add("transaction-title");
                dateLabel.getStyleClass().add("transaction-date");
                iconLabel.getStyleClass().add("transaction-icon");

                titleDateVBox.getChildren().addAll(titleLabel, dateLabel);
                HBox.setHgrow(spacer, Priority.ALWAYS);
                cardLayout.getChildren().addAll(iconLabel, titleDateVBox, spacer, amountLabel);
            }

            @Override
            protected void updateItem(Transaction transaction, boolean empty) {
                super.updateItem(transaction, empty);
                if (empty || transaction == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    titleLabel.setText(transaction.getTitle());
                    dateLabel.setText(transaction.getFormattedDate());
                    iconLabel.setText(getEmojiForCategory(transaction.getCategory()));
                    String formattedAmount = CurrencyUtil.formatCurrency(transaction.getAmount());
                    amountLabel.getStyleClass().removeAll("transaction-amount-in", "transaction-amount-out");

                    if ("Income".equalsIgnoreCase(transaction.getType())) {
                        amountLabel.setText("+" + formattedAmount);
                        amountLabel.getStyleClass().add("transaction-amount-in");
                    } else {
                        amountLabel.setText("-" + formattedAmount);
                        amountLabel.getStyleClass().add("transaction-amount-out");
                    }
                    setGraphic(cardLayout);
                    setText(null);
                }
            }
        });
    }

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

    private VBox createAddTransactionSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(25));
        section.getStyleClass().add("form-card");
        Label sectionTitle = new Label("Add New Transaction");
        sectionTitle.getStyleClass().add("section-title");
        VBox form = createTransactionForm();
        section.getChildren().addAll(sectionTitle, form);
        return section;
    }

    private VBox createFormGroup(String labelText, Node field) {
        VBox group = new VBox(5);
        Label label = createFormLabel(labelText);
        group.getChildren().addAll(label, field);
        return group;
    }

    private VBox createTransactionForm() {
        VBox formLayout = new VBox(15);
        HBox row1 = new HBox(15);
        transactionTitleField = createStyledTextField("Enter transaction title");
        categoryComboBox = createStyledComboBox("Select category");
        VBox titleGroup = createFormGroup("Title:", transactionTitleField);
        VBox categoryGroup = createFormGroup("Category:", categoryComboBox);
        HBox.setHgrow(titleGroup, Priority.ALWAYS);
        HBox.setHgrow(categoryGroup, Priority.ALWAYS);
        row1.getChildren().addAll(titleGroup, categoryGroup);
        HBox row2 = new HBox(15);
        typeComboBox = createStyledComboBox("Income/Expense");
        amountField = createStyledTextField("0.00");
        VBox typeGroup = createFormGroup("Type:", typeComboBox);
        VBox amountGroup = createFormGroup("Amount:", amountField);
        HBox.setHgrow(typeGroup, Priority.ALWAYS);
        HBox.setHgrow(amountGroup, Priority.ALWAYS);
        row2.getChildren().addAll(typeGroup, amountGroup);
        HBox row3 = new HBox(15);
        row3.setAlignment(Pos.BOTTOM_LEFT);
        dateField = createStyledTextField("MM/DD/YYYY");
        VBox dateGroup = createFormGroup("Date:", dateField);
        HBox.setHgrow(dateGroup, Priority.ALWAYS);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        addButton = createPrimaryButton("Add");

        addButton.setOnAction(e -> handleAddTransaction());
        row3.getChildren().addAll(dateGroup, spacer, addButton);
        formLayout.getChildren().addAll(row1, row2, row3);
        return formLayout;
    }

    private TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("form-group-field");
        return field;
    }

    private ComboBox<String> createStyledComboBox(String prompt) {
        ComboBox<String> combo = new ComboBox<>();
        combo.setPromptText(prompt);
        combo.getStyleClass().add("form-group-field");
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private Button createPrimaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("add-button");
        return button;
    }

    private Label createFormLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-group-label");
        return label;
    }

    // BUSINESS LOGIC (With updateBudgetDisplay MOD

    private void setupUI() {
        transactions = DataStore.getInstance().getTransactions();
        if (transactionsListView != null) transactionsListView.setItems(transactions);

        if (categoryComboBox != null) categoryComboBox.setItems(FXCollections.observableArrayList(
                "Food & Dining", "Entertainment", "Transportation", "Shopping",
                "Bills & Utilities", "Healthcare", "Education", "Business", "Income", "Other"
        ));
        if (typeComboBox != null) typeComboBox.setItems(FXCollections.observableArrayList("Income", "Expense"));
        if (dateField != null) dateField.setText(LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy")));
    }

    private void setupPlaceholderData() {
        if (DataStore.getInstance().getTransactions().isEmpty()) {
            DataStore.getInstance().getTransactions().add(new Transaction("Monthly Salary", "Income", "Income", 3000.00, LocalDate.now().minusDays(5)));
            DataStore.getInstance().getTransactions().add(new Transaction("Grocery Shopping", "Food & Dining", "Expense", 85.75, LocalDate.now().minusDays(3)));
            DataStore.getInstance().getTransactions().add(new Transaction("Movie Night", "Entertainment", "Expense", 25.50, LocalDate.now().minusDays(1)));
        }
        calculateTotals();
        updateBudgetDisplay();
    }

    private void calculateTotals() {
        totalIncome = 0.0;
        totalExpenses = 0.0;
        for (Transaction tx : DataStore.getInstance().getTransactions()) {
            if (tx.getType() != null && tx.getType().toLowerCase().contains("income")) totalIncome += tx.getAmount();
            else totalExpenses += tx.getAmount();
        }
    }

    /**
     * This is the main refresh method for the Dashboard.
     * It now gets the budget from the BudgetManager.
     */
    public void updateBudgetDisplay() {
        calculateTotals();
        double currentBudget = BudgetManager.getMonthlyBudget();
        double leftToSpend = currentBudget - totalExpenses;

        if (leftToSpendLabel != null) leftToSpendLabel.setText(CurrencyUtil.formatCurrency(leftToSpend));
        if (monthlyBudgetLabel != null) monthlyBudgetLabel.setText(CurrencyUtil.formatCurrency(currentBudget));
        if (totalIncomeLabel != null) totalIncomeLabel.setText(CurrencyUtil.formatCurrency(totalIncome));
        if (totalExpensesLabel != null) totalExpensesLabel.setText(CurrencyUtil.formatCurrency(totalExpenses));

        if (transactionsListView != null) transactionsListView.refresh();

        if (networkStatusLabel != null) updateNetworkStatus(); // Use full method

        if (budgetPieChart != null) {
            double remaining = (leftToSpend > 0) ? leftToSpend : 0.0;
            double spent = totalExpenses;

            PieChart.Data spentSlice = new PieChart.Data("Spent", spent);
            PieChart.Data remainingSlice = new PieChart.Data("Remaining", remaining);
            budgetPieChart.setData(FXCollections.observableArrayList(spentSlice, remainingSlice));

            // These styles are dynamic and should stay in Java
            spentSlice.getNode().setStyle("-fx-pie-color: " + FINQUEST_WARNING + ";");
            remainingSlice.getNode().setStyle("-fx-pie-color: " + FINQUEST_ACCENT + ";");

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

    private void editMonthlyBudget() {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(BudgetManager.getMonthlyBudget()));
        dialog.setTitle("Edit Monthly Budget");
        dialog.setHeaderText("Set Your Monthly Budget");
        dialog.setContentText("Enter new monthly budget amount:");
        dialog.showAndWait().ifPresent(newBudget -> {
            try {
                double budget = Double.parseDouble(newBudget);
                if (budget >= 0) {
                    BudgetManager.setMonthlyBudget(budget); // This triggers all listeners
                    showSuccess("Budget Updated", "Monthly budget set to " + CurrencyUtil.formatCurrency(budget));
                } else showAlert("Invalid Amount", "Please enter a positive number.");
            } catch (NumberFormatException e) {
                showAlert("Invalid Input", "Please enter a valid number.");
            }
        });
    }

    // --- MODIFIED ---: Made public for TransactionsController
    public void editTransaction(Transaction transaction) {
        Dialog<Transaction> dialog = new Dialog<>();
        dialog.setTitle("Edit Transaction");
        dialog.setHeaderText("Edit Transaction Details");
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        TextField titleField = new TextField(transaction.getTitle());
        TextField amountField = new TextField(String.valueOf(transaction.getAmount()));
        ComboBox<String> categoryCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Food & Dining", "Entertainment", "Transportation", "Shopping",
                "Bills & Utilities", "Healthcare", "Education", "Business", "Income", "Other"
        ));
        categoryCombo.setValue(transaction.getCategory());
        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("Income", "Expense"));
        typeCombo.setValue(transaction.getType());
        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Amount:"), 0, 1);
        grid.add(amountField, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(new Label("Type:"), 0, 3);
        grid.add(typeCombo, 1, 3);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    transaction.setTitle(titleField.getText());
                    transaction.setAmount(Double.parseDouble(amountField.getText()));
                    transaction.setCategory(categoryCombo.getValue());
                    transaction.setType(typeCombo.getValue());
                    return transaction;
                } catch (NumberFormatException e) {
                    showAlert("Invalid Amount", "Please enter a valid number for amount.");
                }
            }
            return null;
        });
        dialog.showAndWait().ifPresent(result -> {
            transactionsListView.refresh();
            updateBudgetDisplay();
            saveData();
            showSuccess("Transaction Updated", "Transaction edited successfully!");
        });
    }

    // --- MODIFIED ---: Made public for TransactionsController
    public void deleteTransaction(Transaction transaction) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Transaction");
        alert.setHeaderText("Delete Transaction");
        alert.setContentText("Are you sure you want to delete: " + transaction.getTitle() + "?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                DataStore.getInstance().getTransactions().remove(transaction);
                saveData();
                updateBudgetDisplay();
                showSuccess("Deleted", "Transaction deleted successfully!");
            }
        });
    }

    // --- MODIFIED ---: Made public for TransactionsController
    public void duplicateTransaction(Transaction transaction) {
        Transaction duplicate = new Transaction(
                transaction.getTitle() + " (Copy)",
                transaction.getCategory(),
                transaction.getType(),
                transaction.getAmount(),
                LocalDate.now()
        );
        DataStore.getInstance().addTransaction(duplicate);
        updateBudgetDisplay();
        saveData();
        showSuccess("Duplicated", "Transaction duplicated successfully!");
    }

    private void handleAddTransaction() {
        try {
            String title = transactionTitleField.getText().trim();
            String category = categoryComboBox.getValue();
            String type = typeComboBox.getValue();
            double amount = Double.parseDouble(amountField.getText());
            LocalDate date = LocalDate.parse(dateField.getText(), java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            if (title.isEmpty() || category == null || type == null) {
                showAlert("Missing Information", "Please fill in all fields.");
                return;
            }
            if (amount <= 0) {
                showAlert("Invalid Amount", "Please enter a positive amount.");
                return;
            }
            Transaction newTransaction = new Transaction(title, category, type, amount, date);
            DataStore.getInstance().addTransaction(newTransaction);
            clearForm();
            updateBudgetDisplay();
            saveData();
            showSuccess("Success", "Transaction added to FINQUEST!");
        } catch (Exception e) {
            showAlert("Input Error", "Please check your input format:\n- Amount must be a number\n- Date must be in MM/DD/YYYY format");
        }
    }

    private void clearForm() {
        if (transactionTitleField != null) transactionTitleField.clear();
        if (categoryComboBox != null) categoryComboBox.setValue(null);
        if (typeComboBox != null) typeComboBox.setValue(null);
        if (amountField != null) amountField.clear();
        if (dateField != null) dateField.setText(LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy")));
    }

    private boolean confirmExit() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit FINQUEST");
        alert.setHeaderText("Exit FINQUEST Budget Manager");
        alert.setContentText("Are you sure you want to exit? Your data is automatically saved.");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void updateNetworkStatus() {
        String status = networkAvailable ? "🟢 FINQUEST Connected" : "🔴 Offline Mode";
        String styleClass = networkAvailable ? "network-status-label" : "network-status-label-offline";
        if (networkStatusLabel != null) {
            networkStatusLabel.setText(status);
            networkStatusLabel.getStyleClass().removeAll("network-status-label", "network-status-label-offline");
            networkStatusLabel.getStyleClass().add(styleClass);
        }
    }

    // --- MODIFIED ---: Made public for TransactionsController
    public void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // --- MODIFIED ---: Made public for TransactionsController
    public void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showTemporaryNotification(String message) {
        System.out.println("FINQUEST: " + message);
    }

    private void createFallbackUI(Stage primaryStage) {
        Label label = new Label("FINQUEST Budget Manager\n\nStarting...");
        label.getStyleClass().add("header-title"); // Use CSS
        StackPane root = new StackPane(label);
        root.setStyle("-fx-background-color: " + currentBackground + "; -fx-padding: 20;");
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.setTitle("FINQUEST - Budget Manager");
        primaryStage.show();
    }

    // --- NEW ---: Utility to find and update any label on the scene
    private void updateLabelText(Scene scene, String labelSelector, String newText) {
        try {
            Node node = scene.lookup(labelSelector);
            if (node instanceof Label) {
                ((Label) node).setText(newText);
            }
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        launch(args);
    }
}