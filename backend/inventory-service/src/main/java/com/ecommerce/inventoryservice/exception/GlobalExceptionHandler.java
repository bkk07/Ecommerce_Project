package com.ecommerce.inventoryservice.exception;

import com.ecommerce.inventoryservice.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 1. Handle Resource Not Found (404)
    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(InventoryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.builder()
                        .errorCode("NOT_FOUND")
                        .errorMessage(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // 2. Handle Business Logic Failures (400)
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(InsufficientStockException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .errorCode("INSUFFICIENT_STOCK")
                        .errorMessage(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // 3. Handle Concurrency Conflicts (409) - Crucial for Inventory!
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrency(OptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                        .errorCode("CONCURRENCY_CONFLICT")
                        .errorMessage("The stock was updated by another transaction. Please retry.")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // 4. Handle Validation Errors (400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .errorCode("VALIDATION_ERROR")
                        .errorMessage(details)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // 5. Handle IllegalArgumentException (400)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .errorCode("INVALID_REQUEST")
                        .errorMessage(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // 6. Handle IllegalStateException (409)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorResponse.builder()
                        .errorCode("INVALID_STATE")
                        .errorMessage(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    // 7. Catch-All for unexpected errors (500) - DO NOT EXPOSE INTERNAL DETAILS
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        // Generate error ID for tracking
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        
        // Log the full exception for debugging
        log.error("Unexpected error [errorId={}]: {}", errorId, ex.getMessage(), ex);
        
        // Return sanitized error to client - DO NOT expose internal details
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.builder()
                        .errorCode("INTERNAL_SERVER_ERROR")
                        .errorMessage("An unexpected error occurred. Please contact support with error ID: " + errorId)
                        .errorId(errorId)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
