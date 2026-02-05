package com.ecommerce.checkoutservice.dto;

/**
 * Enum representing the possible reasons for checkout failure.
 */
public enum CheckoutFailureReason {
    /**
     * One or more products failed validation (out of stock, price mismatch, etc.)
     */
    PRODUCT_VALIDATION_FAILED,
    
    /**
     * Payment was declined or failed
     */
    PAYMENT_FAILED,
    
    /**
     * Payment signature verification failed
     */
    PAYMENT_VERIFICATION_FAILED,
    
    /**
     * Checkout session expired before completion
     */
    SESSION_EXPIRED,
    
    /**
     * One or more downstream services are unavailable
     */
    SERVICE_UNAVAILABLE,
    
    /**
     * Order creation failed
     */
    ORDER_CREATION_FAILED,
    
    /**
     * Cart is empty or items are invalid
     */
    INVALID_CART
}
