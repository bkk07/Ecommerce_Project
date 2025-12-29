package com.ecommerce.product;

import java.math.BigDecimal;
public record ProductCreatedEvent (Long productId,String name,String categoryName,BigDecimal basePrice){

}
