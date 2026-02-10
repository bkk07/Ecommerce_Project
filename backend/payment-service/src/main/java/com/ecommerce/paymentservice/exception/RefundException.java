package com.ecommerce.paymentservice.exception;

/**
 * Exception thrown when refund operations fail
 */
public class RefundException extends RuntimeException {
    
    public RefundException(String message) {
        super(message);
    }
    
    public RefundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static RefundException alreadyRefunded(String orderId) {
        return new RefundException("Payment already refunded for order: " + orderId);
    }
    
    public static RefundException invalidStatus(String orderId, String status) {
        return new RefundException("Cannot refund payment with status " + status + " for order: " + orderId);
    }
}
