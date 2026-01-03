package com.ecommerce.paymentservice.enums;
public enum PaymentStatus {
    CREATED,    // Order created in Razorpay
    VERIFIED,   // Signature verified (Frontend callback)
    PAID,       // Money captured (Webhook confirmed)
    FAILED,
    REFUNDED// Transaction failed
}
