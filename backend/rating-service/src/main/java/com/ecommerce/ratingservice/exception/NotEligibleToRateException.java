package com.ecommerce.ratingservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user is not eligible to rate a product.
 */
public class NotEligibleToRateException extends RatingException {

    public NotEligibleToRateException(String userId, String orderId, String sku) {
        super(String.format("User %s is not eligible to rate SKU %s for order %s. " +
                           "Only verified purchasers with delivered orders can rate products.",
                           userId, sku, orderId),
              HttpStatus.FORBIDDEN,
              "NOT_ELIGIBLE_TO_RATE");
    }

    public NotEligibleToRateException(String message) {
        super(message, HttpStatus.FORBIDDEN, "NOT_ELIGIBLE_TO_RATE");
    }
}
