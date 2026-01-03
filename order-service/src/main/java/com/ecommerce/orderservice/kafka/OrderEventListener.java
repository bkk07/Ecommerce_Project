package com.ecommerce.orderservice.kafka;

import com.ecommerce.checkout.CreateOrderCommand;
import com.ecommerce.inventory.InventoryLockFailedEvent;
import com.ecommerce.inventory.InventoryReleasedEvent;
import com.ecommerce.orderservice.service.OrderService;
import com.ecommerce.orderservice.service.SagaOrchestratorService;
import com.ecommerce.payment.PaymentInitiatedEvent;
import com.ecommerce.payment.PaymentRefundedEvent;
import com.ecommerce.payment.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.ecommerce.common.KafkaProperties.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {
    private final OrderService orderService;
    private final SagaOrchestratorService sagaOrchestratorService;

    @KafkaListener(topics = CREATE_ORDER_COMMAND_TOPIC, groupId = "order-group")
    public void handleCreateOrderCommand(CreateOrderCommand command) {
        log.info("Received CreateOrderCommand for User: {}", command.getUserId());
        try {
            orderService.createOrder(command);
        } catch (Exception e) {
            log.error("Failed to create order for user: {}", command.getUserId(), e);
        }
    }

    @KafkaListener(topics = PAYMENTS_EVENTS_SUCCESS_TOPIC, groupId = "order-group")
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("Received Payment Success Event for Order ID: {}", event.getOrderId());
        try {
            orderService.updatedToPlaced(event);
        } catch (Exception e) {
            log.error("Failed to update to placed order for payment: {}", event.getPaymentId(), e);
        }
    }

    @KafkaListener(topics = PAYMENT_INITIATED_EVENT_TOPIC, groupId = "order-group")
    public void handlePaymentInitiated(PaymentInitiatedEvent event) {
        log.info("Received Payment Initiated Event for Order ID: {}", event.getOrderId());
        try {
            orderService.updatePaymentReady(event);
        } catch (Exception e) {
            log.error("Failed to update payment ready for order: {}", event.getOrderId(), e);
        }
    }
    @KafkaListener(topics = INVENTORY_RELEASED_EVENTS_TOPIC, groupId = "order-saga-group")
    public void handleInventoryReleased(InventoryReleasedEvent event) {
        sagaOrchestratorService.handleInventoryReleased(event);
    }
    @KafkaListener(topics = PAYMENT_REFUNDED_EVENTS_TOPIC, groupId = "order-saga-group")
    public void handlePaymentRefunded(PaymentRefundedEvent event) {
        sagaOrchestratorService.handlePaymentRefunded(event);
    }

    @KafkaListener(topics = INVENTORY_LOCK_FAILED_TOPIC, groupId = "order-group")
    public void handleInventoryLockFailed(InventoryLockFailedEvent event) {
        log.info("Received Inventory Lock Failed Event for Order ID: {}", event.getOrderId());
        orderService.cancelOrder(event.getOrderId());
    }
}
