package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.client.ProductClient;
import com.ecommerce.cartservice.dto.ProductResponse;
import com.ecommerce.cartservice.exception.CartException;
import com.ecommerce.cartservice.model.Cart;
import com.ecommerce.cartservice.model.CartItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductClient productClient;

    private static final String CART_KEY_PREFIX = "cart:";
    private static final Duration CART_TTL = Duration.ofDays(30);

    // 1. Get Cart
    public Cart getCart(String userId) {
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
    public void addToCart(String userId, String skuCode, Integer quantity) {
        String key = CART_KEY_PREFIX + userId;
        Cart cart = getCart(userId);

        try {
            Optional<CartItem> existingItem = cart.getItems().stream()
                    .filter(item -> item.getSkuCode().equals(skuCode))
                    .findFirst();

            if (existingItem.isPresent()) {
                // Item exists: Update quantity
                CartItem item = existingItem.get();
                item.setQuantity(item.getQuantity() + quantity);
                item.setSubTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            } else {
                // Item new: Fetch from Product Service
                ProductResponse product = productClient.getProductBySku(skuCode);

                if (product == null) {
                    throw new CartException("Product not found with SKU: " + skuCode);
                }

                CartItem newItem = CartItem.builder()
                        .skuCode(skuCode)
                        .productName(product.getName())
                        .price(product.getPrice())
                        .quantity(quantity)
                        .subTotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
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
        Cart cart = getCart(userId);

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

    private void calculateTotal(Cart cart) {
        BigDecimal total = cart.getItems().stream()
                .map(CartItem::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setTotalAmount(total);
    }
}