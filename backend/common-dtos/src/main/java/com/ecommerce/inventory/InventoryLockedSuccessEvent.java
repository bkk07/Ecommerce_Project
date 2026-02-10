package com.ecommerce.inventory;

import com.ecommerce.order.OrderItemDto;
import java.math.BigDecimal;
import java.util.List;

/**
 * Event published when inventory is successfully locked for an order.
 * This event signals that the order can proceed to payment creation.
 */
public class InventoryLockedSuccessEvent {
    private String orderId;
    private String userId;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private List<OrderItemDto> items;
    private int totalItemsLocked;

    public InventoryLockedSuccessEvent() {
    }

    public InventoryLockedSuccessEvent(String orderId, String userId, List<OrderItemDto> items, int totalItemsLocked) {
        this.orderId = orderId;
        this.userId = userId;
        this.items = items;
        this.totalItemsLocked = totalItemsLocked;
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

    public int getTotalItemsLocked() {
        return totalItemsLocked;
    }

    public void setTotalItemsLocked(int totalItemsLocked) {
        this.totalItemsLocked = totalItemsLocked;
    }
}
