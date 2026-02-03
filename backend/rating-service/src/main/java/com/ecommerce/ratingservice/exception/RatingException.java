package com.ecommerce.ratingservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Custom exception for rating-related errors.
 * Provides HTTP status code mapping for proper REST API responses.
 */
@Getter
public class RatingException extends RuntimeException {
    
    private final HttpStatus status;
    private final String errorCode;

    public RatingException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorCode = null;
    }

    public RatingException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public RatingException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = null;
    }

    // Factory methods for common exceptions
    public static RatingException notFound(String message) {
        return new RatingException(message, HttpStatus.NOT_FOUND, "RATING_NOT_FOUND");
    }

    public static RatingException alreadyExists(String message) {
        return new RatingException(message, HttpStatus.CONFLICT, "RATING_ALREADY_EXISTS");
    }

    public static RatingException notEligible(String message) {
        return new RatingException(message, HttpStatus.FORBIDDEN, "NOT_ELIGIBLE_TO_RATE");
    }

    public static RatingException unauthorized(String message) {
        return new RatingException(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    public static RatingException forbidden(String message) {
        return new RatingException(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    public static RatingException badRequest(String message) {
        return new RatingException(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public static RatingException internal(String message) {
        return new RatingException(message, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");
    }

    public static RatingException serviceUnavailable(String message) {
        return new RatingException(message, HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE");
    }
}
