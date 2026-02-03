package com.ecommerce.ratingservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores rating eligibility for users who have purchased and received products.
 * This ensures only verified purchasers can rate products.
 */
@Entity
@Table(name = "rating_eligibility", indexes = {
        @Index(name = "idx_eligibility_user_sku", columnList = "userId, sku"),
        @Index(name = "idx_eligibility_order", columnList = "orderId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_eligibility_order_sku", columnNames = {"orderId", "sku"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingEligibility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String productName;

    private String imageUrl;

    @Column(nullable = false)
    private Boolean canRate;

    @Column(nullable = false)
    private Boolean hasRated;

    private LocalDateTime createdAt;
    private LocalDateTime ratedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (canRate == null) canRate = true;
        if (hasRated == null) hasRated = false;
    }
}
