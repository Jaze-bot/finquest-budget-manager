//Esguerra
package com.finquest.budget_manager;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L; // For serialization
    private String title;
    private String category;
    private String type; // "Income" or "Expense"
    private double amount;
    private LocalDate date;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public Transaction(String title, String category, String type, double amount, LocalDate date) {
        this.title = title;
        this.category = category;
        this.type = type;
        this.amount = amount;
        this.date = date;
    }

    // --- Getters ---
    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    // --- Setters ---
    public void setTitle(String title) {
        this.title = title;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    // --- Formatter Utility ---
    public String getFormattedDate() {
        return date.format(DATE_FORMATTER);
    }

    @Override
    public String toString() {
        return String.format("%s (%s): %s %s",
                getFormattedDate(), getCategory(),
                ("Income".equals(type) ? "+" : "-"),
                CurrencyUtil.formatCurrency(getAmount()));
    }
}