package com.ecommerce.paymentservice.kafka;

import com.ecommerce.order.OrderCancelEvent;
import com.ecommerce.order.OrderCreatedEvent;
import com.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.ecommerce.common.KafkaProperties.ORDER_CANCEL_EVENTS_TOPIC;
import static com.ecommerce.common.KafkaProperties.ORDER_CREATED_EVENTS_TOPIC;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final PaymentService paymentService;

    @KafkaListener(topics = ORDER_CREATED_EVENTS_TOPIC, groupId = "payment-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for Order: {}", event.getOrderId());
        try {
            paymentService.handleOrderCreated(event);
        } catch (Exception e) {
            log.error("Failed to process OrderCreatedEvent for Order {}", event.getOrderId(), e);
        }
    }
    @KafkaListener(topics = ORDER_CANCEL_EVENTS_TOPIC, groupId = "payment-saga-group")
    public void handleOrderCancelled(OrderCancelEvent event) {
        log.info("Received OrderCancelEvent for Order: {}", event.getOrderId());
        try {
            paymentService.processRefund(event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to process refund for Order {}", event.getOrderId(), e);
        }
    }
}
