package com.ecommerce.inventoryservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for reserving or releasing stock.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request to reserve or release stock for an order")
public class StockReservationRequest {
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Schema(description = "Quantity to reserve/release", example = "5", minimum = "1")
    private Integer quantity;
    
    @NotBlank(message = "Order ID is required")
    @Schema(description = "Order ID for tracking the reservation", example = "ORD-2026-001")
    private String orderId;
}
