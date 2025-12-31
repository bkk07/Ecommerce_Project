package com.ecommerce.checkoutservice.service;
import com.ecommerce.checkoutservice.dto.*;
import com.ecommerce.checkoutservice.entity.CheckoutSession;
import com.ecommerce.checkoutservice.exception.Exceptions;
import com.ecommerce.checkoutservice.kafka.KafkaEventPublisher;
import com.ecommerce.checkoutservice.kafka.OrderPlacedEvent;
import com.ecommerce.checkoutservice.openfeign.CartClient;
import com.ecommerce.checkoutservice.openfeign.InventoryClient;
import com.ecommerce.checkoutservice.openfeign.PaymentClient;
import com.ecommerce.checkoutservice.repository.CheckoutSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final CartClient cartClient;
    private final CheckoutSessionRepository sessionRepository;
    private final KafkaEventPublisher eventPublisher;

    public CheckoutResponse initiateCheckout(InitiateCheckoutRequest request) {

        List<CheckoutItem> itemsToBuy;

        // 1. Determine Source (Cart or Direct)
        if (request.getCartId() != null && !request.getCartId().isEmpty()) {
            itemsToBuy = cartClient.getCartItems(request.getCartId());
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
        inventoryClient.lockStock(itemsToBuy);

        try {
            // 4. Create Razorpay Order (Payment Service)
            String razorpayOrderId = paymentClient.createOrder(totalAmount.longValue());

            // 5. Save Session in Redis
            CheckoutSession session = CheckoutSession.builder()
                    .orderId(razorpayOrderId)
                    .userId(request.getUserId())
                    .cartId(request.getCartId()) // Will be null if Direct Buy
                    .items(itemsToBuy)
                    .totalAmount(totalAmount)
                    .status("PENDING")
                    .build();

            sessionRepository.save(session);

            return new CheckoutResponse(razorpayOrderId, totalAmount, "INR", "CREATED");

        } catch (Exception e) {
            // Rollback Stock if payment creation fails
            inventoryClient.releaseStock(itemsToBuy);
            throw new RuntimeException("Checkout Init Failed", e);
        }
    }

    public String finalizeOrder(PaymentCallbackRequest callback) {

        // 1. Get Session from Redis
        CheckoutSession session = sessionRepository.findById(callback.getRazorpayOrderId())
                .orElseThrow(() -> new Exceptions.SessionNotFoundException("Session Expired"));

        // 2. Verify Signature (Payment Service)
        boolean isValid = paymentClient.verifyPayment(new PaymentVerificationRequest(
                callback.getRazorpayOrderId(),
                callback.getRazorpayPaymentId(),
                callback.getRazorpaySignature()
        ));

        if (!isValid) {
            inventoryClient.releaseStock(session.getItems());
            sessionRepository.deleteById(session.getOrderId());
            throw new Exceptions.PaymentFailedException("Signature Verification Failed");
        }

        // 3. Publish Event (Success)
        OrderPlacedEvent event = new OrderPlacedEvent(
                session.getOrderId(),
                session.getUserId(),
                session.getItems(),
                session.getTotalAmount()
        );
        eventPublisher.publishOrderEvent(event);

        // 4. Conditional Cart Cleanup
        if (session.getCartId() != null) {
            try {
                cartClient.clearCart(session.getCartId());
            } catch (Exception e) {
                log.error("Failed to clear cart {} after success", session.getCartId());
            }
        }

        // 5. Cleanup Redis
        sessionRepository.deleteById(session.getOrderId());

        return "Order Placed Successfully";
    }
}
