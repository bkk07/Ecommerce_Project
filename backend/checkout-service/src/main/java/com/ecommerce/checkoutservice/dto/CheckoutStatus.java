package com.ecommerce.checkoutservice.dto;

/**
 * Enum representing the possible states of a checkout operation.
 */
public enum CheckoutStatus {
    /**
     * Checkout initiated successfully, awaiting payment
     */
    SUCCESS,
    
    /**
     * Checkout is pending/in progress
     */
    PENDING,
    
    /**
     * Checkout failed due to validation or payment issues
     */
    FAILED,
    
    /**
     * Checkout session expired
     */
    EXPIRED
}
