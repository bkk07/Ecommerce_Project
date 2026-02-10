package com.ecommerce.paymentservice.exception;

/**
 * Exception thrown when payment signature verification fails
 */
public class PaymentVerificationException extends RuntimeException {
    
    public PaymentVerificationException(String message) {
        super(message);
    }
    
    public PaymentVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static PaymentVerificationException signatureInvalid(String razorpayOrderId) {
        return new PaymentVerificationException("Signature verification failed for Razorpay order: " + razorpayOrderId);
    }
}
