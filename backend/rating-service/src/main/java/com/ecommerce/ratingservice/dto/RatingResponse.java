package com.ecommerce.ratingservice.dto;

import com.ecommerce.ratingservice.enums.RatingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponse {
    private Long id;
    private String sku;
    private String orderId;
    private String userId;
    private Integer rating;
    private String message;
    private Boolean isVerifiedPurchase;
    private RatingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
