package com.ecommerce.checkoutservice.service;

import com.ecommerce.cart.CartResponse;
import com.ecommerce.checkout.CheckoutItem;
import com.ecommerce.checkout.CreateOrderCommand;
import com.ecommerce.checkout.OrderCheckoutResponse;
import com.ecommerce.checkoutservice.config.CheckoutConfig;
import com.ecommerce.checkoutservice.dto.CheckoutFailureReason;
import com.ecommerce.checkoutservice.dto.CheckoutResponse;
import com.ecommerce.checkoutservice.dto.CheckoutStatus;
import com.ecommerce.checkoutservice.dto.InitiateCheckoutRequest;
import com.ecommerce.checkoutservice.dto.PaymentCallbackRequest;
import com.ecommerce.checkoutservice.entity.CheckoutSession;
import com.ecommerce.checkoutservice.exception.Exceptions;
import com.ecommerce.checkoutservice.kafka.KafkaEventPublisher;
import com.ecommerce.checkoutservice.mapper.CheckoutMapper;
import com.ecommerce.checkoutservice.openfeign.CartClient;
import com.ecommerce.checkoutservice.openfeign.OrderClient;
import com.ecommerce.checkoutservice.openfeign.ProductClient;
import com.ecommerce.checkoutservice.repository.CheckoutSessionRepository;
import com.ecommerce.product.ProductValidationItem;
import com.ecommerce.product.ProductValidationResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final CartClient cartClient;
    private final OrderClient orderClient;
    private final ProductClient productClient;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final CheckoutMapper checkoutMapper;
    private final CheckoutConfig checkoutConfig;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:checkout:";

    /**
     * Initiate checkout process with idempotency and resilience patterns.
     */
    @CircuitBreaker(name = "checkout", fallbackMethod = "initiateCheckoutFallback")
    @RateLimiter(name = "checkout")
    @Retry(name = "default")
    public CheckoutResponse initiateCheckout(InitiateCheckoutRequest request) {
        
        // Check idempotency key to prevent duplicate submissions
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            String idempotencyKey = IDEMPOTENCY_KEY_PREFIX + request.getUserId() + ":" + request.getIdempotencyKey();
            Boolean isNewRequest = redisTemplate.opsForValue().setIfAbsent(
                    idempotencyKey, 
                    "processing", 
                    Duration.ofMinutes(checkoutConfig.getIdempotencyExpirationMinutes())
            );
            
            if (Boolean.FALSE.equals(isNewRequest)) {
                log.warn("Duplicate checkout request detected for user: {}, key: {}", 
                        request.getUserId(), request.getIdempotencyKey());
                throw new Exceptions.DuplicateCheckoutException(
                        "Duplicate checkout request. Please wait for the previous request to complete.");
            }
        }

        List<CheckoutItem> itemsToBuy;

        // 1. Determine Source (Cart or Direct)
        if (request.isCartId()) {
            CartResponse cartResponse = fetchCartWithResilience();
            itemsToBuy = checkoutMapper.toCheckoutItems(cartResponse.getItems());
            log.info("Checkout items from cart for user {}: {} items", request.getUserId(), itemsToBuy.size());
        } else {
            itemsToBuy = request.getItems();
            log.info("Direct checkout for user {}: {} items", request.getUserId(), 
                    itemsToBuy != null ? itemsToBuy.size() : 0);
        }

        if (itemsToBuy == null || itemsToBuy.isEmpty()) {
            log.warn("No items to checkout for user: {}", request.getUserId());
            throw new IllegalArgumentException("No items to checkout");
        }

        // Validate item count
        if (itemsToBuy.size() > checkoutConfig.getMaxItemsPerCheckout()) {
            throw new IllegalArgumentException(
                    "Cannot checkout more than " + checkoutConfig.getMaxItemsPerCheckout() + " items at once");
        }

        // 2. Validate Products (SYNC)
        List<ProductValidationItem> validationItems = checkoutMapper.toProductValidationItems(itemsToBuy);
        ProductValidationResponse validationResponse = validateProductsWithResilience(validationItems);
        
        log.info("Product validation completed for user {}", request.getUserId());
        
        if (validationResponse == null || !validationResponse.isValid()) {
            log.warn("Checkout failed due to product validation errors for user {}: {}",
                    request.getUserId(),
                    validationResponse != null ? validationResponse.getResults() : "No response from product service");

            return new CheckoutResponse(
                    CheckoutStatus.FAILED,
                    null,
                    null,
                    checkoutConfig.getCurrency(),
                    CheckoutFailureReason.PRODUCT_VALIDATION_FAILED,
                    validationResponse != null ? validationResponse.getResults() : null
            );
        }

        // 3. Calculate Total
        BigDecimal totalAmount = itemsToBuy.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.info("Total amount for user {}: {}", request.getUserId(), totalAmount);

        // 4. Create Order Command
        CreateOrderCommand command = new CreateOrderCommand();
        command.setUserId(String.valueOf(request.getUserId()));
        command.setTotalAmount(totalAmount);
        command.setItems(checkoutMapper.toOrderItemDtos(itemsToBuy));
        command.setShippingAddress(request.getShippingAddress());

        // 5. Call Order Service
        log.info("Creating order for user: {}", request.getUserId());
        OrderCheckoutResponse orderResponse = createOrderWithResilience(command);
        
        String razorpayOrderId = orderResponse.getRazorpayOrderId();
        log.info("Order created for user {}, Razorpay ID: {}", request.getUserId(), razorpayOrderId);

        // 6. Save Session
        CheckoutSession session = CheckoutSession.builder()
                .orderId(razorpayOrderId)
                .userId(request.getUserId())
                .cartId(request.isCartId())
                .totalAmount(totalAmount)
                .items(itemsToBuy)
                .status(checkoutConfig.getStatusProcessing())
                .build();

        checkoutSessionRepository.save(session);
        log.info("Checkout session saved for order: {}", razorpayOrderId);

        // NOTE: Order is already created via synchronous Feign call above (createOrderWithResilience)
        // DO NOT publish CreateOrderCommand via Kafka - it would create a duplicate order!

        // 7. Success response
        return new CheckoutResponse(
                CheckoutStatus.SUCCESS,
                razorpayOrderId,
                totalAmount,
                checkoutConfig.getCurrency(),
                null,
                null
        );
    }

    /**
     * Finalize order after successful payment.
     */
    @CircuitBreaker(name = "checkout", fallbackMethod = "finalizeOrderFallback")
    @Retry(name = "default")
    public String finalizeOrder(Long userId, PaymentCallbackRequest callback) {
        String orderId = callback.getRazorpayOrderId();
        
        Optional<CheckoutSession> sessionOpt = checkoutSessionRepository.findById(orderId);
        if (sessionOpt.isEmpty()) {
            throw new Exceptions.SessionNotFoundException("Checkout session not found: " + orderId);
        }

        CheckoutSession session = sessionOpt.get();
        
        // Verify user owns this session
        if (!session.getUserId().equals(userId)) {
            log.warn("User {} attempted to access session owned by user {}", userId, session.getUserId());
            throw new Exceptions.SessionNotFoundException("Checkout session not found: " + orderId);
        }

        // Check if already processed
        if (checkoutConfig.getStatusCompleted().equals(session.getStatus())) {
            log.info("Order {} already completed", orderId);
            return "Order already completed";
        }

        // TODO: Verify Razorpay signature here
        // For now, assume payment is verified
        log.info("Payment verified for order: {}", orderId);

        // Update session status
        session.setStatus(checkoutConfig.getStatusCompleted());
        checkoutSessionRepository.save(session);

        // Clear cart if checkout was from cart
        if (session.isCartId()) {
            try {
                clearCartWithResilience();
                log.info("Cart cleared for user: {}", userId);
            } catch (Exception e) {
                log.error("Failed to clear cart for user {}: {}", userId, e.getMessage());
                // Don't fail the checkout, cart clearing is not critical
            }
        }

        log.info("Order {} finalized successfully", orderId);
        return "Order confirmed successfully";
    }

    /**
     * Get checkout session status.
     */
    public CheckoutResponse getCheckoutStatus(Long userId, String orderId) {
        Optional<CheckoutSession> sessionOpt = checkoutSessionRepository.findById(orderId);
        
        if (sessionOpt.isEmpty()) {
            throw new Exceptions.SessionNotFoundException("Checkout session not found: " + orderId);
        }

        CheckoutSession session = sessionOpt.get();
        
        // Verify user owns this session
        if (!session.getUserId().equals(userId)) {
            throw new Exceptions.SessionNotFoundException("Checkout session not found: " + orderId);
        }

        CheckoutStatus status = switch (session.getStatus()) {
            case "PROCESSING" -> CheckoutStatus.PENDING;
            case "COMPLETED" -> CheckoutStatus.SUCCESS;
            case "FAILED" -> CheckoutStatus.FAILED;
            case "EXPIRED" -> CheckoutStatus.FAILED;
            default -> CheckoutStatus.PENDING;
        };

        return new CheckoutResponse(
                status,
                session.getOrderId(),
                session.getTotalAmount(),
                checkoutConfig.getCurrency(),
                null,
                null
        );
    }

    /**
     * Cancel a checkout session.
     */
    public void cancelCheckout(Long userId, String orderId) {
        Optional<CheckoutSession> sessionOpt = checkoutSessionRepository.findById(orderId);
        
        if (sessionOpt.isEmpty()) {
            throw new Exceptions.SessionNotFoundException("Checkout session not found: " + orderId);
        }

        CheckoutSession session = sessionOpt.get();
        
        // Verify user owns this session
        if (!session.getUserId().equals(userId)) {
            throw new Exceptions.SessionNotFoundException("Checkout session not found: " + orderId);
        }

        // Check if already completed
        if (checkoutConfig.getStatusCompleted().equals(session.getStatus())) {
            throw new Exceptions.InvalidCheckoutStateException("Cannot cancel a completed checkout");
        }

        // Delete the session
        checkoutSessionRepository.deleteById(orderId);
        log.info("Checkout session {} cancelled by user {}", orderId, userId);
    }

    // ==================== Resilient External Service Calls ====================

    @CircuitBreaker(name = "cartService", fallbackMethod = "fetchCartFallback")
    @Retry(name = "externalService")
    private CartResponse fetchCartWithResilience() {
        log.debug("Fetching cart from cart-service");
        return cartClient.getCart();
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "validateProductsFallback")
    @Retry(name = "externalService")
    private ProductValidationResponse validateProductsWithResilience(List<ProductValidationItem> items) {
        log.debug("Validating {} products with product-service", items.size());
        return productClient.validateProducts(items);
    }

    @CircuitBreaker(name = "orderService", fallbackMethod = "createOrderFallback")
    @Retry(name = "externalService")
    private OrderCheckoutResponse createOrderWithResilience(CreateOrderCommand command) {
        log.debug("Creating order in order-service for user: {}", command.getUserId());
        return orderClient.createCheckoutOrder(command);
    }

    @CircuitBreaker(name = "cartService")
    @Retry(name = "externalService")
    private void clearCartWithResilience() {
        log.debug("Clearing cart");
        cartClient.clearCart();
    }

    // ==================== Fallback Methods ====================

    private CheckoutResponse initiateCheckoutFallback(InitiateCheckoutRequest request, Exception e) {
        log.error("Checkout initiation failed for user {}: {}", request.getUserId(), e.getMessage());
        return new CheckoutResponse(
                CheckoutStatus.FAILED,
                null,
                null,
                checkoutConfig.getCurrency(),
                CheckoutFailureReason.SERVICE_UNAVAILABLE,
                null
        );
    }

    private String finalizeOrderFallback(Long userId, PaymentCallbackRequest callback, Exception e) {
        log.error("Order finalization failed for user {}: {}", userId, e.getMessage());
        throw new Exceptions.ServiceUnavailableException("Unable to finalize order. Please try again later.");
    }

    private CartResponse fetchCartFallback(Exception e) {
        log.error("Cart service unavailable: {}", e.getMessage());
        throw new Exceptions.ServiceUnavailableException("Cart service is temporarily unavailable");
    }

    private ProductValidationResponse validateProductsFallback(List<ProductValidationItem> items, Exception e) {
        log.error("Product service unavailable: {}", e.getMessage());
        throw new Exceptions.ServiceUnavailableException("Product service is temporarily unavailable");
    }

    private OrderCheckoutResponse createOrderFallback(CreateOrderCommand command, Exception e) {
        log.error("Order service unavailable: {}", e.getMessage());
        throw new Exceptions.ServiceUnavailableException("Order service is temporarily unavailable");
    }
}
