package com.shopsense.exception;

/**
 * Exception thrown when a transaction would cause a Customer's balance to exceed their defined credit limit.
 */
public class CreditLimitExceededException extends Exception {
    public CreditLimitExceededException(String message) {
        super(message);
    }
}
