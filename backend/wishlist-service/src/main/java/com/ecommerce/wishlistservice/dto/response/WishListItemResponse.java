package com.ecommerce.wishlistservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WishListItemResponse implements Serializable {

    private Long id;
    private String skuCode;
    private Long productId;
    private String name;
    private String imageUrl;
    private BigDecimal price;
    private LocalDateTime createdAt;
}
