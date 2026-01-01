package com.ecommerce.orderservice.enums;
public enum OrderStatus {
    PLACED,     // Payment confirmed, saved in DB
    PACKED,     // Warehouse is working on it
    SHIPPED,    // Out for delivery
    DELIVERED,  // Done
    CANCELLED
}
