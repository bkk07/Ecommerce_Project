package com.ecommerce.wishlistservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WishListResponse implements Serializable {

    private Long id;
    private Long userId;
    private LocalDateTime createdAt;
    private List<WishListItemResponse> items;
    private int totalItems;
}
