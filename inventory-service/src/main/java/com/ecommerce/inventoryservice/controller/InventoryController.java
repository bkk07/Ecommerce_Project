package com.ecommerce.inventoryservice.controller;

import com.ecommerce.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> reserveStock(
            @PathVariable String skuCode,
            @RequestParam Integer quantity) {
        log.info("Reserving stock for SKU: {} with quantity: {}", skuCode, quantity);
        inventoryService.reserveStock(skuCode, quantity);
        return ResponseEntity.ok("Stock reserved for SKU: " + skuCode);
    }
}
