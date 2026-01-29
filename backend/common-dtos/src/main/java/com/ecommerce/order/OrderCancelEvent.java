package com.ecommerce.order;

import java.util.List;

public class OrderCancelEvent {
    private String orderId;
    private String userId;
    List<OrderItemDto> items;
    public OrderCancelEvent(String orderId, String userId, List<OrderItemDto> items) {
        this.orderId = orderId;
        this.userId = userId;
        this.items = items;
    }
    public OrderCancelEvent() {}

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

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }
}
