package com.ecommerce.payment;
public class PaymentCreateRequest {
    private Long amount;
    private Long userId;
    public PaymentCreateRequest() {
    }
    public PaymentCreateRequest(Long amount, Long userId) {
        this.amount = amount;
        this.userId = userId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
