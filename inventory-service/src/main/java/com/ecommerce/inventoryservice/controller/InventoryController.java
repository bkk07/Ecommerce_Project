package com.ecommerce.inventoryservice.controller;

import com.ecommerce.inventory.StockItem;
import com.ecommerce.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {
    private final InventoryService inventoryService;
    @PostMapping("/init/{skuCode}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> initStock(@PathVariable String skuCode) {
        log.info("Initializing stock for SKU: {}", skuCode);
        inventoryService.initStock(skuCode);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Stock initialized for SKU: " + skuCode);
    }
    @PutMapping("/update/{skuCode}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateStock(
            @PathVariable String skuCode,
            @RequestParam Integer quantity) {
        log.info("Updating stock for SKU: {} with quantity: {}", skuCode, quantity);
        inventoryService.updateStock(skuCode, quantity);
        return ResponseEntity.ok("Stock updated for SKU: " + skuCode);
    }
    @PostMapping("/reserve/{skuCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<String> reserveStock(
            @PathVariable String skuCode,
            @RequestParam Integer quantity,
            @RequestParam String orderId) { // Change: Order ID must come from the caller
        log.info("Reserving stock for SKU: {} with quantity: {} for Order: {}", skuCode, quantity, orderId);
        inventoryService.reserveStock(skuCode, quantity, orderId);
        return ResponseEntity.ok("Stock reserved for SKU: " + skuCode);
    }
    @PostMapping("/release/{skuCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<String> releaseStock(
            @PathVariable String skuCode,
            @RequestParam Integer quantity,
            @RequestParam String orderId) { // Change: Order ID must come from the caller
        log.info("Releasing stock for SKU: {} with quantity: {} for Order: {}", skuCode, quantity, orderId);
        inventoryService.releaseStock(skuCode, quantity, orderId);
        return ResponseEntity.ok("Stock released for SKU: " + skuCode);
    }
    @PostMapping("/lock")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<String> lockStock(@RequestBody List<StockItem> items, @RequestParam String orderId){
        // Ensure transactional all-or-nothing behavior for batch operations
        inventoryService.reserveStock(items, orderId);
        return ResponseEntity.ok("Stock locked for order: " + orderId);
    }
    @PostMapping("/release")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<String> releaseStock(@RequestBody List<StockItem> items, @RequestParam String orderId){
        // Ensure transactional all-or-nothing behavior for batch operations
        inventoryService.releaseStock(items, orderId);
        return ResponseEntity.ok("Stock released for order: " + orderId);
    }
}
