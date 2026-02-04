package com.ecommerce.cartservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartRequest {
    @NotBlank(message = "SKU code is required")
    private String skuCode;
    
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
    
    private String imageUrl;
    
    @Positive(message = "Price must be positive")
    private BigDecimal price;
}