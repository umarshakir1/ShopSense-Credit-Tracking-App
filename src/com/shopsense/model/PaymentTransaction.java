package com.shopsense.model;

import java.time.LocalDate;

/**
 * Concrete transaction subclass representing cash payments received from a customer.
 * Overrides methods to specify a negative balance impact (reducing outstanding debt).
 */
public class PaymentTransaction extends Transaction {

    public PaymentTransaction(String transactionId, double amount, LocalDate date, String notes) {
        super(transactionId, amount, date, notes);
    }

    @Override
    public double getEffectOnBalance() {
        return -getAmount(); // Repayment decreases the customer's outstanding balance
    }

    @Override
    public String getTransactionType() {
        return "Payment";
    }

    @Override
    public String toString() {
        return "PaymentTransaction[ID=" + getTransactionId() + ", Amount=" + getAmount() + 
               ", Date=" + getDate() + ", Notes=" + getNotes() + "]";
    }
}
