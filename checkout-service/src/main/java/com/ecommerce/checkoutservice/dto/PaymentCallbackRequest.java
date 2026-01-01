package com.ecommerce.checkoutservice.dto;

import lombok.Data;

@Data
public class PaymentCallbackRequest {

    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

}