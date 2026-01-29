package com.ecommerce.checkoutservice.dto;
import com.ecommerce.checkout.CheckoutItem;
import lombok.Data;
import java.util.List;
@Data
public class InitiateCheckoutRequest {
    private Long userId; // Needed to pass to Order Service
    private boolean cartId;
    private List<CheckoutItem> items;
}
