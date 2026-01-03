package com.ecommerce.inventory;

import com.ecommerce.order.OrderItemDto;

import java.util.List;
public class InventoryLockEvent {
    private String orderId;
    private List<OrderItemDto> items;

    public InventoryLockEvent() {
    }

    public InventoryLockEvent(String orderId, List<OrderItemDto> items) {
        this.orderId = orderId;
        this.items = items;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }
}
