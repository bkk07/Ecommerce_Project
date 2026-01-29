package com.ecommerce.checkoutservice.openfeign;
import com.ecommerce.cart.CartResponse;
import com.ecommerce.checkout.CheckoutItem;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@FeignClient(name = "CART-SERVICE")
public interface CartClient {
    @DeleteMapping("/api/v1/cart/clear")
    void clearCart();
    @GetMapping("/api/v1/cart")
    CartResponse getCart();
}
