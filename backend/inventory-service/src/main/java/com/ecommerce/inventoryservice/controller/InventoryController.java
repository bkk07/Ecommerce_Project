package com.ecommerce.inventoryservice.controller;

import com.ecommerce.inventory.StockItem;
import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.service.InventoryService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for inventory management operations.
 * Provides endpoints for stock queries, updates, reservations, and releases.
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Inventory", description = "Inventory management API for stock operations")
@SecurityRequirement(name = "gateway-auth")
public class InventoryController {
    
    private final InventoryService inventoryService;

    // ==================== READ OPERATIONS ====================

    @GetMapping("/{skuCode}")
    @Operation(
            summary = "Get inventory by SKU code",
            description = "Retrieves current stock information for a specific product SKU"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Inventory found",
                    content = @Content(schema = @Schema(implementation = InventoryResponse.class))),
            @ApiResponse(responseCode = "404", description = "SKU not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @RateLimiter(name = "inventoryRead")
    public ResponseEntity<InventoryResponse> getInventory(
            @Parameter(description = "SKU code of the product", example = "SKU-LAPTOP-001")
            @PathVariable @NotBlank String skuCode) {
        log.info("Getting inventory for SKU: {}", skuCode);
        return inventoryService.getInventoryBySkuCode(skuCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(
            summary = "Get all inventory",
            description = "Retrieves stock information for all products"
    )
    @ApiResponse(responseCode = "200", description = "List of all inventory items")
    @RateLimiter(name = "inventoryRead")
    public ResponseEntity<List<InventoryResponse>> getAllInventory() {
        log.info("Getting all inventory items");
        return ResponseEntity.ok(inventoryService.getAllInventory());
    }

    @GetMapping("/batch")
    @Operation(
            summary = "Get inventory for multiple SKUs",
            description = "Retrieves stock information for a list of product SKUs"
    )
    @ApiResponse(responseCode = "200", description = "List of inventory items for requested SKUs")
    @RateLimiter(name = "inventoryRead")
    public ResponseEntity<List<InventoryResponse>> getInventoryBatch(
            @Parameter(description = "List of SKU codes", example = "[\"SKU-001\", \"SKU-002\"]")
            @RequestParam List<String> skuCodes) {
        log.info("Getting inventory for {} SKUs", skuCodes.size());
        return ResponseEntity.ok(inventoryService.getInventoryBySkuCodes(skuCodes));
    }

    @GetMapping("/{skuCode}/in-stock")
    @Operation(
            summary = "Check if SKU is in stock",
            description = "Returns true if the product has available stock"
    )
    @ApiResponse(responseCode = "200", description = "Stock availability status")
    @RateLimiter(name = "inventoryRead")
    public ResponseEntity<Boolean> isInStock(
            @Parameter(description = "SKU code of the product")
            @PathVariable @NotBlank String skuCode) {
        log.info("Checking stock availability for SKU: {}", skuCode);
        return ResponseEntity.ok(inventoryService.isInStock(skuCode));
    }

    // ==================== ADMIN OPERATIONS ====================

    @PostMapping("/init/{skuCode}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Initialize stock for new SKU",
            description = "Creates a new inventory record with zero stock for a product SKU. Admin only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Stock initialized successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied - Admin role required")
    })
    public ResponseEntity<String> initStock(
            @Parameter(description = "SKU code for the new product")
            @PathVariable @NotBlank String skuCode) {
        log.info("Initializing stock for SKU: {}", skuCode);
        inventoryService.initStock(skuCode);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Stock initialized for SKU: " + skuCode);
    }

    @PutMapping("/update/{skuCode}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update stock quantity",
            description = "Sets the absolute stock quantity for a SKU. Admin only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid quantity"),
            @ApiResponse(responseCode = "404", description = "SKU not found")
    })
    public ResponseEntity<String> updateStock(
            @Parameter(description = "SKU code of the product")
            @PathVariable @NotBlank String skuCode,
            @Parameter(description = "New stock quantity (must be >= 0)")
            @RequestParam @Min(value = 0, message = "Quantity must be non-negative") Integer quantity) {
        log.info("Updating stock for SKU: {} with quantity: {}", skuCode, quantity);
        inventoryService.updateStock(skuCode, quantity);
        return ResponseEntity.ok("Stock updated for SKU: " + skuCode);
    }

    // ==================== RESERVATION OPERATIONS ====================

    @PostMapping("/reserve/{skuCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(
            summary = "Reserve stock for an order",
            description = "Temporarily reserves stock for an order during checkout. Idempotent."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock reserved successfully"),
            @ApiResponse(responseCode = "400", description = "Insufficient stock or invalid request"),
            @ApiResponse(responseCode = "404", description = "SKU not found"),
            @ApiResponse(responseCode = "409", description = "Concurrency conflict - retry")
    })
    public ResponseEntity<String> reserveStock(
            @Parameter(description = "SKU code of the product")
            @PathVariable @NotBlank String skuCode,
            @Parameter(description = "Quantity to reserve (must be >= 1)")
            @RequestParam @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
            @Parameter(description = "Order ID for tracking")
            @RequestParam @NotBlank String orderId) {
        log.info("Reserving stock for SKU: {} with quantity: {} for Order: {}", skuCode, quantity, orderId);
        inventoryService.reserveStock(skuCode, quantity, orderId);
        return ResponseEntity.ok("Stock reserved for SKU: " + skuCode);
    }

    @PostMapping("/release/{skuCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(
            summary = "Release reserved stock",
            description = "Releases previously reserved stock when an order is cancelled. Idempotent."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock released successfully"),
            @ApiResponse(responseCode = "404", description = "SKU not found")
    })
    public ResponseEntity<String> releaseStock(
            @Parameter(description = "SKU code of the product")
            @PathVariable @NotBlank String skuCode,
            @Parameter(description = "Quantity to release")
            @RequestParam @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
            @Parameter(description = "Order ID for tracking")
            @RequestParam @NotBlank String orderId) {
        log.info("Releasing stock for SKU: {} with quantity: {} for Order: {}", skuCode, quantity, orderId);
        inventoryService.releaseStock(skuCode, quantity, orderId);
        return ResponseEntity.ok("Stock released for SKU: " + skuCode);
    }

    // ==================== BATCH OPERATIONS ====================

    @PostMapping("/lock")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(
            summary = "Lock stock for multiple items",
            description = "Atomically reserves stock for all items in an order. All-or-nothing."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock locked for all items"),
            @ApiResponse(responseCode = "400", description = "Insufficient stock for one or more items")
    })
    public ResponseEntity<String> lockStock(
            @Valid @RequestBody BatchStockRequest request) {
        log.info("Locking stock for order: {} with {} items", request.getOrderId(), request.getItems().size());
        inventoryService.reserveStock(request.getItems(), request.getOrderId());
        return ResponseEntity.ok("Stock locked for order: " + request.getOrderId());
    }

    @PostMapping("/release")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(
            summary = "Release stock for multiple items",
            description = "Releases reserved stock for all items in an order."
    )
    @ApiResponse(responseCode = "200", description = "Stock released for all items")
    public ResponseEntity<String> releaseStock(
            @Valid @RequestBody BatchStockRequest request) {
        log.info("Releasing stock for order: {} with {} items", request.getOrderId(), request.getItems().size());
        inventoryService.releaseStock(request.getItems(), request.getOrderId());
        return ResponseEntity.ok("Stock released for order: " + request.getOrderId());
    }
}
