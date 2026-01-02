package com.ecommerce.orderservice.kafka;

import com.ecommerce.order.OrderCreatedEvent;
import com.ecommerce.orderservice.service.OrderService;
import com.ecommerce.payment.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.ecommerce.common.KafkaProperties.ORDER_CREATED_EVENTS_TOPIC;
import static com.ecommerce.common.KafkaProperties.PAYMENTS_EVENTS_SUCCESS_TOPIC;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {
    private final OrderService orderService;

    @KafkaListener(topics = PAYMENTS_EVENTS_SUCCESS_TOPIC, groupId = "order-group")
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("Received Payment Success Event for Checkout ID: {}", event.getOrderId());
        try {
            orderService.updatedToPlaced(event);
        } catch (Exception e) {
            log.error("Failed to update to placed order for payment: {}", event.getPaymentId(), e);
        }
    }
    @KafkaListener(topics = ORDER_CREATED_EVENTS_TOPIC, groupId = "order-group")
    public void handleOrderCreation(OrderCreatedEvent event) {
        log.info("Received Order From the check out service {}", event.getOrderId());
        try {
            orderService.createOrder(event);
        } catch (Exception e) {
            log.error("Failed to create order for payment: {}", event.getOrderId(), e);
        }
    }
}