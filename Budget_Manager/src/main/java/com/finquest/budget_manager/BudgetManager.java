//Esguerra
package com.finquest.budget_manager;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetManager {
    private static final String SETTINGS_FILE = "finquest_settings.txt";
    private static double monthlyBudget = 2000.00; // Default

    private static final List<BudgetChangeListener> listeners = new ArrayList<>();

    static {
        loadBudget();
    }

    private static void loadBudget() {
        try {
            File file = new File(SETTINGS_FILE);
            if (file.exists()) {
                List<String> lines = Files.readAllLines(file.toPath());
                for (String line : lines) {
                    if (line.startsWith("MONTHLY_BUDGET=")) {
                        String val = line.substring("MONTHLY_BUDGET=".length()).trim();
                        if (!val.isEmpty()) {
                            monthlyBudget = Double.parseDouble(val);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("BudgetManager: Could not load budget, using default.");
            monthlyBudget = 2000.00;
        }
    }

    private static void saveBudget() {
        // Read existing map, update budget, and write back
        Map<String, String> settings = new HashMap<>();
        try {
            Path p = Path.of(SETTINGS_FILE);
            if (Files.exists(p)) {
                List<String> lines = Files.readAllLines(p);
                for (String line : lines) {
                    if (line == null || line.isBlank()) continue;
                    int idx = line.indexOf('=');
                    if (idx > 0) {
                        settings.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
                    }
                }
            }
        } catch (Exception ignored) {}

        // Update the budget value
        settings.put("MONTHLY_BUDGET", String.valueOf(monthlyBudget));

        // Write the new map back to the file
        try (PrintWriter pw = new PrintWriter(new FileWriter(SETTINGS_FILE))) {
            settings.forEach((k, v) -> pw.println(k + "=" + v));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Public Methods

    public static double getMonthlyBudget() {
        return monthlyBudget;
    }

    public static void setMonthlyBudget(double newBudget) {
        if (newBudget < 0) return;
        monthlyBudget = newBudget;
        saveBudget();
        notifyListeners();
    }

    // Listener System

    @FunctionalInterface
    public interface BudgetChangeListener {
        void onBudgetChanged(double newBudget);
    }

    public static void addListener(BudgetChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeListener(BudgetChangeListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (BudgetChangeListener listener : listeners) {
            if (listener != null) {
                listener.onBudgetChanged(monthlyBudget);
            }
        }
    }
}