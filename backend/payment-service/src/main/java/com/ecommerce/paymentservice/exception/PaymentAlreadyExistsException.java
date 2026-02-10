package com.ecommerce.paymentservice.exception;

/**
 * Exception thrown when attempting to create a duplicate payment
 */
public class PaymentAlreadyExistsException extends RuntimeException {
    
    public PaymentAlreadyExistsException(String message) {
        super(message);
    }
    
    public PaymentAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static PaymentAlreadyExistsException forOrderId(String orderId) {
        return new PaymentAlreadyExistsException("Payment already initiated for order: " + orderId);
    }
}
