package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.event.PaymentSuccessEvent;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    // 1. CREATE (Triggered by Kafka)
    @Transactional
    public void createOrder(PaymentSuccessEvent event) {
        if (orderRepository.existsByPaymentId(event.getRazorpayPaymentId())) {
            return;
        }

        Order order = Order.builder()
                .orderNumber(UUID.randomUUID().toString())
                .paymentId(event.getRazorpayPaymentId())
                .userId(event.getUserId())
                .status(OrderStatus.PLACED)
                .totalAmount(new BigDecimal(event.getAmount()))
                .shippingAddress(event.getAddressJson())
                .build();

        List<OrderItem> items = event.getItems().stream()
                .map(itemDto -> OrderItem.builder()
                        .skuCode(itemDto.getSkuCode())
                        .productName(itemDto.getProductName())
                        .price(new BigDecimal(itemDto.getPrice()))
                        .quantity(itemDto.getQuantity())
                        .order(order)
                        .build())
                .collect(Collectors.toList());

        order.setItems(items);
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
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return orderMapper.mapToDto(order);
    }
}