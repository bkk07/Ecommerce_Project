package com.ecommerce.checkoutservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class InitiateCheckoutRequest {
    private String userId;

    // Optional: Only present if buying from cart
    private String cartId;

    // Optional: Only present if Direct Buy
    private List<CheckoutItem> items;
}