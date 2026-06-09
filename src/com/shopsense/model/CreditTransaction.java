package com.shopsense.model;

import java.time.LocalDate;

/**
 * Concrete transaction subclass representing credit extended to a customer ("udhaar" purchase).
 * Overrides methods to specify a positive balance impact and support product categorization.
 */
public class CreditTransaction extends Transaction {
    private String category; // Groceries, Medicines, Household, Others

    public CreditTransaction(String transactionId, double amount, LocalDate date, String notes, String category) {
        super(transactionId, amount, date, notes);
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public double getEffectOnBalance() {
        return getAmount(); // Credit increases the customer's outstanding balance
    }

    @Override
    public String getTransactionType() {
        return "Credit";
    }

    @Override
    public String toString() {
        return "CreditTransaction[ID=" + getTransactionId() + ", Amount=" + getAmount() + 
               ", Category=" + category + ", Date=" + getDate() + ", Notes=" + getNotes() + "]";
    }
}
