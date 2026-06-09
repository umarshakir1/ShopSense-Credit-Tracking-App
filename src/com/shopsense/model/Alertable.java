package com.shopsense.model;

import com.shopsense.exception.CreditLimitExceededException;

/**
 * Interface that defines the capability to track and warn about credit limit thresholds.
 */
public interface Alertable {
    /**
     * Checks if the entity has exceeded its credit limit.
     * @return true if exceeded, false otherwise
     */
    boolean checkCreditLimit();

    /**
     * Triggers/sends an alert if the credit limit is exceeded.
     * @throws CreditLimitExceededException if credit limit is exceeded
     */
    void sendAlert() throws CreditLimitExceededException;
}
