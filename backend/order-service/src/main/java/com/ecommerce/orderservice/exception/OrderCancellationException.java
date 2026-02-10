package com.ecommerce.orderservice.exception;

/**
 * Exception thrown when an order cancellation fails
 */
public class OrderCancellationException extends RuntimeException {
    
    private final String orderId;
    private final String reason;
    
    public OrderCancellationException(String orderId, String reason) {
        super("Cannot cancel order " + orderId + ": " + reason);
        this.orderId = orderId;
        this.reason = reason;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public String getReason() {
        return reason;
    }
}
