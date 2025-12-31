package com.ecommerce.checkoutservice.dto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentVerificationRequest {
    private String orderId;
    private String paymentId;
    private String signature;
}