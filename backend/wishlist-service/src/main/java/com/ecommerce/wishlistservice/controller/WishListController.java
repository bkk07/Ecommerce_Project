package com.ecommerce.wishlistservice.controller;

import com.ecommerce.wishlistservice.dto.request.AddWishListItemRequest;
import com.ecommerce.wishlistservice.dto.response.ItemExistsResponse;
import com.ecommerce.wishlistservice.dto.response.MessageResponse;
import com.ecommerce.wishlistservice.dto.response.WishListItemResponse;
import com.ecommerce.wishlistservice.dto.response.WishListResponse;
import com.ecommerce.wishlistservice.service.WishListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishListController {

    private final WishListService wishListService;

    /**
     * Get the logged-in user's wishlist
     */
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WishListResponse> getWishList() {
        Long userId = getAuthenticatedUserId();
        log.info("GET wishlist request for user: {}", userId);
        return ResponseEntity.ok(wishListService.getWishList(userId));
    }

    /**
     * Add item to wishlist by SKU code
     */
    @PostMapping("/items")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WishListItemResponse> addItem(
            @Valid @RequestBody AddWishListItemRequest request) {
        Long userId = getAuthenticatedUserId();
        log.info("POST add item to wishlist for user: {}, SKU: {}", userId, request.getSkuCode());
        WishListItemResponse response = wishListService.addItem(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Remove item from wishlist by SKU code
     */
    @DeleteMapping("/items/{skuCode}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MessageResponse> removeItem(@PathVariable String skuCode) {
        Long userId = getAuthenticatedUserId();
        log.info("DELETE item from wishlist for user: {}, SKU: {}", userId, skuCode);
        wishListService.removeItem(userId, skuCode);
        return ResponseEntity.ok(MessageResponse.success("Item removed from wishlist"));
    }

    /**
     * Check if item exists in wishlist by SKU code
     */
    @GetMapping("/items/{skuCode}/exists")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ItemExistsResponse> checkItemExists(@PathVariable String skuCode) {
        Long userId = getAuthenticatedUserId();
        log.info("GET check item exists in wishlist for user: {}, SKU: {}", userId, skuCode);
        return ResponseEntity.ok(wishListService.itemExists(userId, skuCode));
    }

    /**
     * Move item from wishlist to cart
     * Removes item from wishlist and returns item details for cart addition
     */
    @PostMapping("/{skuCode}/move-to-cart")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<WishListItemResponse> moveToCart(@PathVariable String skuCode) {
        Long userId = getAuthenticatedUserId();
        log.info("POST move item to cart for user: {}, SKU: {}", userId, skuCode);
        WishListItemResponse response = wishListService.moveToCart(userId, skuCode);
        return ResponseEntity.ok(response);
    }

    /**
     * Clear all items from wishlist
     */
    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MessageResponse> clearWishList() {
        Long userId = getAuthenticatedUserId();
        log.info("DELETE clear wishlist for user: {}", userId);
        wishListService.clearWishList(userId);
        return ResponseEntity.ok(MessageResponse.success("Wishlist cleared"));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Wishlist service is running");
    }

    /**
     * Extract userId from SecurityContext (set by GatewayHeaderFilter)
     * The API Gateway validates JWT and forwards X-Auth-User-Id header
     */
    private Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        String userIdStr = authentication.getPrincipal().toString();
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid user ID format: " + userIdStr);
        }
    }
}
