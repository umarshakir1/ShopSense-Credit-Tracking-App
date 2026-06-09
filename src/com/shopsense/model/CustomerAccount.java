package com.shopsense.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Concrete financial account class for customers.
 * Handles dynamic balance calculations and overloads addTransaction() for diverse signatures.
 */
public class CustomerAccount extends Account {

    public CustomerAccount(String accountId) {
        super(accountId);
    }

    @Override
    public double calculateBalance() {
        double balance = 0.0;
        for (Transaction transaction : transactions) {
            balance += transaction.getEffectOnBalance();
        }
        return balance;
    }

    @Override
    public void addTransaction(Transaction transaction) {
        this.transactions.add(transaction);
    }

    // Overloaded addTransaction #1: Basic amount, notes, type (Defaults to current date and no category)
    public void addTransaction(double amount, String notes, String type) {
        addTransaction(amount, notes, type, LocalDate.now(), "Others");
    }

    // Overloaded addTransaction #2: Custom date, amount, notes, type
    public void addTransaction(double amount, String notes, String type, LocalDate date) {
        addTransaction(amount, notes, type, date, "Others");
    }

    // Overloaded addTransaction #3: Complete detail including custom date and category
    public void addTransaction(double amount, String notes, String type, LocalDate date, String category) {
        String txId = "TX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Transaction newTx;
        if ("Payment".equalsIgnoreCase(type)) {
            newTx = new PaymentTransaction(txId, amount, date, notes);
        } else {
            newTx = new CreditTransaction(txId, amount, date, notes, category);
        }
        addTransaction(newTx);
    }
}
