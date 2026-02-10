package com.ecommerce.paymentservice.exception;

import com.ecommerce.paymentservice.dto.response.ApiErrorResponse;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Enhanced Global Exception Handler for Payment Service.
 * Provides comprehensive error handling with:
 * - Standardized error responses
 * - Distributed tracing integration (optional)
 * - Error categorization with codes
 * - Detailed validation errors
 * - Resilience4j exception handling
 */
@RestControllerAdvice
@Slf4j
public class EnhancedGlobalExceptionHandler {

    @Autowired(required = false)
    private Tracer tracer;

    // ==================== Error Codes ====================
    public static final String PAYMENT_NOT_FOUND = "PAYMENT_001";
    public static final String PAYMENT_ALREADY_EXISTS = "PAYMENT_002";
    public static final String PAYMENT_VERIFICATION_FAILED = "PAYMENT_003";
    public static final String PAYMENT_GATEWAY_ERROR = "PAYMENT_004";
    public static final String WEBHOOK_ERROR = "PAYMENT_005";
    public static final String REFUND_ERROR = "PAYMENT_006";
    public static final String VALIDATION_ERROR = "VALIDATION_001";
    public static final String CONSTRAINT_VIOLATION = "VALIDATION_002";
    public static final String CIRCUIT_BREAKER_OPEN = "RESILIENCE_001";
    public static final String RATE_LIMITED = "RESILIENCE_002";
    public static final String BULKHEAD_FULL = "RESILIENCE_003";
    public static final String TIMEOUT_ERROR = "RESILIENCE_004";
    public static final String DATABASE_ERROR = "DATA_001";
    public static final String DATA_INTEGRITY_ERROR = "DATA_002";
    public static final String SECURITY_ERROR = "SECURITY_001";
    public static final String BAD_REQUEST = "REQUEST_001";
    public static final String METHOD_NOT_ALLOWED = "REQUEST_002";
    public static final String UNSUPPORTED_MEDIA = "REQUEST_003";
    public static final String NOT_FOUND = "REQUEST_004";
    public static final String INTERNAL_ERROR = "INTERNAL_001";

