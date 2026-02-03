package com.ecommerce.ratingservice.repository;

import com.ecommerce.ratingservice.entity.Rating;
import com.ecommerce.ratingservice.enums.RatingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Rating entity with pagination support.
 */
@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    // Find rating by order and SKU (unique constraint)
    Optional<Rating> findByOrderIdAndSku(String orderId, String sku);

    // Find all ratings by user (non-paginated)
    List<Rating> findByUserId(String userId);

    // Find all ratings by user (paginated)
    Page<Rating> findByUserId(String userId, Pageable pageable);

    // Find all ratings for a product (SKU) with approved status (non-paginated)
    List<Rating> findBySkuAndStatus(String sku, RatingStatus status);

    // Find all ratings for a product (SKU) with approved status (paginated)
    Page<Rating> findBySkuAndStatus(String sku, RatingStatus status, Pageable pageable);

    // Find all ratings for a product (SKU)
    List<Rating> findBySku(String sku);

    // Find all ratings by status (paginated) - for admin
    Page<Rating> findByStatus(RatingStatus status, Pageable pageable);

    // Check if user has rated a product in an order
    boolean existsByOrderIdAndSkuAndUserId(String orderId, String sku, String userId);

    // Get average rating for a SKU (only approved ratings)
    @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.sku = :sku AND r.status = 'APPROVED'")
    Double getAverageRatingBySku(@Param("sku") String sku);

    // Get total count of approved ratings for a SKU
    @Query("SELECT COUNT(r) FROM Rating r WHERE r.sku = :sku AND r.status = 'APPROVED'")
    Long getApprovedRatingCountBySku(@Param("sku") String sku);

    // Get rating distribution for a SKU
    @Query("SELECT r.rating, COUNT(r) FROM Rating r WHERE r.sku = :sku AND r.status = 'APPROVED' GROUP BY r.rating")
    List<Object[]> getRatingDistributionBySku(@Param("sku") String sku);

    // Find all ratings by user for specific order
    List<Rating> findByOrderIdAndUserId(String orderId, String userId);

    // Count pending ratings for moderation
    @Query("SELECT COUNT(r) FROM Rating r WHERE r.status = 'PENDING'")
    Long countPendingRatings();

    // Find ratings by multiple SKUs (for batch operations)
    @Query("SELECT r FROM Rating r WHERE r.sku IN :skus AND r.status = 'APPROVED'")
    List<Rating> findApprovedRatingsBySkus(@Param("skus") List<String> skus);
}
