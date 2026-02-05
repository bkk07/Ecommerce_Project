package com.ecommerce.paymentservice.kafka;

import com.ecommerce.order.OrderCancelEvent;
import com.ecommerce.order.OrderCreatedEvent;
import com.ecommerce.payment.PaymentInitiatedEvent;
import com.ecommerce.paymentservice.exception.PaymentAlreadyExistsException;
import com.ecommerce.paymentservice.exception.PaymentGatewayException;
import com.ecommerce.paymentservice.exception.PaymentNotFoundException;
import com.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.ecommerce.common.KafkaProperties.ORDER_CANCEL_EVENTS_TOPIC;
import static com.ecommerce.common.KafkaProperties.ORDER_CREATED_EVENTS_TOPIC;

/**
 * Kafka listener for order-related events
 * Handles OrderCreatedEvent and OrderCancelEvent
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final PaymentService paymentService;
    private final PaymentEventProducer paymentEventProducer;

    /**
     * Handle OrderCreatedEvent from order-service
     * Creates a payment order in Razorpay and publishes PaymentInitiatedEvent
     */
    @KafkaListener(topics = ORDER_CREATED_EVENTS_TOPIC, groupId = "payment-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for Order: {}", event.getOrderId());
        try {
            PaymentInitiatedEvent paymentEvent = paymentService.handleOrderCreated(event);
            paymentEventProducer.publishPaymentInitiated(paymentEvent);
            log.info("Successfully processed OrderCreatedEvent for Order: {}", event.getOrderId());
        } catch (PaymentAlreadyExistsException e) {
            log.warn("Payment already exists for Order: {} - skipping", event.getOrderId());
        } catch (PaymentGatewayException e) {
            log.error("Payment gateway error for Order: {} - {}", event.getOrderId(), e.getMessage());
            // TODO: Consider publishing a PaymentFailedEvent or implementing retry logic
        } catch (Exception e) {
            log.error("Failed to process OrderCreatedEvent for Order: {}", event.getOrderId(), e);
            // TODO: Consider DLQ or compensation logic
        }
    }

    /**
     * Handle OrderCancelEvent from order-service (Saga compensation)
     * Processes refund if payment was made
     */
    @KafkaListener(topics = ORDER_CANCEL_EVENTS_TOPIC, groupId = "payment-saga-group")
    public void handleOrderCancelled(OrderCancelEvent event) {
        log.info("Received OrderCancelEvent for Order: {}", event.getOrderId());
        try {
            paymentService.processRefund(event.getOrderId());
            log.info("Successfully processed refund for Order: {}", event.getOrderId());
        } catch (PaymentNotFoundException e) {
            log.warn("No payment found for Order: {} - nothing to refund", event.getOrderId());
        } catch (PaymentGatewayException e) {
            log.error("Payment gateway error during refund for Order: {} - {}", event.getOrderId(), e.getMessage());
            // TODO: Consider retry logic or manual intervention alert
        } catch (Exception e) {
            log.error("Failed to process refund for Order: {}", event.getOrderId(), e);
            // TODO: Consider DLQ or manual intervention
        }
    }
}
