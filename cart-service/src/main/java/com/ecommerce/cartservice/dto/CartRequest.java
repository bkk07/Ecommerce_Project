package com.ecommerce.cartservice.dto;

import lombok.Data;

@Data
public class CartRequest {
    private String skuCode;
    private Integer quantity;
}