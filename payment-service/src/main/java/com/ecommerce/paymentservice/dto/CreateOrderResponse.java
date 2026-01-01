package com.ecommerce.paymentservice.dto;

import lombok.Data;

@Data
public class CreateOrderResponse {
    private String razorpayOrderId;
    private String razorpayKeyId; // Safe to expose public key
    private Long amount; // In Paise
    private String currency;
}
