package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.dto.CartItemPriceUpdate;
import com.ecommerce.cartservice.dto.CartResponse;
import com.ecommerce.cartservice.dto.CartRequest;
import com.ecommerce.cartservice.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@Slf4j
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    // We expect the Gateway to pass the "X-User-Id" header

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CartResponse> getCart(@RequestHeader("X-Auth-User-Id") String userId) {
        return ResponseEntity.ok(cartService.getCart(userId));
    }
    @PostMapping("/add")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> addToCart(
            @RequestHeader("X-Auth-User-Id") String userId,
            @RequestBody CartRequest cartRequest) {
        log.info("I am in addToCart");
        cartService.addToCart(userId, cartRequest.getSkuCode(), cartRequest.getQuantity(), cartRequest.getPrice());
        return ResponseEntity.ok("Item added to cart");
    }
    @DeleteMapping("/remove/{skuCode}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> removeFromCart(
            @RequestHeader("X-Auth-User-Id") String userId,
            @PathVariable String skuCode) {

        cartService.removeFromCart(userId, skuCode);
        return ResponseEntity.ok("Item removed");
    }
    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> clearCart(@RequestHeader("X-Auth-User-Id") String userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok("Cart cleared");

    }

    @PutMapping("/items/price")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> updateItemPrice(
            @RequestHeader("X-Auth-User-Id") String userId,
            @RequestBody CartItemPriceUpdate priceUpdate) {
        cartService.updateItemPrice(userId, priceUpdate.getSkuCode(), priceUpdate.getPrice());
        return ResponseEntity.ok("Item price updated");
    }
}