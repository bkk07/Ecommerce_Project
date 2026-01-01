package com.ecommerce.orderservice.mapper;

import com.ecommerce.orderservice.dto.OrderItemResponse;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponse mapToDto(Order order) {
        return OrderResponse.builder()
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getCreatedAt())
                .shippingAddress(order.getShippingAddress())
                .items(mapToItemDtos(order.getItems()))
                .build();
    }

    private List<OrderItemResponse> mapToItemDtos(List<OrderItem> items) {
        return items.stream()
                .map(item -> OrderItemResponse.builder()
                        .skuCode(item.getSkuCode())
                        .productName(item.getProductName())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());
    }
}