package com.ecommerce.checkoutservice.openfeign;
import com.ecommerce.checkoutservice.dto.CheckoutItem;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

@FeignClient(name = "INVENTORY-SERVICE")
public interface InventoryClient {
    @PostMapping("/api/inventory/lock")
    void lockStock(@RequestBody List<CheckoutItem> items);

    @PostMapping("/api/inventory/release")
    void releaseStock(@RequestBody List<CheckoutItem> items);
}
