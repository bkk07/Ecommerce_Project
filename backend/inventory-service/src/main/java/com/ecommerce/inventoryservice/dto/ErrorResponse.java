package com.ecommerce.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private String errorCode;      // e.g., "INVENTORY_NOT_FOUND"
    private String errorMessage;
    private LocalDateTime timestamp;
    private String details;        // Specific validation error details
    private String errorId;        // Unique ID for tracking in logs
}
