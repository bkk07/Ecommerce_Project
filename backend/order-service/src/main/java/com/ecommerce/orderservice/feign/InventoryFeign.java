package com.ecommerce.orderservice.feign;

import com.ecommerce.inventory.BatchStockLockRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for synchronous inventory operations.
 * Used to lock stock before payment creation to prevent race conditions.
 * Uses Eureka service discovery for load balancing and failover.
 */
@FeignClient(name = "inventory-service")
public interface InventoryFeign {
    
    /**
     * Locks stock for all items in the order atomically (all-or-nothing).
     * This is called synchronously before payment creation.
     * 
     * @param request the batch lock request containing items and orderId
     * @return success message
     * @throws feign.FeignException if lock fails (insufficient stock, SKU not found)
     */
    @PostMapping("/api/v1/inventory/lock")
    String lockStock(@RequestBody BatchStockLockRequest request);
}
