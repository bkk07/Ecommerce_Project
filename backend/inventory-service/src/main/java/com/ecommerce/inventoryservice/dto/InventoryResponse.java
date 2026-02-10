package com.ecommerce.inventoryservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for inventory information.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Inventory information for a product SKU")
public class InventoryResponse {
    
    @Schema(description = "Unique identifier of the inventory record", example = "1")
    private Long id;
    
    @Schema(description = "Stock Keeping Unit code", example = "SKU-LAPTOP-001")
    private String skuCode;
    
    @Schema(description = "Total physical stock quantity", example = "100")
    private Integer quantity;
    
    @Schema(description = "Stock currently reserved for pending orders", example = "10")
    private Integer reservedQuantity;
    
    @Schema(description = "Available stock (quantity - reservedQuantity)", example = "90")
    private Integer availableStock;
    
    @Schema(description = "Whether the product is in stock", example = "true")
    private Boolean inStock;
}
