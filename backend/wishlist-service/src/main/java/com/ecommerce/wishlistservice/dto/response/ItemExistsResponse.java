package com.ecommerce.wishlistservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ItemExistsResponse {

    private String skuCode;
    private boolean exists;
}
