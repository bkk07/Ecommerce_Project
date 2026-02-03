package com.ecommerce.ratingservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when attempting to create a duplicate rating.
 */
public class DuplicateRatingException extends RatingException {

    public DuplicateRatingException(String orderId, String sku) {
        super(String.format("Rating already exists for order: %s and SKU: %s", orderId, sku),
              HttpStatus.CONFLICT,
              "DUPLICATE_RATING");
    }

    public DuplicateRatingException(String message) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_RATING");
    }
}
