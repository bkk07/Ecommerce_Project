package com.ecommerce.paymentservice.exception;

/**
 * Exception thrown when webhook processing fails
 */
public class WebhookProcessingException extends RuntimeException {
    
    public WebhookProcessingException(String message) {
        super(message);
    }
    
    public WebhookProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static WebhookProcessingException invalidSignature() {
        return new WebhookProcessingException("Invalid webhook signature");
    }
    
    public static WebhookProcessingException processingFailed(Throwable cause) {
        return new WebhookProcessingException("Webhook processing failed", cause);
    }
}
