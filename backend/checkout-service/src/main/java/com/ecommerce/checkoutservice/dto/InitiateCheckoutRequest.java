package com.ecommerce.checkoutservice.dto;

import com.ecommerce.checkout.CheckoutItem;
import com.ecommerce.order.AddressDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class InitiateCheckoutRequest {
    private Long userId; // Set from header, not from request body
    
    private boolean cartId; // True if checking out from cart
    
    @Valid
    @Size(max = 50, message = "Cannot checkout more than 50 items at once")
    private List<CheckoutItem> items; // Required only if cartId is false
    
    @NotBlank(message = "Shipping address is required")
    @Size(max = 500, message = "Shipping address cannot exceed 500 characters")
    private String shippingAddress;
    
    // Idempotency key to prevent duplicate checkout submissions
    private String idempotencyKey;
}