    // ==================== Payment Specific Exceptions ====================

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentNotFound(
            PaymentNotFoundException ex, HttpServletRequest request) {
        log.error("Payment not found: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                PAYMENT_NOT_FOUND,
                "Payment Not Found",
                ex.getMessage(),
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId())
         .withSuggestion("Verify the order ID or Razorpay order ID and try again");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(PaymentAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentAlreadyExists(
            PaymentAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("Payment already exists: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                PAYMENT_ALREADY_EXISTS,
                "Payment Already Exists",
                ex.getMessage(),
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId())
         .withSuggestion("A payment has already been initiated for this order. Check payment status instead.");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(PaymentVerificationException.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentVerification(
            PaymentVerificationException ex, HttpServletRequest request) {
        log.error("Payment verification failed: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                PAYMENT_VERIFICATION_FAILED,
                "Payment Verification Failed",
                ex.getMessage(),
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId())
         .withSuggestion("Ensure the signature is calculated correctly using Razorpay secret key")
         .withDetails(Map.of(
                 "possibleCauses", List.of(
                         "Invalid payment signature",
                         "Tampered payment data",
                         "Incorrect secret key used"
                 )
         ));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentGateway(
            PaymentGatewayException ex, HttpServletRequest request) {
        log.error("Payment gateway error: {}", ex.getMessage(), ex);

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                PAYMENT_GATEWAY_ERROR,
                "Payment Gateway Error",
                "Payment gateway is temporarily unavailable. Please try again later.",
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId())
         .withSuggestion("Retry the request after a few minutes. If the problem persists, contact support.")
         .withDetails(Map.of("retryAfterSeconds", 30));

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(WebhookProcessingException.class)
    public ResponseEntity<ApiErrorResponse> handleWebhookProcessing(
            WebhookProcessingException ex, HttpServletRequest request) {
        log.error("Webhook processing error: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                WEBHOOK_ERROR,
                "Webhook Processing Error",
                ex.getMessage(),
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RefundException.class)
    public ResponseEntity<ApiErrorResponse> handleRefund(
            RefundException ex, HttpServletRequest request) {
        log.error("Refund error: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                REFUND_ERROR,
                "Refund Error",
                ex.getMessage(),
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId())
         .withSuggestion("Verify the payment status is eligible for refund");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ==================== Resilience4j Exceptions ====================

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiErrorResponse> handleCircuitBreakerOpen(
            CallNotPermittedException ex, HttpServletRequest request) {
        log.warn("Circuit breaker is open: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                CIRCUIT_BREAKER_OPEN,
                "Service Temporarily Unavailable",
                "Payment service is experiencing high error rates. Circuit breaker is open.",
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId())
         .withSuggestion("Please retry after 60 seconds")
         .withDetails(Map.of(
                 "circuitBreaker", ex.getCausingCircuitBreakerName(),
                 "retryAfterSeconds", 60
         ));

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimitExceeded(
            RequestNotPermitted ex, HttpServletRequest request) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                RATE_LIMITED,
                "Too Many Requests",
                "Rate limit exceeded. Please slow down your requests.",
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId())
         .withSuggestion("Wait before making more requests")
         .withDetails(Map.of("retryAfterSeconds", 1));

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    @ExceptionHandler(BulkheadFullException.class)
    public ResponseEntity<ApiErrorResponse> handleBulkheadFull(
            BulkheadFullException ex, HttpServletRequest request) {
        log.warn("Bulkhead full: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                BULKHEAD_FULL,
                "Service Busy",
                "Service is at maximum capacity. Please retry.",
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId())
         .withSuggestion("Retry with exponential backoff")
         .withDetails(Map.of("retryAfterSeconds", 5));

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ApiErrorResponse> handleTimeout(
            TimeoutException ex, HttpServletRequest request) {
        log.error("Request timeout: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.GATEWAY_TIMEOUT.value(),
                TIMEOUT_ERROR,
                "Request Timeout",
                "The request took too long to process.",
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId())
         .withSuggestion("Retry the request. If the problem persists, the payment gateway may be slow.");

        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(response);
    }

    // ==================== Validation Exceptions ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        BindingResult result = ex.getBindingResult();

        List<ApiErrorResponse.FieldValidationError> fieldErrors = result.getFieldErrors().stream()
                .map(error -> ApiErrorResponse.FieldValidationError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(error.getRejectedValue())
                        .code(error.getCode())
                        .build())
                .collect(Collectors.toList());

        log.warn("Validation failed for {} fields: {}", fieldErrors.size(), 
                fieldErrors.stream().map(ApiErrorResponse.FieldValidationError::getField).toList());

        ApiErrorResponse response = ApiErrorResponse.withFieldErrors(
                HttpStatus.BAD_REQUEST.value(),
                VALIDATION_ERROR,
                "Validation Failed",
                String.format("Request validation failed for %d field(s)", fieldErrors.size()),
                request.getRequestURI(),
                fieldErrors
        ).withTracing(getTraceId(), getSpanId());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<ApiErrorResponse.FieldValidationError> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> ApiErrorResponse.FieldValidationError.builder()
                        .field(getFieldName(violation))
                        .message(violation.getMessage())
                        .rejectedValue(violation.getInvalidValue())
                        .code(violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName())
                        .build())
                .collect(Collectors.toList());

        log.warn("Constraint violation: {}", fieldErrors);

        ApiErrorResponse response = ApiErrorResponse.withFieldErrors(
                HttpStatus.BAD_REQUEST.value(),
                CONSTRAINT_VIOLATION,
                "Constraint Violation",
                "Request parameters failed validation",
                request.getRequestURI(),
                fieldErrors
        ).withTracing(getTraceId(), getSpanId());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ==================== Request Exceptions ====================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed JSON request: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                BAD_REQUEST,
                "Malformed Request",
                "Request body is not valid JSON or contains invalid values",
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId())
         .withSuggestion("Ensure the request body is valid JSON with correct field types");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing request parameter: {}", ex.getParameterName());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                BAD_REQUEST,
                "Missing Parameter",
                String.format("Required parameter '%s' is missing", ex.getParameterName()),
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Type mismatch for parameter: {}", ex.getName());

        String expectedType = Optional.ofNullable(ex.getRequiredType())
                .map(Class::getSimpleName)
                .orElse("unknown");

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                BAD_REQUEST,
                "Invalid Parameter Type",
                String.format("Parameter '%s' should be of type '%s'", ex.getName(), expectedType),
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Method not allowed: {} for {}", ex.getMethod(), request.getRequestURI());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                METHOD_NOT_ALLOWED,
                "Method Not Allowed",
                String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod()),
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId())
         .withDetails(Map.of("supportedMethods", Optional.ofNullable(ex.getSupportedHttpMethods())
                 .map(methods -> methods.stream().map(m -> m.name()).toList())
                 .orElse(List.of())));

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("Unsupported media type: {}", ex.getContentType());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                UNSUPPORTED_MEDIA,
                "Unsupported Media Type",
                String.format("Content type '%s' is not supported", ex.getContentType()),
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId())
         .withSuggestion("Use 'application/json' content type");

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("No handler found for: {} {}", ex.getHttpMethod(), ex.getRequestURL());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                NOT_FOUND,
                "Endpoint Not Found",
                String.format("No endpoint found for %s %s", ex.getHttpMethod(), ex.getRequestURL()),
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId())
         .withDocumentation("/swagger-ui.html");

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ==================== Database Exceptions ====================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.error("Data integrity violation: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                DATA_INTEGRITY_ERROR,
                "Data Integrity Error",
                "Operation would violate data integrity constraints",
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId())
         .withSuggestion("Check for duplicate entries or invalid references");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDataAccessException(
            DataAccessException ex, HttpServletRequest request) {
        log.error("Database error: {}", ex.getMessage(), ex);

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                DATABASE_ERROR,
                "Database Error",
                "Database is temporarily unavailable",
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId())
         .withSuggestion("Retry the request. If the problem persists, contact support.");

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    // ==================== Security Exceptions ====================

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiErrorResponse> handleSecurity(
            SecurityException ex, HttpServletRequest request) {
        log.error("Security exception: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                SECURITY_ERROR,
                "Access Denied",
                "You do not have permission to perform this action",
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                BAD_REQUEST,
                "Invalid Request",
                ex.getMessage(),
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ==================== Generic Exception Handler ====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ApiErrorResponse response = ApiErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                INTERNAL_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        ).withTracing(getTraceId(), getSpanId())
         .withSuggestion("If this error persists, please contact support with the trace ID");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ==================== Helper Methods ====================

    private String getTraceId() {
        if (tracer == null) {
            return null;
        }
        Span currentSpan = tracer.currentSpan();
        return currentSpan != null ? currentSpan.context().traceId() : null;
    }

    private String getSpanId() {
        if (tracer == null) {
            return null;
        }
        Span currentSpan = tracer.currentSpan();
        return currentSpan != null ? currentSpan.context().spanId() : null;
    }

    private String getFieldName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        return path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
    }
}
