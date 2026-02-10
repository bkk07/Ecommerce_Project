package com.ecommerce.paymentservice.exception;

/**
 * Exception thrown when a payment record is not found
 */
public class PaymentNotFoundException extends RuntimeException {
    
    public PaymentNotFoundException(String message) {
        super(message);
    }
    
    public PaymentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static PaymentNotFoundException forOrderId(String orderId) {
        return new PaymentNotFoundException("Payment not found for order: " + orderId);
    }
    
    public static PaymentNotFoundException forRazorpayOrderId(String razorpayOrderId) {
        return new PaymentNotFoundException("Payment not found for Razorpay order: " + razorpayOrderId);
    }
}
