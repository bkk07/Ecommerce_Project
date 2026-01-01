package com.ecommerce.orderservice.kafka;

import com.ecommerce.orderservice.event.PaymentSuccessEvent;
import com.ecommerce.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final OrderService orderService;

    @KafkaListener(topics = "payment-success", groupId = "order-group")
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("Received Payment Success Event for Checkout ID: {}", event.getCheckoutId());

        try {
            orderService.createOrder(event);
        } catch (Exception e) {
            // CRITICAL: If this fails, we have taken money but created no order.
            // In Production: Send to Dead Letter Queue (DLQ) for manual fix.
            log.error("Failed to create order for payment: {}", event.getRazorpayPaymentId(), e);
        }
    }
}