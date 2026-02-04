package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.dto.CartItemPriceUpdate;
import com.ecommerce.cartservice.dto.CartResponse;
import com.ecommerce.cartservice.dto.CartRequest;
import com.ecommerce.cartservice.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart management endpoints")
public class CartController {

    private final CartService cartService;

    @Operation(summary = "Get user's cart", description = "Retrieves the current user's shopping cart with all items")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart retrieved successfully",
                    content = @Content(schema = @Schema(implementation = CartResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
            @ApiResponse(responseCode = "503", description = "Service unavailable - Redis connection failed")
    })
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CartResponse> getCart(
            @Parameter(description = "User ID from gateway", required = true, hidden = true)
            @RequestHeader("X-Auth-User-Id") String userId) {
        log.info("Getting cart for user: {}", userId);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @Operation(summary = "Add item to cart", description = "Adds a product to the user's shopping cart or updates quantity if already exists")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request - validation failed or product not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
            @ApiResponse(responseCode = "503", description = "Service unavailable - Redis or Product service unavailable")
    })
    @PostMapping("/add")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> addToCart(
            @Parameter(description = "User ID from gateway", required = true, hidden = true)
            @RequestHeader("X-Auth-User-Id") String userId,
            @Valid @RequestBody CartRequest cartRequest) {
        log.info("Adding item {} to cart for user: {}", cartRequest.getSkuCode(), userId);
        cartService.addToCart(userId, cartRequest.getSkuCode(), cartRequest.getQuantity(), cartRequest.getPrice());
        return ResponseEntity.ok("Item added to cart");
    }

    @Operation(summary = "Remove item from cart", description = "Removes a specific product from the user's shopping cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Item removed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
            @ApiResponse(responseCode = "503", description = "Service unavailable - Redis connection failed")
    })
    @DeleteMapping("/remove/{skuCode}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> removeFromCart(
            @Parameter(description = "User ID from gateway", required = true, hidden = true)
            @RequestHeader("X-Auth-User-Id") String userId,
            @Parameter(description = "SKU code of the product to remove", required = true)
            @PathVariable String skuCode) {
        log.info("Removing item {} from cart for user: {}", skuCode, userId);
        cartService.removeFromCart(userId, skuCode);
        return ResponseEntity.ok("Item removed");
    }

    @Operation(summary = "Clear cart", description = "Removes all items from the user's shopping cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cart cleared successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
            @ApiResponse(responseCode = "503", description = "Service unavailable - Redis connection failed")
    })
    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> clearCart(
            @Parameter(description = "User ID from gateway", required = true, hidden = true)
            @RequestHeader("X-Auth-User-Id") String userId) {
        log.info("Clearing cart for user: {}", userId);
        cartService.clearCart(userId);
        return ResponseEntity.ok("Cart cleared");
    }

    @Operation(summary = "Update item price", description = "Updates the price of a specific item in the cart")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Price updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request - validation failed or item not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated"),
            @ApiResponse(responseCode = "503", description = "Service unavailable - Redis connection failed")
    })
    @PutMapping("/items/price")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> updateItemPrice(
            @Parameter(description = "User ID from gateway", required = true, hidden = true)
            @RequestHeader("X-Auth-User-Id") String userId,
            @Valid @RequestBody CartItemPriceUpdate priceUpdate) {
        log.info("Updating price for item {} in cart for user: {}", priceUpdate.getSkuCode(), userId);
        cartService.updateItemPrice(userId, priceUpdate.getSkuCode(), priceUpdate.getPrice());
        return ResponseEntity.ok("Item price updated");
    }
}