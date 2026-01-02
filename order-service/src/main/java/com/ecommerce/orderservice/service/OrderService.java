package com.ecommerce.orderservice.service;

import com.ecommerce.order.OrderCreatedEvent;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.repository.OrderRepository;
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

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    // 1. CREATE (Triggered by Kafka)
    @Transactional
    public void createOrder(OrderCreatedEvent event) {
        log.info("Order Service is Processing Create Order");
        Order order = Order.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .status(OrderStatus.PENDING)
                .totalAmount(event.getTotalAmount())
                .shippingAddress("Address") // I need to change here for the testing purpose
                .build();

        List<OrderItem> items = event.getItems().stream()
                .map(itemDto -> OrderItem.builder()
                        .skuCode(itemDto.getSkuCode())
                        .productName(itemDto.getProductName())
                        .price(itemDto.getPrice())
                        .quantity(itemDto.getQuantity())
                        .order(order)
                        .build())
                .collect(Collectors.toList());
        order.setItems(items);
        log.info("Order Created Successfully");
        orderRepository.save(order);
    }

    // 2. READ: Get My Orders (Returns DTOs)
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(String userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        return orders.stream()
                .map(orderMapper::mapToDto)
                .collect(Collectors.toList());
    }

    // 3. READ: Get Specific Order (Returns DTO)
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetails(String orderNumber) {
        Order order = orderRepository.findByOrderId(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return orderMapper.mapToDto(order);
    }

    @Transactional
    public void updatedToPlaced(PaymentSuccessEvent event){
        Order order = orderRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setPaymentId(event.getPaymentId());
        order.setStatus(OrderStatus.PLACED);
        orderRepository.save(order);
        log.info("Order Updated Successfully {}", event.getOrderId());
    }
    @Transactional
    public String updateStateOfTheOrder(String orderId, OrderStatus status){
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        order.setStatus(status);
        orderRepository.save(order);
        log.info("Successfully Updated the state of the order {}",order.getOrderId());
        return "Order Updated Successfully";
    }
}