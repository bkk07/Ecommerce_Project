package com.ecommerce.checkoutservice.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CheckoutResponse {
    private String razorpayOrderId;
    private BigDecimal amount;
    private String currency;
    private String status;
}
