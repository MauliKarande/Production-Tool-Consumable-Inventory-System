package com.ameya.inventory.exception;

/**
 * Raised when a request is well-formed but violates a business rule
 * (e.g. issuing more than is currently available). Maps to HTTP 409.
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
