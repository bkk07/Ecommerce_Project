package com.ecommerce.orderservice.exception;

/**
 * Exception thrown when inventory lock fails during order creation.
 */
public class InventoryLockException extends RuntimeException {
    private final String orderId;
    private final String reason;

    public InventoryLockException(String orderId, String reason) {
        super("Failed to lock inventory for order " + orderId + ": " + reason);
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
