package com.ecommerce.orderservice.exception;

import java.util.List;

/**
 * Exception thrown when order validation fails
 */
public class OrderValidationException extends RuntimeException {
    
    private final List<String> errors;
    
    public OrderValidationException(String message) {
        super(message);
        this.errors = List.of(message);
    }
    
    public OrderValidationException(List<String> errors) {
        super("Validation failed: " + String.join(", ", errors));
        this.errors = errors;
    }
    
    public List<String> getErrors() {
        return errors;
    }
}
