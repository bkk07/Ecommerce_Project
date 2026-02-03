package com.ecommerce.ratingservice.repository;

import com.ecommerce.ratingservice.entity.RatingEligibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RatingEligibilityRepository extends JpaRepository<RatingEligibility, Long> {

    /**
     * Find eligibility by orderId and sku
     */
    Optional<RatingEligibility> findByOrderIdAndSku(String orderId, String sku);

    /**
     * Find all eligible products for a user that can still be rated
     */
    List<RatingEligibility> findByUserIdAndCanRateTrueAndHasRatedFalse(String userId);

    /**
     * Find all products a user has rated
     */
    List<RatingEligibility> findByUserIdAndHasRatedTrue(String userId);

    /**
     * Check if user is eligible to rate a specific order+sku combination
     */
    boolean existsByOrderIdAndSkuAndUserIdAndCanRateTrue(String orderId, String sku, String userId);

    /**
     * Get all eligibility records for an order
     */
    List<RatingEligibility> findByOrderId(String orderId);

    /**
     * Get all eligibility records for a user and specific sku
     */
    List<RatingEligibility> findByUserIdAndSku(String userId, String sku);

    /**
     * Mark as rated
     */
    @Modifying
    @Query("UPDATE RatingEligibility e SET e.hasRated = true, e.ratedAt = :ratedAt WHERE e.orderId = :orderId AND e.sku = :sku")
    int markAsRated(@Param("orderId") String orderId, @Param("sku") String sku, @Param("ratedAt") LocalDateTime ratedAt);

    /**
     * Unmark as rated (when rating is deleted)
     */
    @Modifying
    @Query("UPDATE RatingEligibility e SET e.hasRated = false, e.ratedAt = null WHERE e.orderId = :orderId AND e.sku = :sku")
    int unmarkAsRated(@Param("orderId") String orderId, @Param("sku") String sku);
}
