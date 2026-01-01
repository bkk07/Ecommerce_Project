package com.ecommerce.orderservice.dto;

import lombok.Data;

@Data
public class OrderItemDto {
    private String skuCode;
    private String productName;
    private String price;
    private Integer quantity;
}