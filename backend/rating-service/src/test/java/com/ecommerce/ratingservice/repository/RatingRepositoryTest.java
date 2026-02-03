package com.ecommerce.ratingservice.repository;

import com.ecommerce.ratingservice.entity.Rating;
import com.ecommerce.ratingservice.enums.RatingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for RatingRepository using in-memory H2 database.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RatingRepositoryTest {

    @Autowired
    private RatingRepository ratingRepository;

    private static final String USER_ID = "user-123";
    private static final String SKU = "PROD-001";
    private static final String ORDER_ID = "order-456";

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save and find rating by ID")
    void shouldSaveAndFindById() {
        // Given
        Rating rating = createRating(SKU, ORDER_ID, USER_ID, 5);

        // When
        Rating saved = ratingRepository.save(rating);
        Optional<Rating> found = ratingRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getSku()).isEqualTo(SKU);
        assertThat(found.get().getRating()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should find rating by order ID and SKU")
    void shouldFindByOrderIdAndSku() {
        // Given
        Rating rating = createRating(SKU, ORDER_ID, USER_ID, 5);
        ratingRepository.save(rating);

        // When
        Optional<Rating> found = ratingRepository.findByOrderIdAndSku(ORDER_ID, SKU);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo(ORDER_ID);
        assertThat(found.get().getSku()).isEqualTo(SKU);
    }

    @Test
    @DisplayName("Should find all ratings by user ID")
    void shouldFindByUserId() {
        // Given
        ratingRepository.save(createRating("SKU-1", "ORD-1", USER_ID, 5));
        ratingRepository.save(createRating("SKU-2", "ORD-2", USER_ID, 4));
        ratingRepository.save(createRating("SKU-3", "ORD-3", "other-user", 3));

        // When
        List<Rating> ratings = ratingRepository.findByUserId(USER_ID);

        // Then
        assertThat(ratings).hasSize(2);
        assertThat(ratings).allMatch(r -> r.getUserId().equals(USER_ID));
    }

    @Test
    @DisplayName("Should find ratings by SKU and status with pagination")
    void shouldFindBySkuAndStatusPaginated() {
        // Given
        for (int i = 0; i < 25; i++) {
            Rating rating = createRating(SKU, "ORD-" + i, "user-" + i, (i % 5) + 1);
            rating.setStatus(RatingStatus.APPROVED);
            ratingRepository.save(rating);
        }

        // When
        Page<Rating> page1 = ratingRepository.findBySkuAndStatus(
                SKU, RatingStatus.APPROVED, PageRequest.of(0, 10));
        Page<Rating> page2 = ratingRepository.findBySkuAndStatus(
                SKU, RatingStatus.APPROVED, PageRequest.of(1, 10));

        // Then
        assertThat(page1.getContent()).hasSize(10);
        assertThat(page1.getTotalElements()).isEqualTo(25);
        assertThat(page1.getTotalPages()).isEqualTo(3);
        assertThat(page2.getContent()).hasSize(10);
    }

    @Test
    @DisplayName("Should check if user rated a product")
    void shouldCheckIfUserRatedProduct() {
        // Given
        ratingRepository.save(createRating(SKU, ORDER_ID, USER_ID, 5));

        // When
        boolean hasRated = ratingRepository.existsByOrderIdAndSkuAndUserId(ORDER_ID, SKU, USER_ID);
        boolean notRated = ratingRepository.existsByOrderIdAndSkuAndUserId(ORDER_ID, SKU, "other-user");

        // Then
        assertThat(hasRated).isTrue();
        assertThat(notRated).isFalse();
    }

    @Test
    @DisplayName("Should calculate average rating for SKU")
    void shouldCalculateAverageRating() {
        // Given
        ratingRepository.save(createApprovedRating(SKU, "ORD-1", "user-1", 5));
        ratingRepository.save(createApprovedRating(SKU, "ORD-2", "user-2", 4));
        ratingRepository.save(createApprovedRating(SKU, "ORD-3", "user-3", 3));

        // When
        Double average = ratingRepository.getAverageRatingBySku(SKU);

        // Then
        assertThat(average).isEqualTo(4.0);
    }

    @Test
    @DisplayName("Should count approved ratings for SKU")
    void shouldCountApprovedRatings() {
        // Given
        ratingRepository.save(createApprovedRating(SKU, "ORD-1", "user-1", 5));
        ratingRepository.save(createApprovedRating(SKU, "ORD-2", "user-2", 4));
        
        Rating pending = createRating(SKU, "ORD-3", "user-3", 3);
        pending.setStatus(RatingStatus.PENDING);
        ratingRepository.save(pending);

        // When
        Long count = ratingRepository.getApprovedRatingCountBySku(SKU);

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should get rating distribution for SKU")
    void shouldGetRatingDistribution() {
        // Given
        ratingRepository.save(createApprovedRating(SKU, "ORD-1", "user-1", 5));
        ratingRepository.save(createApprovedRating(SKU, "ORD-2", "user-2", 5));
        ratingRepository.save(createApprovedRating(SKU, "ORD-3", "user-3", 4));
        ratingRepository.save(createApprovedRating(SKU, "ORD-4", "user-4", 3));

        // When
        List<Object[]> distribution = ratingRepository.getRatingDistributionBySku(SKU);

        // Then
        assertThat(distribution).hasSize(3); // 3 different rating values
    }

    // ==================== HELPER METHODS ====================

    private Rating createRating(String sku, String orderId, String userId, int ratingValue) {
        return Rating.builder()
                .sku(sku)
                .orderId(orderId)
                .userId(userId)
                .rating(ratingValue)
                .message("Test review")
                .isVerifiedPurchase(true)
                .status(RatingStatus.APPROVED)
                .build();
    }

    private Rating createApprovedRating(String sku, String orderId, String userId, int ratingValue) {
        Rating rating = createRating(sku, orderId, userId, ratingValue);
        rating.setStatus(RatingStatus.APPROVED);
        return rating;
    }
}
