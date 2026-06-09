package com.shopsense.model;

import com.shopsense.exception.CreditLimitExceededException;

/**
 * Concrete Customer class representing retail customers in ShopSense.
 * Extends Person, implements Alertable, and manages an active CustomerAccount.
 */
public class Customer extends Person implements Alertable {
    private double creditLimit;
    private CustomerAccount account;

    public Customer(String id, String name, String phoneNumber, String email, double creditLimit) {
        super(id, name, phoneNumber, email);
        this.creditLimit = creditLimit;
        this.account = new CustomerAccount("ACC-" + id);
    }

    public double getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(double creditLimit) {
        this.creditLimit = creditLimit;
    }

    public CustomerAccount getAccount() {
        return account;
    }

    public void setAccount(CustomerAccount account) {
        this.account = account;
    }

    @Override
    public boolean checkCreditLimit() {
        // Exceeds limit if current outstanding balance is greater than the set credit limit
        return account.calculateBalance() > creditLimit;
    }

    @Override
    public void sendAlert() throws CreditLimitExceededException {
        if (checkCreditLimit()) {
            double currentBalance = account.calculateBalance();
            double exceededBy = currentBalance - creditLimit;
            throw new CreditLimitExceededException(
                String.format("Alert: Customer '%s' has exceeded their credit limit of Rs. %.2f! Current outstanding balance: Rs. %.2f (Exceeded by Rs. %.2f)",
                    getName(), creditLimit, currentBalance, exceededBy)
            );
        }
    }

    @Override
    public String toString() {
        return "Customer[ID=" + getId() + ", Name=" + getName() + 
               ", Balance=" + account.calculateBalance() + ", Limit=" + creditLimit + "]";
    }
}
