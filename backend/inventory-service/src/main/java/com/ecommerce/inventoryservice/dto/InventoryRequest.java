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
 * Request DTO for inventory operations.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request for inventory initialization or update")
public class InventoryRequest {
    
    @NotBlank(message = "SKU code is required")
    @Schema(description = "Stock Keeping Unit code", example = "SKU-LAPTOP-001")
    private String skuCode;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be non-negative")
    @Schema(description = "Stock quantity", example = "100", minimum = "0")
    private Integer quantity;
}
