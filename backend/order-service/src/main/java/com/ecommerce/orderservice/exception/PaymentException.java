package com.ecommerce.orderservice.exception;

/**
 * Exception thrown when payment operations fail
 */
public class PaymentException extends RuntimeException {
    
    private final String orderId;
    private final String paymentId;
    
    public PaymentException(String message) {
        super(message);
        this.orderId = null;
        this.paymentId = null;
    }
    
    public PaymentException(String orderId, String message) {
        super(message);
        this.orderId = orderId;
        this.paymentId = null;
    }
    
    public PaymentException(String orderId, String paymentId, String message) {
        super(message);
        this.orderId = orderId;
        this.paymentId = paymentId;
    }
    
    public PaymentException(String message, Throwable cause) {
        super(message, cause);
        this.orderId = null;
        this.paymentId = null;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public String getPaymentId() {
        return paymentId;
    }
}
