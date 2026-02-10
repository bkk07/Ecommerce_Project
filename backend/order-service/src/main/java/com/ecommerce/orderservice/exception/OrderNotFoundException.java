package com.ecommerce.orderservice.exception;

/**
 * Exception thrown when an order cannot be found
 */
public class OrderNotFoundException extends RuntimeException {
    
    private final String orderId;
    
    public OrderNotFoundException(String orderId) {
        super("Order not found with ID: " + orderId);
        this.orderId = orderId;
    }
    
    public OrderNotFoundException(String orderId, String message) {
        super(message);
        this.orderId = orderId;
    }
    
    public String getOrderId() {
        return orderId;
    }
}
