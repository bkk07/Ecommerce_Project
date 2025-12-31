package com.ecommerce.checkoutservice.entity;


import com.ecommerce.checkoutservice.dto.CheckoutItem;
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
// Data lives for 15 minutes (900s) then auto-deletes
@RedisHash(value = "CheckoutSession", timeToLive = 900)
public class CheckoutSession {

    @Id
    private String orderId; // Razorpay Order ID is the Key

    private String userId;

    // Logic: If this is NOT null, we delete this cart after success.
    private String cartId;

    private BigDecimal totalAmount;
    private List<CheckoutItem> items;
    private String status;
}