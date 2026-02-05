package com.ecommerce.orderservice.service;

import com.ecommerce.checkout.CreateOrderCommand;
import com.ecommerce.checkout.OrderCheckoutResponse;
import com.ecommerce.inventory.InventoryLockEvent;
import com.ecommerce.order.*;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderOutbox;
import com.ecommerce.orderservice.entity.OutboxStatus;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.exception.OrderCancellationException;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.exception.PaymentException;
import com.ecommerce.orderservice.exception.ServiceUnavailableException;
import com.ecommerce.orderservice.feign.PaymentFeign;
import com.ecommerce.orderservice.kafka.OrderEventPublisher;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.repository.OrderOutboxRepository;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.payment.PaymentInitiatedEvent;
import com.ecommerce.payment.PaymentSuccessEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderEventPublisher orderEventPublisher;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderOutboxRepository orderOutboxRepository;
    private final ObjectMapper objectMapper;
    private final PaymentFeign paymentFeign;

    // 1. CREATE (Triggered by Kafka CreateOrderCommand)
    @Transactional
    @CircuitBreaker(name = "paymentService", fallbackMethod = "createOrderFallback")
    @Retry(name = "paymentService")
    public OrderCheckoutResponse createOrder(CreateOrderCommand command) {
        log.info("Order Service is Processing Create Order Command for User: {}", command.getUserId());
        String orderId = UUID.randomUUID().toString();

        Order order = Order.builder()
                .orderId(orderId)
                .userId(command.getUserId())
                .status(OrderStatus.PENDING) // Initial status PENDING
                .totalAmount(command.getTotalAmount())
                .shippingAddress(command.getShippingAddress()) // Placeholder
                .build();
        List<OrderItem> items = command.getItems().stream()
                .map(itemDto -> OrderItem.builder()
                        .skuCode(itemDto.getSkuCode())
                        .productName(itemDto.getProductName())
                        .price(itemDto.getPrice())
                        .quantity(itemDto.getQuantity())
                        .imageUrl(itemDto.getImageUrl())
                        .order(order)
                        .build())
                .collect(Collectors.toList());
        order.setItems(items);
        orderRepository.save(order);
        log.info("Order Persisted Successfully with ID: {}", orderId);

        // Publish InventoryLockEvent
        InventoryLockEvent inventoryLockEvent = new InventoryLockEvent(orderId, command.getItems());
        orderEventPublisher.publishInventoryLockEvent(inventoryLockEvent);
        log.info("Published InventoryLockEvent for Order ID: {}", orderId);

        // Publish OrderCreatedEvent
        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(orderId);
        event.setUserId(command.getUserId());
        event.setTotalAmount(command.getTotalAmount());
        event.setItems(command.getItems());
        event.setShippingAddress(command.getShippingAddress());

        try {
            PaymentInitiatedEvent paymentInitiatedEvent = paymentFeign.createPayment(event);
            return new OrderCheckoutResponse(paymentInitiatedEvent.getRazorpayOrderId());
        } catch (Exception e) {
            log.error("Payment service call failed for order: {}", orderId, e);
            throw new PaymentException(orderId, "Failed to initiate payment: " + e.getMessage());
        }
    }
    
    /**
     * Fallback method when payment service is unavailable
     */
    public OrderCheckoutResponse createOrderFallback(CreateOrderCommand command, Throwable t) {
        log.error("Payment service unavailable. Fallback triggered for user: {}", command.getUserId(), t);
        throw new ServiceUnavailableException("Payment Service");
    }

    // 2. READ: Get My Orders (Returns DTOs)
    @Transactional(readOnly = true)
    @Retry(name = "databaseRetry")
    public List<OrderResponse> getUserOrders(String userId) {
        log.info("Fetching orders for User ID: {}", userId);
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(orderMapper::mapToDto)
                .collect(Collectors.toList());
    }

    // 3. READ: Get Specific Order (Returns DTO)
    @Transactional(readOnly = true)
    @Retry(name = "databaseRetry")
    public OrderResponse getOrderDetails(String orderNumber) {
        log.info("Fetching details for Order ID: {}", orderNumber);
        Order order = orderRepository.findByOrderId(orderNumber)
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", orderNumber);
                    return new OrderNotFoundException(orderNumber);
                });
        return orderMapper.mapToDto(order);
    }

    @Transactional
    @Retry(name = "databaseRetry")
    public void updatedToPlaced(PaymentSuccessEvent event){
        log.info("========================================");
        log.info("PROCESSING PAYMENT SUCCESS EVENT");
        log.info("Order ID: {}", event.getOrderId());
        log.info("Payment ID: {}", event.getPaymentId());
        log.info("========================================");
        
        Order order = orderRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", event.getOrderId());
                    return new OrderNotFoundException(event.getOrderId());
                });
        
        log.info("Found order - Current Status: {}", order.getStatus());
        
        // Idempotency check - skip if already placed or in a later state
        if (order.getStatus() == OrderStatus.PLACED || 
            order.getStatus() == OrderStatus.CONFIRMED ||
            order.getStatus() == OrderStatus.SHIPPED ||
            order.getStatus() == OrderStatus.DELIVERED) {
            log.info("Order {} already in status {}, skipping update to PLACED", 
                    event.getOrderId(), order.getStatus());
            return;
        }
        
        order.setPaymentId(event.getPaymentId());
        order.setStatus(OrderStatus.PLACED);
        orderRepository.save(order);
        
        log.info("========================================");
        log.info("ORDER UPDATED TO PLACED SUCCESSFULLY");
        log.info("Order ID: {}", event.getOrderId());
        log.info("Payment ID: {}", event.getPaymentId());
        log.info("========================================");

        // Create Outbox Event for ORDER_PLACED
        saveOutboxEvent(order, OrderNotificationType.ORDER_PLACED, null);
    }

    @Transactional
    @Retry(name = "databaseRetry")
    public void updatePaymentReady(PaymentInitiatedEvent event) {
        log.info("Updating order status to PAYMENT_READY for Order ID: {}", event.getOrderId());
        Order order = orderRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", event.getOrderId());
                    return new OrderNotFoundException(event.getOrderId());
                });
        
        order.setRazorpayOrderId(event.getRazorpayOrderId());
        order.setStatus(OrderStatus.PAYMENT_READY);
        orderRepository.save(order);

        log.info("Payment Initiated for Order: {}, Razorpay Order ID: {}", event.getOrderId(), event.getRazorpayOrderId());
    }

    @Transactional
    @Retry(name = "databaseRetry")
    public String updateStateOfTheOrder(String orderId, OrderStatus status){
        log.info("Updating state of Order ID: {} to {}", orderId, status);
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", orderId);
                    return new OrderNotFoundException(orderId);
                });
        
        // Validate status transition
        validateStatusTransition(order, status);
        
        // If user requests cancellation, we start the Saga
        if(status == OrderStatus.CANCELLED){
            // Check if order can be cancelled
            if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
                throw new OrderCancellationException(orderId, "Order cannot be cancelled after shipping");
            }
            
            // We set it to CANCEL_REQUESTED to indicate Saga is in progress
            log.info("Initiated Saga Cancellation for Order {}", orderId);
            order.setStatus(OrderStatus.CANCEL_REQUESTED);
            
            OrderCancelEvent orderCancelEvent = new OrderCancelEvent();
            orderCancelEvent.setOrderId(orderId);
            orderCancelEvent.setUserId(order.getUserId());
            List<OrderItemDto> orderItemDtos = orderMapper.mapToOrderItemDtos(order.getItems());
            orderCancelEvent.setItems(orderItemDtos);
            orderEventPublisher.handleOrderCancel(orderCancelEvent);
        } else {
            order.setStatus(status);
            
            // If status is DELIVERED, publish event for rating eligibility and notification
            if (status == OrderStatus.DELIVERED) {
                publishOrderDeliveredEvent(order);
                // Trigger notification for order delivered
                saveOutboxEvent(order, OrderNotificationType.ORDER_DELIVERED, null);
            }
            
            // If status is SHIPPED, trigger notification
            if (status == OrderStatus.SHIPPED) {
                saveOutboxEvent(order, OrderNotificationType.ORDER_SHIPPED, null);
            }
        }

        orderRepository.save(order);
        log.info("Successfully Updated the state of the order {} to {}", order.getOrderId(), order.getStatus());
        return "Order Updated Successfully";
    }
    
    /**
     * Validate that the status transition is valid
     */
    private void validateStatusTransition(Order order, OrderStatus newStatus) {
        OrderStatus currentStatus = order.getStatus();
        
        // Already in terminal state
        if (currentStatus == OrderStatus.CANCELLED || currentStatus == OrderStatus.DELIVERED) {
            if (newStatus != currentStatus) {
                throw new OrderCancellationException(order.getOrderId(), 
                    "Cannot change status from " + currentStatus + " to " + newStatus);
            }
        }
    }

    /**
     * Publish ORDER_DELIVERED event for rating eligibility
     */
    private void publishOrderDeliveredEvent(Order order) {
        List<OrderDeliveredEvent.DeliveredItem> deliveredItems = order.getItems().stream()
                .map(item -> new OrderDeliveredEvent.DeliveredItem(
                        item.getSkuCode(),
                        item.getProductName(),
                        item.getImageUrl()))
                .collect(Collectors.toList());

        OrderDeliveredEvent event = OrderDeliveredEvent.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .items(deliveredItems)
                .build();

        orderEventPublisher.publishOrderDeliveredEvent(event);
        log.info("Published ORDER_DELIVERED event for order: {} with {} items", 
                order.getOrderId(), deliveredItems.size());
    }

    @Transactional
    @Retry(name = "databaseRetry")
    public void cancelOrder(String orderId) {
        log.info("Attempting to cancel Order ID: {} due to inventory lock failure", orderId);
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", orderId);
                    return new OrderNotFoundException(orderId);
                });
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order {} Cancelled successfully due to inventory lock failure", orderId);

        // Create Outbox Event for ORDER_CANCELLED
        saveOutboxEvent(order, OrderNotificationType.ORDER_CANCELLED, "Inventory Lock Failure");
    }

    // Changed to public so SagaOrchestratorService can use it
    public void saveOutboxEvent(Order order, OrderNotificationType type, String reason) {
        try {
            OrderPayload payload = new OrderPayload(
                    order.getOrderId(),
                    order.getTotalAmount(),
                    "USD", // Assuming USD for now, or fetch from order if available
                    order.getItems() != null ? order.getItems().size() : 0,
                    reason
            );

            OrderNotificationEvent notificationEvent = new OrderNotificationEvent(
                    UUID.randomUUID().toString(),
                    type,
                    order.getUserId(),
                    Instant.now(),
                    1,
                    payload
            );

            OrderOutbox outbox = OrderOutbox.builder()
                    .eventId(notificationEvent.getEventId())
                    .aggregateId(order.getOrderId())
                    .eventType(type.name())
                    .payload(objectMapper.writeValueAsString(notificationEvent))
                    .status(OutboxStatus.PENDING)
                    .build();

            orderOutboxRepository.save(outbox);
            log.info("Saved Outbox Event: {} for Order: {}", type, order.getOrderId());

        } catch (JsonProcessingException e) {
            log.error("Error serializing outbox event for Order: {}", order.getOrderId(), e);
            throw new RuntimeException("Error serializing outbox event", e);
        }
    }
}