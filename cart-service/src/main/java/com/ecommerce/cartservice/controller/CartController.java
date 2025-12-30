package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.dto.CartRequest;
import com.ecommerce.cartservice.model.Cart;
import com.ecommerce.cartservice.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // We expect the Gateway to pass the "X-User-Id" header
    @GetMapping
    public ResponseEntity<Cart> getCart(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/add")
    public ResponseEntity<String> addToCart(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody CartRequest cartRequest) {

        cartService.addToCart(userId, cartRequest.getSkuCode(), cartRequest.getQuantity());
        return ResponseEntity.ok("Item added to cart");
    }

    @DeleteMapping("/remove/{skuCode}")
    public ResponseEntity<String> removeFromCart(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String skuCode) {

        cartService.removeFromCart(userId, skuCode);
        return ResponseEntity.ok("Item removed");
    }
    @DeleteMapping("/clear")
    public ResponseEntity<String> clearCart(@RequestHeader("X-User-Id") String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok("Cart cleared");
    }
}