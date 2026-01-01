package com.ecommerce.checkoutservice.service;
import com.ecommerce.cart.CartItemResponse;
import com.ecommerce.cart.CartResponse;
import com.ecommerce.checkout.CheckoutItem;
import com.ecommerce.checkoutservice.dto.*;
import com.ecommerce.checkoutservice.entity.CheckoutSession;
import com.ecommerce.checkoutservice.exception.Exceptions;
import com.ecommerce.checkoutservice.kafka.KafkaEventPublisher;
import com.ecommerce.checkoutservice.kafka.OrderPlacedEvent;
import com.ecommerce.checkoutservice.openfeign.CartClient;
import com.ecommerce.checkoutservice.openfeign.InventoryClient;
import com.ecommerce.checkoutservice.openfeign.PaymentClient;
import com.ecommerce.checkoutservice.repository.CheckoutSessionRepository;
import com.ecommerce.inventory.StockItem;
import com.ecommerce.payment.PaymentCreateRequest;
import com.ecommerce.payment.VerifyPaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final CartClient cartClient;
    private final CheckoutSessionRepository sessionRepository;
    private final KafkaEventPublisher eventPublisher;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SHADOW_KEY_PREFIX = "shadow:";
    public CheckoutResponse initiateCheckout(InitiateCheckoutRequest request) {

        List<CheckoutItem> itemsToBuy;

        // 1. Determine Source (Cart or Direct)
        if (request.isCartId()) {
            CartResponse cartResponse = cartClient.getCart();
            itemsToBuy = mapToCartResponse(cartResponse);
        } else {
            itemsToBuy = request.getItems();
        }

        if (itemsToBuy == null || itemsToBuy.isEmpty()) {
            throw new IllegalArgumentException("No items to checkout");
        }

        // 2. Calculate Total
        BigDecimal totalAmount = itemsToBuy.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Lock Stock (Inventory Service)
        inventoryClient.lockStock(mapToStockItems(itemsToBuy));
        try {
            // 4. Create Razorpay Order (Payment Service)
            String razorpayOrderId = paymentClient.createOrder(new PaymentCreateRequest(totalAmount.longValue(), request.getUserId()));

            // 5. Save Session in Redis (Data Key - 20 mins)
            CheckoutSession session = CheckoutSession.builder()
                    .orderId(razorpayOrderId)
                    .userId(request.getUserId()) // Save userId to session
                    .cartId(request.isCartId())
                    .items(itemsToBuy)
                    .totalAmount(totalAmount)
                    .status("PENDING")
                    .build();

            sessionRepository.save(session);

            // 6. Create Shadow Key (15 mins)
            String shadowKey = SHADOW_KEY_PREFIX + razorpayOrderId;
            redisTemplate.opsForValue().set(shadowKey, "dummy");
            redisTemplate.expire(shadowKey, 15, TimeUnit.MINUTES);

            return new CheckoutResponse(razorpayOrderId, totalAmount, "INR", "CREATED");

        } catch (Exception e) {
            // Rollback Stock if payment creation fails
            inventoryClient.releaseStock(mapToStockItems(itemsToBuy));
            throw new RuntimeException("Checkout Init Failed", e);
        }
    }

    private List<CheckoutItem> mapToCartResponse(CartResponse cartResponse) {
        List<CheckoutItem> checkoutItems= new ArrayList<>();
        for(CartItemResponse cartItemResponse:cartResponse.getItems()){
            CheckoutItem checkoutItem = new CheckoutItem();
            checkoutItem.setSkuCode(cartItemResponse.getSkuCode());
            checkoutItem.setPrice(cartItemResponse.getPrice());
            checkoutItem.setQuantity(cartItemResponse.getQuantity());
            checkoutItem.setProductName(cartItemResponse.getProductName());
            checkoutItems.add(checkoutItem);
        }
        return checkoutItems;
    }

    public String finalizeOrder(PaymentCallbackRequest callback) {

        // 1. Get Session from Redis
        CheckoutSession session = sessionRepository.findById(callback.getRazorpayOrderId())
                .orElseThrow(() -> new Exceptions.SessionNotFoundException("Session Expired"));

        // 2. Verify Signature (Payment Service)
        boolean isValid = paymentClient.verifyPayment(new VerifyPaymentRequest(
                callback.getRazorpayOrderId(),
                callback.getRazorpayPaymentId(),
                callback.getRazorpaySignature()
        ));

        if (!isValid) {
            inventoryClient.releaseStock(mapToStockItems(session.getItems()));
            // Delete both keys
            sessionRepository.deleteById(session.getOrderId());
            redisTemplate.delete(SHADOW_KEY_PREFIX + session.getOrderId());
            throw new Exceptions.PaymentFailedException("Signature Verification Failed");
        }
        
        // 3. Publish Event (Success)
        OrderPlacedEvent event = new OrderPlacedEvent(
                session.getOrderId(),
                session.getUserId(), // Now we have userId
                session.getItems(),
                session.getTotalAmount()
        );
        eventPublisher.publishOrderEvent(event);

        if (session.isCartId()) {
            try {
                cartClient.clearCart();
            } catch (Exception e) {
                log.error("Failed to clear cart after success");
            }
        }
        // 5. Cleanup Redis
        sessionRepository.deleteById(session.getOrderId());
        redisTemplate.delete(SHADOW_KEY_PREFIX + session.getOrderId());
        return "Order Placed Successfully";
    }

    private List<StockItem> mapToStockItems(List<CheckoutItem> items){
        List<StockItem> stockItems = new ArrayList<>();
        for(CheckoutItem item:items) {
            StockItem stockItem = new StockItem();
            stockItem.setSku(item.getSkuCode());
            stockItem.setQuantity(item.getQuantity());
            stockItems.add(stockItem);
        }
        return stockItems;
    }
}
