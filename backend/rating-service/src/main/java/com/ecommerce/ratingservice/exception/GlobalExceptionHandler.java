package com.ecommerce.ratingservice.exception;

import com.ecommerce.ratingservice.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Rating Service.
 * Provides consistent error responses and logging across all endpoints.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    private static final String PRODUCTION_PROFILE = "prod";

    /**
     * Handle custom RatingException
     */
    @ExceptionHandler(RatingException.class)
    public ResponseEntity<ErrorResponse> handleRatingException(
            RatingException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        log.error("RatingException [traceId={}]: {} - Status: {}", 
                traceId, ex.getMessage(), ex.getStatus());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .errorCode(ex.getErrorCode())
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();

        return new ResponseEntity<>(errorResponse, ex.getStatus());
    }

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        log.warn("Validation failed [traceId={}]: {}", traceId, ex.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ErrorResponse.FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(maskSensitiveValue(error.getField(), error.getRejectedValue()))
                        .build())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("One or more fields failed validation")
                .errorCode("VALIDATION_ERROR")
                .path(request.getRequestURI())
                .traceId(traceId)
                .fieldErrors(fieldErrors)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle constraint violations
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        log.warn("Constraint violation [traceId={}]: {}", traceId, ex.getMessage());

        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> ErrorResponse.FieldError.builder()
                        .field(violation.getPropertyPath().toString())
                        .message(violation.getMessage())
                        .rejectedValue(violation.getInvalidValue())
                        .build())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Constraint Violation")
                .message("Request parameters failed validation")
                .errorCode("CONSTRAINT_VIOLATION")
                .path(request.getRequestURI())
                .traceId(traceId)
                .fieldErrors(fieldErrors)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle missing request headers
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(
            MissingRequestHeaderException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        log.warn("Missing header [traceId={}]: {}", traceId, ex.getHeaderName());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Missing Request Header")
                .message(String.format("Required header '%s' is missing", ex.getHeaderName()))
                .errorCode("MISSING_HEADER")
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle missing request parameters
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        log.warn("Missing parameter [traceId={}]: {}", traceId, ex.getParameterName());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Missing Request Parameter")
                .message(String.format("Required parameter '%s' is missing", ex.getParameterName()))
                .errorCode("MISSING_PARAMETER")
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle type mismatch errors
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        log.warn("Type mismatch [traceId={}]: {} - Expected: {}", 
                traceId, ex.getName(), ex.getRequiredType());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Type Mismatch")
                .message(String.format("Parameter '%s' should be of type '%s'", 
                        ex.getName(), 
                        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"))
                .errorCode("TYPE_MISMATCH")
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle malformed JSON
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        log.warn("Malformed request body [traceId={}]: {}", traceId, ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Malformed Request Body")
                .message("Request body is not valid JSON or contains invalid data")
                .errorCode("MALFORMED_REQUEST")
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle unsupported HTTP methods
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        log.warn("Method not supported [traceId={}]: {} for path {}", 
                traceId, ex.getMethod(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error("Method Not Allowed")
                .message(String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod()))
                .errorCode("METHOD_NOT_ALLOWED")
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * Handle unsupported media types
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        log.warn("Media type not supported [traceId={}]: {}", traceId, ex.getContentType());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .error("Unsupported Media Type")
                .message(String.format("Content type '%s' is not supported", ex.getContentType()))
                .errorCode("UNSUPPORTED_MEDIA_TYPE")
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    /**
     * Handle 404 Not Found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        log.warn("No handler found [traceId={}]: {} {}", traceId, ex.getHttpMethod(), ex.getRequestURL());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(String.format("No endpoint found for %s %s", ex.getHttpMethod(), ex.getRequestURL()))
                .errorCode("ENDPOINT_NOT_FOUND")
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle database constraint violations
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        log.error("Data integrity violation [traceId={}]: {}", traceId, ex.getMessage());

        String message = "A database constraint was violated";
        if (ex.getMessage() != null && ex.getMessage().contains("Duplicate entry")) {
            message = "A record with this data already exists";
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Data Integrity Violation")
                .message(message)
                .errorCode("DATA_INTEGRITY_VIOLATION")
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    /**
     * Handle all other unhandled exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, HttpServletRequest request) {
        
        String traceId = generateTraceId();
        log.error("Unhandled exception [traceId={}]: {} - {}", 
                traceId, ex.getClass().getSimpleName(), ex.getMessage(), ex);

        // Don't expose internal error details in production
        String message = isProduction() 
                ? "An unexpected error occurred. Please try again later." 
                : ex.getMessage();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message(message)
                .errorCode("INTERNAL_ERROR")
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Generate a unique trace ID for error tracking
     */
    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Check if running in production mode
     */
    private boolean isProduction() {
        return PRODUCTION_PROFILE.equalsIgnoreCase(activeProfile);
    }

    /**
     * Mask sensitive field values in error responses
     */
    private Object maskSensitiveValue(String fieldName, Object value) {
        if (fieldName == null || value == null) {
            return value;
        }
        
        String lowerFieldName = fieldName.toLowerCase();
        if (lowerFieldName.contains("password") || 
            lowerFieldName.contains("token") || 
            lowerFieldName.contains("secret") ||
            lowerFieldName.contains("key")) {
            return "***MASKED***";
        }
        
        return value;
    }
}
