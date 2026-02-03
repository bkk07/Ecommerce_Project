package com.ecommerce.ratingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for rating eligibility information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityResponse {
    private Long id;
    private String orderId;
    private String sku;
    private String productName;
    private String imageUrl;
    private Boolean canRate;
    private Boolean hasRated;
    private LocalDateTime createdAt;
    private LocalDateTime ratedAt;
}
