package com.ecommerce.checkoutservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutItem {
    private String productId;
    private String productName;
    private int quantity;
    private BigDecimal price;
    private String skuCode;
}