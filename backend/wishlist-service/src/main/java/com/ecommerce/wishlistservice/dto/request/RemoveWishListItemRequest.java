package com.ecommerce.wishlistservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RemoveWishListItemRequest {

    @NotBlank(message = "SKU code is required")
    private String skuCode;
}
