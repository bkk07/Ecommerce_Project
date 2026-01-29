package com.ecommerce.inventoryservice.exception;
import com.ecommerce.inventoryservice.dto.ErrorResponse;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
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

    // 4. Catch-All for unexpected errors (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.builder()
                        .errorCode("INTERNAL_SERVER_ERROR")
                        .errorMessage("An unexpected error occurred: " + ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
