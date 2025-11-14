//Esguerra
package com.finquest.budget_manager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Static utility class to manage currency formatting and listeners.
 */
public class CurrencyUtil {
    private static final String SETTINGS_FILE = "finquest_settings.txt";
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0.00");

    private static String currentSymbol = "₱"; // Default
    private static String currentCode = "PHP";  // Default

    // List of listeners to notify when currency changes
    private static final List<CurrencyChangeListener> listeners = new ArrayList<>();

    // Static initializer: Runs once when the class is first used.
    // This loads the saved currency from the settings file.
    static {
        try {
            File file = new File(SETTINGS_FILE);
            if (!file.exists()) {
                System.out.println("CurrencyUtil: No settings file, using default PHP.");
                setCurrency("PHP");
            } else {
                List<String> lines = Files.readAllLines(file.toPath());
                String code = "PHP"; // Default
                for (String line : lines) {
                    if (line.startsWith("CURRENCY=")) {
                        code = line.substring("CURRENCY=".length()).trim();
                        break;
                    }
                }
                setCurrency(code);
                System.out.println("CurrencyUtil: Loaded currency " + code);
            }
        } catch (IOException e) { // Catch the file exception properly
            System.out.println("CurrencyUtil: Error loading settings, using default PHP.");
            setCurrency("PHP");
        } catch (Exception e) {
            System.out.println("CurrencyUtil: Unknown error during settings load.");
            setCurrency("PHP");
        }
    }

    /**
     * Formats a double value into a currency string (e.g., "₱1,234.56").
     */
    public static String formatCurrency(double amount) {
        return currentSymbol + AMOUNT_FORMAT.format(amount);
    }

    /**
     * Sets the application's global currency.
     * @param code The currency code (e.g., "PHP", "USD").
     */
    public static void setCurrency(String code) {
        if (code == null) code = "PHP";
        currentCode = code;

        switch (code) {
            case "USD":
                currentSymbol = "$";
                break;
            case "EUR":
                currentSymbol = "€";
                break;
            case "GBP":
                currentSymbol = "£";
                break;
            case "JPY":
                currentSymbol = "¥";
                break;
            case "PHP":
            default:
                currentSymbol = "₱";
                currentCode = "PHP";
                break;
        }
        // Notify all registered listeners of the change
        notifyListeners();
    }

    public static String getCurrencyCode() {
        return currentCode;
    }

    // Listener Functional Interface
    @FunctionalInterface
    public interface CurrencyChangeListener {
        void onCurrencyChanged(String newCode, String newSymbol);
    }

    public static void addListener(CurrencyChangeListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(CurrencyChangeListener listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (CurrencyChangeListener listener : listeners) {
            if (listener != null) {
                listener.onCurrencyChanged(currentCode, currentSymbol);
            }
        }
    }
}