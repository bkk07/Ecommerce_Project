package com.ecommerce.inventory;

public class InventoryLockFailedEvent {
    private String orderId;
    private String reason;


    public InventoryLockFailedEvent() {
    }
    public InventoryLockFailedEvent(String orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
