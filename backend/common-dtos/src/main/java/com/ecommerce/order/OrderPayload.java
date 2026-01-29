package com.ecommerce.order;

import java.math.BigDecimal;

public class OrderPayload {
    private String orderId;
    private BigDecimal totalAmount;
    private String currency;
    private int itemCount;
    private String cancellationReason;

    public OrderPayload() {
    }

    public OrderPayload(String orderId, BigDecimal totalAmount, String currency, int itemCount, String cancellationReason) {
        this.orderId = orderId;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.itemCount = itemCount;
        this.cancellationReason = cancellationReason;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}
