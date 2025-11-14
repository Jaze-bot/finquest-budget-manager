//Esguerra
package com.finquest.budget_manager;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Window;

import java.net.URL;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    @FXML private TextField monthlyBudgetField;
    @FXML private ComboBox<String> currencyComboBox;
    @FXML private Label statusLabel;
    @FXML private Button saveButton;

    // --- UI Elements ---
    @FXML private ToggleButton lightThemeButton;
    @FXML private ToggleButton darkThemeButton;
    private ToggleGroup themeToggleGroup;

    // --- Reference to the main application for theme switching ---
    private BudgetApplication mainApp;

    private static final String SETTINGS_FILE = "finquest_settings.txt";

    /**
     * Public setter to inject the main application instance
     */
    public void setMainApplication(BudgetApplication app) {
        this.mainApp = app;
        // Now that mainApp is set, load the budget from it
        loadSettings();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupCurrencyComboBox();
        setupThemeToggle();

        saveButton.setOnAction(e -> handleSave());

        // Initial setup
        // loadSettings() is now called from setMainApplication
        statusLabel.setText(" ");
    }

    /**
     * Populates the currency ComboBox with options.
     */
    private void setupCurrencyComboBox() {
        currencyComboBox.getItems().addAll(
                "PHP (₱)",
                "USD ($)",
                "EUR (€)",
                "GBP (£)",
                "JPY (¥)"
        );
    }

    /**
     * Sets up the Light/Dark theme toggle buttons
     */
    private void setupThemeToggle() {
        themeToggleGroup = new ToggleGroup();
        if (lightThemeButton != null) lightThemeButton.setToggleGroup(themeToggleGroup);
        if (darkThemeButton != null) darkThemeButton.setToggleGroup(themeToggleGroup);
        if (lightThemeButton != null) lightThemeButton.setSelected(true);
    }

    /**
     * Reads the settings file and applies values to the UI fields.
     */
    private void loadSettings() {
        Map<String, String> settings = readSettings();

        // 1. Load Monthly Budget from the BudgetManager
        // This ensures it's always in sync with the dashboard
        monthlyBudgetField.setText(String.valueOf(BudgetManager.getMonthlyBudget()));

        // 2. Load Currency and pre-select ComboBox
        String currencyCode = settings.getOrDefault("CURRENCY", CurrencyUtil.getCurrencyCode());
        switch (currencyCode) {
            case "USD": currencyComboBox.setValue("USD ($)"); break;
            case "EUR": currencyComboBox.setValue("EUR (€)"); break;
            case "GBP": currencyComboBox.setValue("GBP (£)"); break;
            case "JPY": currencyComboBox.setValue("JPY (¥)"); break;
            default: currencyComboBox.setValue("PHP (₱)");
        }

        // 3. Load Theme
        String theme = settings.getOrDefault("THEME", "Light");
        if ("Dark".equals(theme) && darkThemeButton != null) {
            darkThemeButton.setSelected(true);
        } else if (lightThemeButton != null) {
            lightThemeButton.setSelected(true);
        }
    }


    /**
     * Handles saving all essential settings from the UI to the file
     */
    @FXML
    private void handleSave() {
        try {
            String monthlyBudgetText = monthlyBudgetField.getText();
            String currencySelection = currencyComboBox.getValue();
            String newTheme = (darkThemeButton != null && darkThemeButton.isSelected()) ? "Dark" : "Light";

            // 1. Validate and save Monthly Budget
            double newBudget = Double.parseDouble(monthlyBudgetText);
            if (newBudget < 0) {
                throw new NumberFormatException("Budget must be positive.");
            }
            // --- FUNCTIONAL BUDGET LINK ---
            // Set the budget in the central manager. This will update all listeners.
            BudgetManager.setMonthlyBudget(newBudget);
            // ------------------------------

            // 2. Extract and save Currency
            String currencyCode = extractCode(currencySelection);
            CurrencyUtil.setCurrency(currencyCode); // This will update all currency listeners

            // 3. Apply Theme Change Globally
            if (mainApp != null) {
                mainApp.applyThemeToScene(newTheme);
            }

            // 4. Persist all settings to file
            persistSettings(currencyCode, newTheme);

            // 5. Refresh UI elements on other tabs
            Scene scene = getScene();
            if (scene != null) {
                // Refresh all relevant tables/lists to show new currency symbol
                refreshNodeIfPresent(scene, "#transactionsListView");
                refreshNodeIfPresent(scene, "#expenseTableView");
                refreshNodeIfPresent(scene, "#incomeTableView");
                refreshNodeIfPresent(scene, "#transactionReportTable"); // Refresh reports table too
            }

            statusLabel.setText("Settings saved successfully!");
            statusLabel.setStyle("-fx-text-fill: #27AE60; -fx-padding: 5 8; -fx-background-color: rgba(39,174,96,0.08); -fx-background-radius: 8;");

        } catch (NumberFormatException nfe) {
            statusLabel.setText("Invalid budget amount. Must be a positive number.");
            statusLabel.setStyle("-fx-text-fill: #E74C3C; -fx-padding: 5 8; -fx-background-color: rgba(231,76,60,0.08); -fx-background-radius: 8;");
        } catch (Exception e) {
            statusLabel.setText("Failed to save settings.");
            statusLabel.setStyle("-fx-text-fill: #E74C3C; -fx-padding: 5 8; -fx-background-color: rgba(231,76,60,0.08); -fx-background-radius: 8;");
            e.printStackTrace();
        }
    }

    /**
     * Reads the settings file into a simple Map
     */
    private Map<String, String> readSettings() {
        Map<String, String> map = new HashMap<>();
        try {
            Path p = Path.of(SETTINGS_FILE);
            if (Files.exists(p)) {
                List<String> lines = Files.readAllLines(p);
                for (String line : lines) {
                    if (line == null || line.isBlank()) continue;
                    int idx = line.indexOf('=');
                    if (idx > 0) {
                        map.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
                    }
                }
            }
        } catch (Exception ignored) {}
        return map;
    }

    /**
     * Merges the new settings with existing ones and writes to the file.
     */
    private void persistSettings(String currencyCode, String theme) {
        Map<String, String> settings = readSettings();

        // Update with new values
        // BUDGET is now saved by BudgetManager
        settings.put("CURRENCY", currencyCode);
        settings.put("THEME", theme); // Save the theme preference

        // Write the new map back to the file
        try (PrintWriter pw = new PrintWriter(new FileWriter(SETTINGS_FILE))) {
            settings.forEach((k, v) -> pw.println(k + "=" + v));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Refreshes TableView or ListView nodes by ID.
     */
    private void refreshNodeIfPresent(Scene scene, String selector) {
        try {
            Node node = scene.lookup(selector);
            if (node instanceof TableView) {
                ((TableView<?>) node).refresh();
            } else if (node instanceof ListView) {
                ((ListView<?>) node).refresh();
            }
        } catch (Exception ignored) {}
    }

    private String extractCode(String selection) {
        if (selection == null) return "PHP";
        int idx = selection.indexOf(" ");
        if (idx > 0) return selection.substring(0, idx);
        return selection;
    }

    private Scene getScene() {
        if (statusLabel != null && statusLabel.getScene() != null) return statusLabel.getScene();
        if (monthlyBudgetField != null && monthlyBudgetField.getScene() != null) return monthlyBudgetField.getScene();
        return null;
    }
}