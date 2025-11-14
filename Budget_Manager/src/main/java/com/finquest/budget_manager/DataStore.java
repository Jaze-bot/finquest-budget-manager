//Esguerra
package com.finquest.budget_manager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.util.ArrayList;
import java.util.List; // <-- THE FIX: This line was missing

/**
 * Singleton DataStore to hold the application's transaction list.
 * This ensures all controllers are looking at the same data.
 */
public class DataStore {
    private static final DataStore instance = new DataStore();
    private static final String DATA_FILE = "finquest_data.dat";

    private final ObservableList<Transaction> transactions;

    private DataStore() {
        // Initialize with an empty list.
        // BudgetApplication's loadData() will populate it.
        transactions = FXCollections.observableArrayList();
        System.out.println("DataStore: new instance created");
    }

    public static DataStore getInstance() {
        return instance;
    }

    public ObservableList<Transaction> getTransactions() {
        return transactions;
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        // You could also call save() here if you want to save on every add
    }

    /**
     * Saves the current list of transactions to the data file.
     * This is called by BudgetApplication.
     */
    public void save() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            // Must save as a standard ArrayList, not an ObservableList
            oos.writeObject(new ArrayList<>(transactions));
            System.out.println("DataStore: saved " + transactions.size() + " transactions to " + DATA_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads transactions from the data file.
     * Note: This is now handled by BudgetApplication's loadData() method.
     * This method is kept for future reference if you refactor.
     */
    @SuppressWarnings("unchecked")
    public void load() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            File file = new File(DATA_FILE);
            if (file.exists()) {
                List<Transaction> savedList = (List<Transaction>) ois.readObject();
                transactions.setAll(savedList);
                System.out.println("DataStore: loaded " + savedList.size() + " transactions from " + DATA_FILE);
            }
        } catch (Exception e) {
            System.out.println("DataStore: Could not load data. " + e.getMessage());
        }
    }
}