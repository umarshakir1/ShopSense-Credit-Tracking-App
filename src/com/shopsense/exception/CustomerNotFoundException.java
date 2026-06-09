package com.shopsense.exception;

/**
 * Exception thrown when a requested Customer cannot be found by ID or Name search in the repository.
 */
public class CustomerNotFoundException extends Exception {
    public CustomerNotFoundException(String message) {
        super(message);
    }
}
