package com.ecommerce.inventory;
public class InventoryReleasedEvent {
    private String orderId;
    private String skuCode;
    private Integer quantity;

    public InventoryReleasedEvent() {
    }

    public InventoryReleasedEvent(String orderId, String skuCode, Integer quantity) {
        this.orderId = orderId;
        this.skuCode = skuCode;
        this.quantity = quantity;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
