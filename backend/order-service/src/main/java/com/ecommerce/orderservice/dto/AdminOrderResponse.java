package com.ecommerce.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailed order response for admin view - includes user info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderResponse {
    private String orderId;
    private String userId;
    private String paymentId;
    private String razorpayOrderId;
    private String status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private int itemCount;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
}
