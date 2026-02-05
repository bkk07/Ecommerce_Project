package com.ecommerce.checkoutservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Externalized configuration for checkout service.
 * Values are loaded from application.yml under "checkout" prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "checkout")
@Validated
@Getter
@Setter
public class CheckoutConfig {
    
    /**
     * Currency code for transactions (e.g., INR, USD)
     */
    @NotBlank(message = "Currency must be configured")
    private String currency = "INR";
    
    /**
     * Default status for new checkout sessions
     */
    @NotBlank(message = "Default status must be configured")
    private String statusProcessing = "PROCESSING";
    
    /**
     * Status for completed checkouts
     */
    private String statusCompleted = "COMPLETED";
    
    /**
     * Status for failed checkouts
     */
    private String statusFailed = "FAILED";
    
    /**
     * Status for expired sessions
     */
    private String statusExpired = "EXPIRED";
    
    /**
     * Session timeout in minutes
     */
    @Positive(message = "Session timeout must be positive")
    private int sessionTimeoutMinutes = 20;
    
    /**
     * Maximum items allowed in a single checkout
     */
    @Positive(message = "Max items must be positive")
    private int maxItemsPerCheckout = 50;
    
    /**
     * Idempotency key expiration in minutes
     */
    @Positive(message = "Idempotency expiration must be positive")
    private int idempotencyExpirationMinutes = 30;
}
