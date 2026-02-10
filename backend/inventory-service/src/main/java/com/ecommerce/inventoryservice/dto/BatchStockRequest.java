package com.ecommerce.inventoryservice.dto;

import com.ecommerce.inventory.StockItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for batch stock operations.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request for batch stock lock/release operations")
public class BatchStockRequest {
    
    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    @Schema(description = "List of stock items to lock/release")
    private List<StockItem> items;
    
    @NotBlank(message = "Order ID is required")
    @Schema(description = "Order ID for tracking the reservation", example = "ORD-2026-001")
    private String orderId;
}
