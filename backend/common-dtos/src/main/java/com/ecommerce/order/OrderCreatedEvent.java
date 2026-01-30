package com.ecommerce.order;

import java.math.BigDecimal;
import java.util.List;

public class OrderCreatedEvent {
    private String orderId;
    private String userId;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private List<OrderItemDto> items;

    public OrderCreatedEvent() {
    }

    public OrderCreatedEvent(String orderId, String userId, BigDecimal totalAmount, String shippingAddress, List<OrderItemDto> items) {
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
        this.items = items;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }
}
