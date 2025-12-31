package com.ecommerce.checkoutservice.openfeign;
import com.ecommerce.checkoutservice.dto.CheckoutItem;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "CART-SERVICE")
public interface CartClient {
    @GetMapping("/api/cart/{cartId}/items")
    List<CheckoutItem> getCartItems(@PathVariable String cartId);

    @DeleteMapping("/api/cart/{cartId}")
    void clearCart(@PathVariable String cartId);
}
