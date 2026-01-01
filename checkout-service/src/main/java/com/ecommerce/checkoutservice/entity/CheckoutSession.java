package com.ecommerce.checkoutservice.entity;
import com.ecommerce.checkout.CheckoutItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
// Data lives for 20 minutes (1200s) - This is the "Data Key"
@RedisHash(value = "CheckoutSession", timeToLive = 1200)
public class CheckoutSession {
    @Id
    private String orderId; // Razorpay Order ID is the Key
    
    private Long userId; // Added back because Order Service needs it to link order to user
    
    private boolean cartId;

    private BigDecimal totalAmount;
    private List<CheckoutItem> items;
    private String status;
}
