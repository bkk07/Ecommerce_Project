package com.ecommerce.paymentservice.exception;

/**
 * Exception thrown when payment gateway (Razorpay) operations fail
 */
public class PaymentGatewayException extends RuntimeException {
    
    public PaymentGatewayException(String message) {
        super(message);
    }
    
    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static PaymentGatewayException orderCreationFailed(String orderId, Throwable cause) {
        return new PaymentGatewayException("Failed to create Razorpay order for order: " + orderId, cause);
    }
    
    public static PaymentGatewayException refundFailed(String orderId, Throwable cause) {
        return new PaymentGatewayException("Failed to process refund for order: " + orderId, cause);
    }
    
    public static PaymentGatewayException fetchFailed(String razorpayOrderId, Throwable cause) {
        return new PaymentGatewayException("Failed to fetch order status from Razorpay: " + razorpayOrderId, cause);
    }
}
