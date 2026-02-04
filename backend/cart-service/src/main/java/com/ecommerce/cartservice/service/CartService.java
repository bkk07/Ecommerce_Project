package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.CartItemResponse;
import com.ecommerce.cartservice.dto.CartResponse;
import com.ecommerce.cartservice.client.SearchClient;
import com.ecommerce.cartservice.exception.CartException;
import com.ecommerce.cartservice.model.Cart;
import com.ecommerce.cartservice.model.CartItem;
import com.ecommerce.feigndtos.ProductResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
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
    private static final int MAX_RETRY_ATTEMPTS = 3;

    // 1. Get Cart
    @CircuitBreaker(name = "redis", fallbackMethod = "getCartFallback")
    @Retry(name = "redis")
    public CartResponse getCart(String userId) {
        Cart cart = getCartModel(userId);
        return mapToCartResponse(cart);
    }

    // Fallback for getCart when Redis is unavailable
    private CartResponse getCartFallback(String userId, Exception e) {
        log.warn("Fallback triggered for getCart. User: {}, Error: {}", userId, e.getMessage());
        CartResponse emptyCart = new CartResponse();
        emptyCart.setUserId(userId);
        emptyCart.setItems(new ArrayList<>());
        emptyCart.setTotalAmount(BigDecimal.ZERO);
        return emptyCart;
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

    // 2. Add Item with optimistic locking
    @CircuitBreaker(name = "cartModification", fallbackMethod = "addToCartFallback")
    @RateLimiter(name = "cartModification")
    @Retry(name = "redis")
    public void addToCart(String userId, String skuCode, Integer quantity, BigDecimal price) {
        String key = CART_KEY_PREFIX + userId;
        
        // Fetch product info outside of transaction (if needed)
        ProductResponse product = null;
        Cart existingCart = getCartModel(userId);
        boolean itemExists = existingCart.getItems().stream()
                .anyMatch(item -> item.getSkuCode().equals(skuCode));
        
        if (!itemExists) {
            product = fetchProductWithResilience(skuCode);
        }
        
        final ProductResponse finalProduct = product;
        
        // Execute with retry for optimistic locking
        executeWithRetry(key, () -> {
            return redisTemplate.execute(new SessionCallback<Boolean>() {
                @Override
                @SuppressWarnings("unchecked")
                public Boolean execute(RedisOperations operations) throws DataAccessException {
                    operations.watch(key);
                    
                    Cart cart = (Cart) operations.opsForValue().get(key);
                    if (cart == null) {
                        cart = Cart.builder()
                                .userId(userId)
                                .items(new ArrayList<>())
                                .totalAmount(BigDecimal.ZERO)
                                .build();
                    }
                    
                    Optional<CartItem> existingItem = cart.getItems().stream()
                            .filter(item -> item.getSkuCode().equals(skuCode))
                            .findFirst();

                    if (existingItem.isPresent()) {
                        CartItem item = existingItem.get();
                        item.setQuantity(item.getQuantity() + quantity);
                        if (price != null) {
                            item.setPrice(price);
                        }
                        item.setSubTotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                    } else {
                        BigDecimal itemPrice = (price != null) ? price : finalProduct.getPrice();
                        CartItem newItem = CartItem.builder()
                                .skuCode(skuCode)
                                .productName(finalProduct.getName())
                                .price(itemPrice)
                                .quantity(quantity)
                                .subTotal(itemPrice.multiply(BigDecimal.valueOf(quantity)))
                                .imageUrl(finalProduct.getImageUrl())
                                .build();
                        cart.getItems().add(newItem);
                    }

                    calculateTotal(cart);
                    
                    operations.multi();
                    operations.opsForValue().set(key, cart, CART_TTL);
                    List<Object> results = operations.exec();
                    
                    return results != null && !results.isEmpty();
                }
            });
        });
        
        log.info("Item {} added to cart for user {}", skuCode, userId);
    }

    // Fallback for addToCart when service is unavailable
    private void addToCartFallback(String userId, String skuCode, Integer quantity, BigDecimal price, Exception e) {
        log.error("Fallback triggered for addToCart. User: {}, SKU: {}, Error: {}", userId, skuCode, e.getMessage());
        throw new CartException("Cart service is temporarily unavailable. Please try again later.");
    }

    // Fetch product with circuit breaker and retry
    @CircuitBreaker(name = "searchService", fallbackMethod = "fetchProductFallback")
    @Retry(name = "externalService")
    private ProductResponse fetchProductWithResilience(String skuCode) {
        log.debug("Fetching product info for SKU: {}", skuCode);
        ProductResponse product = searchClient.getProductBySku(skuCode);
        if (product == null) {
            throw new CartException("Product not found with SKU: " + skuCode);
        }
        return product;
    }

    // Fallback for search service
    private ProductResponse fetchProductFallback(String skuCode, Exception e) {
        log.warn("Search service unavailable for SKU: {}. Error: {}", skuCode, e.getMessage());
        throw new CartException("Unable to fetch product details. Search service is temporarily unavailable.");
    }

    // 3. Remove Item with optimistic locking
    @CircuitBreaker(name = "cartModification", fallbackMethod = "removeFromCartFallback")
    @RateLimiter(name = "cartModification")
    @Retry(name = "redis")
    public void removeFromCart(String userId, String skuCode) {
        String key = CART_KEY_PREFIX + userId;
        
        executeWithRetry(key, () -> {
            return redisTemplate.execute(new SessionCallback<Boolean>() {
                @Override
                @SuppressWarnings("unchecked")
                public Boolean execute(RedisOperations operations) throws DataAccessException {
                    operations.watch(key);
                    
                    Cart cart = (Cart) operations.opsForValue().get(key);
                    if (cart == null) {
                        operations.unwatch();
                        return true; // Nothing to remove
                    }
                    
                    boolean removed = cart.getItems().removeIf(item -> item.getSkuCode().equals(skuCode));
                    
                    if (removed) {
                        calculateTotal(cart);
                        operations.multi();
                        operations.opsForValue().set(key, cart, CART_TTL);
                        List<Object> results = operations.exec();
                        return results != null && !results.isEmpty();
                    }
                    
                    operations.unwatch();
                    return true;
                }
            });
        });
        
        log.info("Item {} removed from cart for user {}", skuCode, userId);
    }

    // Fallback for removeFromCart
    private void removeFromCartFallback(String userId, String skuCode, Exception e) {
        log.error("Fallback triggered for removeFromCart. User: {}, SKU: {}, Error: {}", userId, skuCode, e.getMessage());
        throw new CartException("Unable to remove item from cart. Please try again later.");
    }

    // 4. Clear Cart
    @CircuitBreaker(name = "redis", fallbackMethod = "clearCartFallback")
    @Retry(name = "redis")
    public void clearCart(String userId) {
        String key = CART_KEY_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("Cart cleared for user {}", userId);
    }

    // Fallback for clearCart
    private void clearCartFallback(String userId, Exception e) {
        log.error("Fallback triggered for clearCart. User: {}, Error: {}", userId, e.getMessage());
        throw new CartException("Unable to clear cart. Please try again later.");
    }

    // 5. Update Item Price with optimistic locking
    @CircuitBreaker(name = "cartModification", fallbackMethod = "updateItemPriceFallback")
    @RateLimiter(name = "cartModification")
    @Retry(name = "redis")
    public void updateItemPrice(String userId, String skuCode, BigDecimal price) {
        String key = CART_KEY_PREFIX + userId;
        
        executeWithRetry(key, () -> {
            return redisTemplate.execute(new SessionCallback<Boolean>() {
                @Override
                @SuppressWarnings("unchecked")
                public Boolean execute(RedisOperations operations) throws DataAccessException {
                    operations.watch(key);
                    
                    Cart cart = (Cart) operations.opsForValue().get(key);
                    if (cart == null) {
                        operations.unwatch();
                        throw new CartException("Cart not found for user: " + userId);
                    }
                    
                    Optional<CartItem> existingItem = cart.getItems().stream()
                            .filter(item -> item.getSkuCode().equals(skuCode))
                            .findFirst();

                    if (existingItem.isPresent()) {
                        CartItem item = existingItem.get();
                        item.setPrice(price);
                        item.setSubTotal(price.multiply(BigDecimal.valueOf(item.getQuantity())));
                        calculateTotal(cart);
                        
                        operations.multi();
                        operations.opsForValue().set(key, cart, CART_TTL);
                        List<Object> results = operations.exec();
                        return results != null && !results.isEmpty();
                    } else {
                        operations.unwatch();
                        throw new CartException("Item not found in cart with SKU: " + skuCode);
                    }
                }
            });
        });
        
        log.info("Price updated for item {} in cart for user {}", skuCode, userId);
    }

    // Fallback for updateItemPrice
    private void updateItemPriceFallback(String userId, String skuCode, BigDecimal price, Exception e) {
        log.error("Fallback triggered for updateItemPrice. User: {}, SKU: {}, Error: {}", userId, skuCode, e.getMessage());
        throw new CartException("Unable to update item price. Please try again later.");
    }

    // Helper method for retry logic with optimistic locking
    private void executeWithRetry(String key, java.util.function.Supplier<Boolean> operation) {
        int attempts = 0;
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                Boolean success = operation.get();
                if (success != null && success) {
                    return;
                }
                attempts++;
                log.warn("Optimistic lock failed for key {}, attempt {}/{}", key, attempts, MAX_RETRY_ATTEMPTS);
            } catch (CartException e) {
                throw e; // Don't retry business exceptions
            } catch (Exception e) {
                attempts++;
                log.warn("Redis operation failed for key {}, attempt {}/{}: {}", key, attempts, MAX_RETRY_ATTEMPTS, e.getMessage());
            }
        }
        throw new CartException("Failed to update cart after " + MAX_RETRY_ATTEMPTS + " attempts. Please try again.");
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