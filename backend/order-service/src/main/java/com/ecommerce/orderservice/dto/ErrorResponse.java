package com.ecommerce.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response format for the Order Service API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Error response returned when an API call fails")
public class ErrorResponse {
    
    @Schema(description = "HTTP status code", example = "400")
    private int status;
    
    @Schema(description = "Error type", example = "Bad Request")
    private String error;
    
    @Schema(description = "Human-readable error message", example = "Order not found with ID: ORD-12345")
    private String message;
    
    @Schema(description = "Additional error details", example = "{\"field\": \"orderId\", \"reason\": \"must not be null\"}")
    private Map<String, Object> details;
    
    @Schema(description = "Timestamp when the error occurred")
    private LocalDateTime timestamp;
    
    @Schema(description = "Request path that caused the error", example = "/api/orders/ORD-12345")
    private String path;
}
