package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.CartItemResponse;
import com.ecommerce.cartservice.dto.CartResponse;
import com.ecommerce.cartservice.client.SearchClient;
import com.ecommerce.cartservice.exception.CartException;
import com.ecommerce.cartservice.model.Cart;
import com.ecommerce.cartservice.model.CartItem;
import com.ecommerce.feigndtos.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final SearchClient searchClient;
    private static final String CART_KEY_PREFIX = "cart:";
    private static final Duration CART_TTL = Duration.ofDays(30);

    // 1. Get Cart
    public CartResponse getCart(String userId) {
        Cart cart = getCartModel(userId);
        return mapToCartResponse(cart);
    }

    // Helper method to get the internal Cart model
    private Cart getCartModel(String userId) {
        String key = CART_KEY_PREFIX + userId;
        Cart cart = (Cart) redisTemplate.opsForValue().get(key);

        if (cart == null) {
            cart = Cart.builder()
                    .userId(userId)
                    .items(new ArrayList<>())
                    .totalAmount(BigDecimal.ZERO)
                    .build();
        }
        return cart;
    }

    // 2. Add Item
    public void addToCart(String userId, String skuCode, Integer quantity, BigDecimal price) {
        String key = CART_KEY_PREFIX + userId;
        Cart cart = getCartModel(userId);

        try {
            Optional<CartItem> existingItem = cart.getItems().stream()
                    .filter(item -> item.getSkuCode().equals(skuCode))
                    .findFirst();

            if (existingItem.isPresent()) {
                // Item exists: Update quantity
                CartItem item = existingItem.get();
                item.setQuantity(item.getQuantity() + quantity);
                if (price != null) {
                    item.setPrice(price);
                }
                item.setSubTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            } else {
                // Item new: Fetch from Product Service
                ProductResponse product = searchClient.getProductBySku(skuCode);

                if (product == null) {
                    throw new CartException("Product not found with SKU: " + skuCode);
                }
                
                BigDecimal itemPrice = (price != null) ? price : product.getPrice();

                CartItem newItem = CartItem.builder()
                        .skuCode(skuCode)
                        .productName(product.getName())
                        .price(itemPrice)
                        .quantity(quantity)
                        .subTotal(itemPrice.multiply(BigDecimal.valueOf(quantity)))
                        .imageUrl(product.getImageUrl())
                        .build();

                cart.getItems().add(newItem);
            }

            calculateTotal(cart);
            redisTemplate.opsForValue().set(key, cart, CART_TTL);

        } catch (Exception e) {
            log.error("Error adding to cart: {}", e.getMessage());
            throw new CartException("Failed to add item. Ensure Product Service is running.");
        }
    }

    // 3. Remove Item
    public void removeFromCart(String userId, String skuCode) {
        String key = CART_KEY_PREFIX + userId;
        Cart cart = getCartModel(userId);

        boolean removed = cart.getItems().removeIf(item -> item.getSkuCode().equals(skuCode));

        if (removed) {
            calculateTotal(cart);
            redisTemplate.opsForValue().set(key, cart, CART_TTL);
        }
    }

    // 4. Clear Cart
    public void clearCart(String userId) {
        String key = CART_KEY_PREFIX + userId;
        redisTemplate.delete(key);
    }

    // 5. Update Item Price
    public void updateItemPrice(String userId, String skuCode, BigDecimal price) {
        String key = CART_KEY_PREFIX + userId;
        Cart cart = getCartModel(userId);

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getSkuCode().equals(skuCode))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setPrice(price);
            item.setSubTotal(price.multiply(BigDecimal.valueOf(item.getQuantity())));
            calculateTotal(cart);
            redisTemplate.opsForValue().set(key, cart, CART_TTL);
        } else {
            throw new CartException("Item not found in cart with SKU: " + skuCode);
        }
    }

    private void calculateTotal(Cart cart) {
        BigDecimal total = cart.getItems().stream()
                .map(CartItem::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalAmount(total);
    }

    private CartResponse mapToCartResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setUserId(cart.getUserId());
        response.setTotalAmount(cart.getTotalAmount());

        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::mapToCartItemResponse)
                .collect(Collectors.toList());
        
        response.setItems(itemResponses);
        return response;
    }

    private CartItemResponse mapToCartItemResponse(CartItem item) {
        CartItemResponse response = new CartItemResponse();
        response.setSkuCode(item.getSkuCode());
        response.setProductName(item.getProductName());
        response.setQuantity(item.getQuantity());
        response.setPrice(item.getPrice());
        response.setSubTotal(item.getSubTotal());
        response.setImageUrl(item.getImageUrl());
        return response;
    }
}