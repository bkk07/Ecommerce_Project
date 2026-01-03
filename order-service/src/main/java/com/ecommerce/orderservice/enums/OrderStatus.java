package com.ecommerce.orderservice.enums;
public enum OrderStatus {
    PENDING,
    PAYMENT_READY, // Razorpay order created
    PLACED,     // Payment confirmed, saved in DB
    PACKED,     // Warehouse is working on it
    SHIPPED,    // Out for delivery
    DELIVERED,  // Done
    CANCEL_REQUESTED, // Saga started for cancellation
    CANCELLED   // Saga completed
}
