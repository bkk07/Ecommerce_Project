package com.ecommerce.payment;

import java.math.BigDecimal;

public class PaymentInitiatedEvent {
    private String orderId;
    private String razorpayOrderId;
    private BigDecimal amount;
    private String userId;

    public PaymentInitiatedEvent() {
    }

    public PaymentInitiatedEvent(String orderId, String razorpayOrderId, BigDecimal amount, String userId) {
        this.orderId = orderId;
        this.razorpayOrderId = razorpayOrderId;
        this.amount = amount;
        this.userId = userId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
