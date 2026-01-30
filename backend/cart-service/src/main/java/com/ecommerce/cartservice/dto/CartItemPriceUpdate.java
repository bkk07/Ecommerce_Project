package com.ecommerce.cartservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartItemPriceUpdate {
    private String skuCode;
    private BigDecimal price;
}