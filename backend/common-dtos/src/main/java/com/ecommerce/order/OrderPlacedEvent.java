package com.ecommerce.order;
public class OrderPlacedEvent {
    private String orderId;
    private String userId;

    public OrderPlacedEvent(String orderId, String userId) {
        this.orderId = orderId;
        this.userId = userId;
    }

    public OrderPlacedEvent(){}


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
}

