package com.shopsense.model;

import java.util.ArrayList;

/**
 * Abstract class representing a financial Account.
 * Manages lists of associated Transaction objects.
 */
public abstract class Account {
    protected String accountId;
    protected ArrayList<Transaction> transactions;

    public Account(String accountId) {
        this.accountId = accountId;
        this.transactions = new ArrayList<>();
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public ArrayList<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(ArrayList<Transaction> transactions) {
        this.transactions = transactions;
    }

    /**
     * Calculates the current balance of the account dynamically by traversing transactions.
     * @return outstanding balance
     */
    public abstract double calculateBalance();

    /**
     * Records a transaction into the account ledger.
     * @param transaction the transaction to add
     */
    public abstract void addTransaction(Transaction transaction);
}
