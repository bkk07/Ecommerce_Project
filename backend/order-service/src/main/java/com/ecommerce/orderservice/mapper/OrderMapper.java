package com.ecommerce.orderservice.mapper;

import com.ecommerce.order.OrderItemDto;
import com.ecommerce.orderservice.dto.OrderItemResponse;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
@Component
public class OrderMapper {
    public OrderResponse mapToDto(Order order) {
        return OrderResponse.builder()
                .orderNumber(order.getOrderId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .orderDate(order.getCreatedAt())
                .shippingAddress(order.getShippingAddress())
                .items(mapToItemDtos(order.getItems()))
                .build();
    }
    public List<OrderItemDto> mapToOrderItemDtos(List<OrderItem> orderItems) {
        List<OrderItemDto> orderItemDtos = new ArrayList<OrderItemDto>();
        for (OrderItem orderItem : orderItems) {
            OrderItemDto orderItemDto = new OrderItemDto();
            orderItemDto.setPrice(orderItem.getPrice());
            orderItemDto.setQuantity(orderItem.getQuantity());
            orderItemDto.setProductName(orderItem.getProductName());
            orderItemDto.setSkuCode(orderItem.getSkuCode());
            orderItemDto.setImageUrl(orderItem.getImageUrl());
            orderItemDtos.add(orderItemDto);
        }
        return orderItemDtos;
    }
    public List<OrderItemResponse> mapToItemDtos(List<OrderItem> items) {
        return items.stream()
                .map(item -> OrderItemResponse.builder()
                        .skuCode(item.getSkuCode())
                        .productName(item.getProductName())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .imageUrl(item.getImageUrl())
                        .build())
                .collect(Collectors.toList());
    }
}