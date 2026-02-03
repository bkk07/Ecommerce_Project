package com.ecommerce.ratingservice.entity;

import com.ecommerce.ratingservice.enums.RatingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ratings", indexes = {
        @Index(name = "idx_sku", columnList = "sku"),
        @Index(name = "idx_order_id", columnList = "orderId"),
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_sku_user", columnList = "sku, userId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_order_sku", columnNames = {"orderId", "sku"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Integer rating; // 1-5 stars

    @Column(length = 1000)
    private String message;

    @Column(nullable = false)
    private Boolean isVerifiedPurchase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RatingStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
