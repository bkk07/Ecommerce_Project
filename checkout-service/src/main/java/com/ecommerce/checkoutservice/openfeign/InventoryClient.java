package com.ecommerce.checkoutservice.openfeign;
import com.ecommerce.checkout.CheckoutItem;
import com.ecommerce.inventory.StockItem;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

@FeignClient(name = "INVENTORY-SERVICE")
public interface InventoryClient {
    @PostMapping("/api/v1/inventory/lock")
    void lockStock(@RequestBody List<StockItem> items);
    @PostMapping("/api/v1/inventory/release")
    void releaseStock(@RequestBody List<StockItem> items);
}
