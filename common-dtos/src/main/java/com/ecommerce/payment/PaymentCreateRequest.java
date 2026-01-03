package com.ecommerce.payment;

import java.math.BigDecimal;

public class PaymentCreateRequest {
    private BigDecimal amount;

//    private Long userId;
//    private String orderId; // Added orderId
    public PaymentCreateRequest() {
    }
    public PaymentCreateRequest(BigDecimal amount) {
        this.amount = amount;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }


}
