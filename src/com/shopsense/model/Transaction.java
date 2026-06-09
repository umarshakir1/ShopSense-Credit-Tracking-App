package com.shopsense.model;

import java.time.LocalDate;

/**
 * Abstract class representing a general financial transaction.
 */
public abstract class Transaction {
    private String transactionId;
    private double amount;
    private LocalDate date;
    private String notes;

    public Transaction(String transactionId, double amount, LocalDate date, String notes) {
        this.transactionId = transactionId;
        this.amount = amount;
        this.date = date;
        this.notes = notes;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Gets the net effect of this transaction on the account balance.
     * Credit transactions will increase the outstanding balance, while
     * payments will decrease the outstanding balance.
     * @return the mathematical effect (+amount or -amount)
     */
    public abstract double getEffectOnBalance();

    /**
     * Returns a user-friendly string of the transaction type.
     * @return transaction type label
     */
    public abstract String getTransactionType();

    @Override
    public String toString() {
        return "Transaction[ID=" + transactionId + ", Type=" + getTransactionType() + 
               ", Amount=" + amount + ", Date=" + date + ", Notes=" + notes + "]";
    }
}
