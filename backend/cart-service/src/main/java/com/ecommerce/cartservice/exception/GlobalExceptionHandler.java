package com.ecommerce.cartservice.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CartException.class)
    public ResponseEntity<Object> handleCartException(CartException ex) {
        log.warn("Cart error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Cart Error",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now()
        ));
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<Object> handleRedisConnectionException(RedisConnectionFailureException ex) {
        log.error("Redis connection failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "Service Unavailable",
                "message", "Unable to connect to Redis. Please check if Redis is running.",
                "timestamp", LocalDateTime.now()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Validation Error",
                "message", "Invalid request data",
                "details", errors,
                "timestamp", LocalDateTime.now()
        ));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Object> handleFeignException(FeignException ex) {
        log.error("Feign client error: {} - {}", ex.status(), ex.getMessage());
        String message = "Unable to communicate with product service";
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        
        if (ex.status() == 404) {
            message = "Product not found";
            status = HttpStatus.NOT_FOUND;
        } else if (ex.status() >= 400 && ex.status() < 500) {
            status = HttpStatus.BAD_REQUEST;
        }
        
        return ResponseEntity.status(status).body(Map.of(
                "error", "External Service Error",
                "message", message,
                "timestamp", LocalDateTime.now()
        ));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Object> handleMissingHeaderException(MissingRequestHeaderException ex) {
        log.warn("Missing required header: {}", ex.getHeaderName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Missing Header",
                "message", "Required header '" + ex.getHeaderName() + "' is missing",
                "timestamp", LocalDateTime.now()
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "Access Denied",
                "message", "You don't have permission to access this resource",
                "timestamp", LocalDateTime.now()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal Server Error",
                "message", "An unexpected error occurred. Please try again later.",
                "timestamp", LocalDateTime.now()
        ));
    }
}