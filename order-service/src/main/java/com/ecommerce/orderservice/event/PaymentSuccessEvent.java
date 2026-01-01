package com.ecommerce.orderservice.event;

import com.ecommerce.orderservice.dto.OrderItemDto;
import lombok.Data;
import java.util.List;

@Data
public class PaymentSuccessEvent {
    private String checkoutId;
    private String razorpayPaymentId;
    private String userId;
    private String amount;
    private String addressJson; // Passed from Checkout -> Payment -> Order
    private List<OrderItemDto> items;
}