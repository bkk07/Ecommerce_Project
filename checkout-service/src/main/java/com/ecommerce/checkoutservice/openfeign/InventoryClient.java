package com.ecommerce.checkoutservice.openfeign;

import com.ecommerce.inventory.StockItem;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "INVENTORY-SERVICE")
public interface InventoryClient {
    // Methods removed as inventory lock/release is now handled asynchronously via Kafka
}
