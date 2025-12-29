package com.ecommerce.product;
public record ProductUpdatedEvent(
    String productId,
    String name,
    boolean isActive) {
}
