package com.ecommerce.ratingservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a user attempts to access/modify a resource they don't own.
 */
public class UnauthorizedAccessException extends RatingException {

    public UnauthorizedAccessException(String action, String resourceType) {
        super(String.format("You can only %s your own %s", action, resourceType),
              HttpStatus.FORBIDDEN,
              "UNAUTHORIZED_ACCESS");
    }

    public UnauthorizedAccessException(String message) {
        super(message, HttpStatus.FORBIDDEN, "UNAUTHORIZED_ACCESS");
    }
}
