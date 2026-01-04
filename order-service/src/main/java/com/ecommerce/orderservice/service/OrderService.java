package com.ecommerce.orderservice.service;

import com.ecommerce.checkout.CreateOrderCommand;
import com.ecommerce.inventory.InventoryLockEvent;
import com.ecommerce.order.OrderCancelEvent;
import com.ecommerce.order.OrderCreatedEvent;
import com.ecommerce.order.OrderItemDto;
import com.ecommerce.order.OrderPlacedEvent;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.kafka.OrderEventPublisher;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.ecommerce.payment.PaymentInitiatedEvent;
import com.ecommerce.payment.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    // 1. CREATE (Triggered by Kafka CreateOrderCommand)
    @Transactional
    public void createOrder(CreateOrderCommand command) {
        log.info("Order Service is Processing Create Order Command for User: {}", command.getUserId());
        
        String orderId = UUID.randomUUID().toString();

        Order order = Order.builder()
                .orderId(orderId)
                .userId(command.getUserId())
                .status(OrderStatus.PENDING) // Initial status PENDING
                .totalAmount(command.getTotalAmount())
                .shippingAddress("Address") // Placeholder
                .build();

        List<OrderItem> items = command.getItems().stream()
                .map(itemDto -> OrderItem.builder()
                        .skuCode(itemDto.getSkuCode())
                        .productName(itemDto.getProductName())
                        .price(itemDto.getPrice())
                        .quantity(itemDto.getQuantity())
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
        event.setAddressDTO(command.getAddressDTO());
        
        orderEventPublisher.publishOrderCreatedEvent(event);
        log.info("Published OrderCreatedEvent for Order ID: {}", orderId);
    }

    // 2. READ: Get My Orders (Returns DTOs)
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(String userId) {
        log.info("Fetching orders for User ID: {}", userId);
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(orderMapper::mapToDto)
                .collect(Collectors.toList());
    }

    // 3. READ: Get Specific Order (Returns DTO)
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetails(String orderNumber) {
        log.info("Fetching details for Order ID: {}", orderNumber);
        Order order = orderRepository.findByOrderId(orderNumber)
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", orderNumber);
                    return new RuntimeException("Order not found");
                });
        return orderMapper.mapToDto(order);
    }

    @Transactional
    public void updatedToPlaced(PaymentSuccessEvent event){
        log.info("Updating order status to PLACED for Order ID: {}", event.getOrderId());
        Order order = orderRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", event.getOrderId());
                    return new RuntimeException("Order not found");
                });
        order.setPaymentId(event.getPaymentId());
        order.setStatus(OrderStatus.PLACED);
        orderRepository.save(order);
        log.info("Order Updated Successfully to PLACED for Order ID: {}", event.getOrderId());

        // Publish OrderPlacedEvent
        OrderPlacedEvent orderPlacedEvent = new OrderPlacedEvent(order.getOrderId(), order.getUserId());
        orderEventPublisher.publishOrderPlacedEvent(orderPlacedEvent);
        log.info("Published OrderPlacedEvent for Order ID: {}", order.getOrderId());
    }

    @Transactional
    public void updatePaymentReady(PaymentInitiatedEvent event) {
        log.info("Updating order status to PAYMENT_READY for Order ID: {}", event.getOrderId());
        Order order = orderRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", event.getOrderId());
                    return new RuntimeException("Order not found");
                });
        
        order.setRazorpayOrderId(event.getRazorpayOrderId());
        order.setStatus(OrderStatus.PAYMENT_READY);
        orderRepository.save(order);

        log.info("Payment Initiated for Order: {}, Razorpay Order ID: {}", event.getOrderId(), event.getRazorpayOrderId());
    }

    @Transactional
    public String updateStateOfTheOrder(String orderId, OrderStatus status){
        log.info("Updating state of Order ID: {} to {}", orderId, status);
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", orderId);
                    return new RuntimeException("Order not found");
                });
        
        // If user requests cancellation, we start the Saga
        if(status == OrderStatus.CANCELLED){
            // We set it to CANCEL_REQUESTED to indicate Saga is in progress
            order.setStatus(OrderStatus.CANCEL_REQUESTED);
            
            OrderCancelEvent orderCancelEvent = new OrderCancelEvent();
            orderCancelEvent.setOrderId(orderId);
            orderCancelEvent.setUserId(order.getUserId());
            List<OrderItemDto> orderItemDtos = orderMapper.mapToOrderItemDtos(order.getItems());
            orderCancelEvent.setItems(orderItemDtos);

            orderEventPublisher.handleOrderCancel(orderCancelEvent);
            log.info("Initiated Saga Cancellation for Order {}", orderId);
        } else {
            order.setStatus(status);
        }

        orderRepository.save(order);
        log.info("Successfully Updated the state of the order {} to {}", order.getOrderId(), order.getStatus());
        return "Order Updated Successfully";
    }

    @Transactional
    public void cancelOrder(String orderId) {
        log.info("Attempting to cancel Order ID: {} due to inventory lock failure", orderId);
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", orderId);
                    return new RuntimeException("Order not found");
                });
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Order {} Cancelled successfully due to inventory lock failure", orderId);
    }
}
