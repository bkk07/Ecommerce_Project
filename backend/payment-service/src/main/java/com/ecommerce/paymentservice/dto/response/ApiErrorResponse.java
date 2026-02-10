package com.ecommerce.paymentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Standardized API error response with comprehensive details
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized error response for API errors")
public class ApiErrorResponse {

    @Schema(description = "Error response timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "Error type/category", example = "VALIDATION_ERROR")
    private String errorCode;

    @Schema(description = "Human-readable error title", example = "Validation Failed")
    private String error;

    @Schema(description = "Detailed error message", example = "Request validation failed for 2 fields")
    private String message;

    @Schema(description = "Request path that caused the error", example = "/api/payments/verify")
    private String path;

    @Schema(description = "Trace ID for distributed tracing", example = "abc123def456")
    private String traceId;

    @Schema(description = "Span ID for distributed tracing", example = "span789")
    private String spanId;

    @Schema(description = "Validation field errors")
    private List<FieldValidationError> fieldErrors;

    @Schema(description = "Additional error details")
    private Map<String, Object> details;

    @Schema(description = "Suggested action to resolve the error")
    private String suggestion;

    @Schema(description = "Link to documentation for this error")
    private String documentationUrl;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "Field-level validation error details")
    public static class FieldValidationError {

        @Schema(description = "Field name that failed validation", example = "razorpayOrderId")
        private String field;

        @Schema(description = "Validation error message", example = "Razorpay Order ID is required")
        private String message;

        @Schema(description = "Value that was rejected", example = "null")
        private Object rejectedValue;

        @Schema(description = "Validation constraint that failed", example = "NotBlank")
        private String code;
    }

    // Factory methods
    public static ApiErrorResponse of(int status, String errorCode, String error, String message, String path) {
        return ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .errorCode(errorCode)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }

    public static ApiErrorResponse withFieldErrors(int status, String errorCode, String error, 
                                                    String message, String path, 
                                                    List<FieldValidationError> fieldErrors) {
        return ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .errorCode(errorCode)
                .error(error)
                .message(message)
                .path(path)
                .fieldErrors(fieldErrors)
                .build();
    }

    public ApiErrorResponse withTracing(String traceId, String spanId) {
        this.traceId = traceId;
        this.spanId = spanId;
        return this;
    }

    public ApiErrorResponse withSuggestion(String suggestion) {
        this.suggestion = suggestion;
        return this;
    }

    public ApiErrorResponse withDocumentation(String url) {
        this.documentationUrl = url;
        return this;
    }

    public ApiErrorResponse withDetails(Map<String, Object> details) {
        this.details = details;
        return this;
    }
}
