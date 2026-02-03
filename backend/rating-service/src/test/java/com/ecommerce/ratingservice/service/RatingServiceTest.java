package com.ecommerce.ratingservice.service;

import com.ecommerce.ratingservice.dto.CreateRatingRequest;
import com.ecommerce.ratingservice.dto.ProductRatingSummary;
import com.ecommerce.ratingservice.dto.RatingResponse;
import com.ecommerce.ratingservice.dto.UpdateRatingRequest;
import com.ecommerce.ratingservice.entity.Rating;
import com.ecommerce.ratingservice.entity.RatingEligibility;
import com.ecommerce.ratingservice.enums.RatingStatus;
import com.ecommerce.ratingservice.exception.DuplicateRatingException;
import com.ecommerce.ratingservice.exception.NotEligibleToRateException;
import com.ecommerce.ratingservice.exception.ResourceNotFoundException;
import com.ecommerce.ratingservice.exception.UnauthorizedAccessException;
import com.ecommerce.ratingservice.kafka.RatingEventProducer;
import com.ecommerce.ratingservice.mapper.RatingMapper;
import com.ecommerce.ratingservice.repository.RatingEligibilityRepository;
import com.ecommerce.ratingservice.repository.RatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RatingService.
 */
@ExtendWith(MockitoExtension.class)
class RatingServiceTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private RatingEligibilityRepository eligibilityRepository;

    @Mock
    private RatingEventProducer ratingEventProducer;

    @Mock
    private RatingMapper ratingMapper;

    @InjectMocks
    private RatingService ratingService;

    private static final String USER_ID = "user-123";
    private static final String ORDER_ID = "order-456";
    private static final String SKU = "PROD-001";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ratingService, "autoApprove", true);
        ReflectionTestUtils.setField(ratingService, "requireVerifiedPurchase", false);
        ReflectionTestUtils.setField(ratingService, "defaultPageSize", 20);
        ReflectionTestUtils.setField(ratingService, "maxPageSize", 100);
    }

    @Nested
    @DisplayName("Create Rating Tests")
    class CreateRatingTests {

        @Test
        @DisplayName("Should create rating successfully with eligibility")
        void shouldCreateRatingWithEligibility() {
            // Given
            CreateRatingRequest request = CreateRatingRequest.builder()
                    .sku(SKU)
                    .orderId(ORDER_ID)
                    .rating(5)
                    .message("Great product!")
                    .build();

            RatingEligibility eligibility = createEligibility();
            Rating savedRating = createRating(1L, 5);
            RatingResponse expectedResponse = createRatingResponse(1L, 5);

            when(eligibilityRepository.findByOrderId(ORDER_ID))
                    .thenReturn(List.of(eligibility));
            when(eligibilityRepository.existsByOrderIdAndSkuAndUserIdAndCanRateTrue(ORDER_ID, SKU, USER_ID))
                    .thenReturn(true);
            when(ratingRepository.existsByOrderIdAndSkuAndUserId(ORDER_ID, SKU, USER_ID))
                    .thenReturn(false);
            when(ratingRepository.save(any(Rating.class)))
                    .thenReturn(savedRating);
            when(ratingMapper.toRatingResponse(savedRating))
                    .thenReturn(expectedResponse);

            // When
            RatingResponse response = ratingService.createRating(request, USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getRating()).isEqualTo(5);
            verify(ratingRepository).save(any(Rating.class));
            verify(eligibilityRepository).markAsRated(eq(ORDER_ID), eq(SKU), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should create rating without eligibility (backward compatibility)")
        void shouldCreateRatingWithoutEligibility() {
            // Given
            CreateRatingRequest request = CreateRatingRequest.builder()
                    .sku(SKU)
                    .orderId(ORDER_ID)
                    .rating(4)
                    .message("Good product")
                    .build();

            Rating savedRating = createRating(1L, 4);
            RatingResponse expectedResponse = createRatingResponse(1L, 4);

            when(eligibilityRepository.findByOrderId(ORDER_ID))
                    .thenReturn(Collections.emptyList());
            when(ratingRepository.existsByOrderIdAndSkuAndUserId(ORDER_ID, SKU, USER_ID))
                    .thenReturn(false);
            when(ratingRepository.save(any(Rating.class)))
                    .thenReturn(savedRating);
            when(ratingMapper.toRatingResponse(savedRating))
                    .thenReturn(expectedResponse);

            // When
            RatingResponse response = ratingService.createRating(request, USER_ID);

            // Then
            assertThat(response).isNotNull();
            verify(ratingRepository).save(any(Rating.class));
        }

        @Test
        @DisplayName("Should throw exception when not eligible to rate")
        void shouldThrowWhenNotEligible() {
            // Given
            CreateRatingRequest request = CreateRatingRequest.builder()
                    .sku(SKU)
                    .orderId(ORDER_ID)
                    .rating(5)
                    .build();

            RatingEligibility eligibility = createEligibility();

            when(eligibilityRepository.findByOrderId(ORDER_ID))
                    .thenReturn(List.of(eligibility));
            when(eligibilityRepository.existsByOrderIdAndSkuAndUserIdAndCanRateTrue(ORDER_ID, SKU, USER_ID))
                    .thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> ratingService.createRating(request, USER_ID))
                    .isInstanceOf(NotEligibleToRateException.class);
        }

        @Test
        @DisplayName("Should throw exception when rating already exists")
        void shouldThrowWhenDuplicateRating() {
            // Given
            CreateRatingRequest request = CreateRatingRequest.builder()
                    .sku(SKU)
                    .orderId(ORDER_ID)
                    .rating(5)
                    .build();

            when(eligibilityRepository.findByOrderId(ORDER_ID))
                    .thenReturn(Collections.emptyList());
            when(ratingRepository.existsByOrderIdAndSkuAndUserId(ORDER_ID, SKU, USER_ID))
                    .thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> ratingService.createRating(request, USER_ID))
                    .isInstanceOf(DuplicateRatingException.class);
        }
    }

    @Nested
    @DisplayName("Update Rating Tests")
    class UpdateRatingTests {

        @Test
        @DisplayName("Should update rating successfully")
        void shouldUpdateRating() {
            // Given
            Long ratingId = 1L;
            UpdateRatingRequest request = UpdateRatingRequest.builder()
                    .rating(4)
                    .message("Updated review")
                    .build();

            Rating existingRating = createRating(ratingId, 5);
            Rating updatedRating = createRating(ratingId, 4);
            RatingResponse expectedResponse = createRatingResponse(ratingId, 4);

            when(ratingRepository.findById(ratingId))
                    .thenReturn(Optional.of(existingRating));
            when(ratingRepository.save(any(Rating.class)))
                    .thenReturn(updatedRating);
            when(ratingMapper.toRatingResponse(updatedRating))
                    .thenReturn(expectedResponse);

            // When
            RatingResponse response = ratingService.updateRating(ratingId, request, USER_ID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getRating()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should throw exception when rating not found")
        void shouldThrowWhenRatingNotFound() {
            // Given
            Long ratingId = 999L;
            UpdateRatingRequest request = UpdateRatingRequest.builder()
                    .rating(4)
                    .build();

            when(ratingRepository.findById(ratingId))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> ratingService.updateRating(ratingId, request, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when user doesn't own rating")
        void shouldThrowWhenNotOwner() {
            // Given
            Long ratingId = 1L;
            UpdateRatingRequest request = UpdateRatingRequest.builder()
                    .rating(4)
                    .build();

            Rating existingRating = createRating(ratingId, 5);
            existingRating.setUserId("other-user");

            when(ratingRepository.findById(ratingId))
                    .thenReturn(Optional.of(existingRating));

            // When/Then
            assertThatThrownBy(() -> ratingService.updateRating(ratingId, request, USER_ID))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }

    @Nested
    @DisplayName("Delete Rating Tests")
    class DeleteRatingTests {

        @Test
        @DisplayName("Should delete rating successfully")
        void shouldDeleteRating() {
            // Given
            Long ratingId = 1L;
            Rating existingRating = createRating(ratingId, 5);

            when(ratingRepository.findById(ratingId))
                    .thenReturn(Optional.of(existingRating));

            // When
            ratingService.deleteRating(ratingId, USER_ID);

            // Then
            verify(ratingRepository).delete(existingRating);
            verify(eligibilityRepository).unmarkAsRated(ORDER_ID, SKU);
        }

        @Test
        @DisplayName("Should throw exception when deleting others rating")
        void shouldThrowWhenDeletingOthersRating() {
            // Given
            Long ratingId = 1L;
            Rating existingRating = createRating(ratingId, 5);
            existingRating.setUserId("other-user");

            when(ratingRepository.findById(ratingId))
                    .thenReturn(Optional.of(existingRating));

            // When/Then
            assertThatThrownBy(() -> ratingService.deleteRating(ratingId, USER_ID))
                    .isInstanceOf(UnauthorizedAccessException.class);
        }
    }

    @Nested
    @DisplayName("Get Rating Summary Tests")
    class GetRatingSummaryTests {

        @Test
        @DisplayName("Should return correct rating summary")
        void shouldReturnRatingSummary() {
            // Given
            when(ratingRepository.getAverageRatingBySku(SKU))
                    .thenReturn(4.5);
            when(ratingRepository.getApprovedRatingCountBySku(SKU))
                    .thenReturn(100L);
            when(ratingRepository.getRatingDistributionBySku(SKU))
                    .thenReturn(List.of(
                            new Object[]{5, 50L},
                            new Object[]{4, 30L},
                            new Object[]{3, 15L},
                            new Object[]{2, 3L},
                            new Object[]{1, 2L}
                    ));

            // When
            ProductRatingSummary summary = ratingService.getProductRatingSummary(SKU);

            // Then
            assertThat(summary.getSku()).isEqualTo(SKU);
            assertThat(summary.getAverageRating()).isEqualTo(4.5);
            assertThat(summary.getTotalRatings()).isEqualTo(100L);
            assertThat(summary.getFiveStarCount()).isEqualTo(50L);
            assertThat(summary.getFourStarCount()).isEqualTo(30L);
        }

        @Test
        @DisplayName("Should return zero summary when no ratings")
        void shouldReturnZeroSummaryWhenNoRatings() {
            // Given
            when(ratingRepository.getAverageRatingBySku(SKU))
                    .thenReturn(null);
            when(ratingRepository.getApprovedRatingCountBySku(SKU))
                    .thenReturn(null);
            when(ratingRepository.getRatingDistributionBySku(SKU))
                    .thenReturn(Collections.emptyList());

            // When
            ProductRatingSummary summary = ratingService.getProductRatingSummary(SKU);

            // Then
            assertThat(summary.getAverageRating()).isEqualTo(0.0);
            assertThat(summary.getTotalRatings()).isEqualTo(0L);
        }
    }

    // ==================== HELPER METHODS ====================

    private Rating createRating(Long id, int ratingValue) {
        return Rating.builder()
                .id(id)
                .sku(SKU)
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .rating(ratingValue)
                .message("Test message")
                .isVerifiedPurchase(true)
                .status(RatingStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private RatingResponse createRatingResponse(Long id, int ratingValue) {
        return RatingResponse.builder()
                .id(id)
                .sku(SKU)
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .rating(ratingValue)
                .message("Test message")
                .isVerifiedPurchase(true)
                .status(RatingStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private RatingEligibility createEligibility() {
        return RatingEligibility.builder()
                .id(1L)
                .orderId(ORDER_ID)
                .userId(USER_ID)
                .sku(SKU)
                .productName("Test Product")
                .canRate(true)
                .hasRated(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
