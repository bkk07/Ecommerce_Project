package com.ecommerce.ratingservice.service;

import com.ecommerce.ratingservice.config.CacheConfig;
import com.ecommerce.ratingservice.dto.*;
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
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Production-grade service for managing product ratings.
 * Features: Caching, Circuit Breaker, Retry, Pagination, Proper Exception Handling
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService {

    private final RatingRepository ratingRepository;
    private final RatingEligibilityRepository eligibilityRepository;
    private final RatingEventProducer ratingEventProducer;
    private final RatingMapper ratingMapper;

    @Value("${rating.moderation.auto-approve:true}")
    private boolean autoApprove;

    @Value("${rating.moderation.require-verified-purchase:false}")
    private boolean requireVerifiedPurchase;

    @Value("${rating.pagination.default-page-size:20}")
    private int defaultPageSize;

    @Value("${rating.pagination.max-page-size:100}")
    private int maxPageSize;

    // ==================== CREATE OPERATIONS ====================

    /**
     * Create a new rating for a product.
     * Validates eligibility, checks for duplicates, and publishes events.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.PRODUCT_RATINGS_CACHE, key = "#request.sku"),
            @CacheEvict(value = CacheConfig.PRODUCT_SUMMARY_CACHE, key = "#request.sku"),
            @CacheEvict(value = CacheConfig.USER_RATINGS_CACHE, key = "#userId")
    })
    public RatingResponse createRating(CreateRatingRequest request, String userId) {
        log.info("Creating rating for SKU: {} by user: {} for order: {}",
                request.getSku(), userId, request.getOrderId());

        // Sanitize message input
        String sanitizedMessage = sanitizeInput(request.getMessage());

        // Check eligibility - user must have received the product
        List<RatingEligibility> orderEligibility = eligibilityRepository.findByOrderId(request.getOrderId());

        boolean isVerifiedPurchase = false;
        if (!orderEligibility.isEmpty()) {
            // Eligibility system is active for this order - enforce it
            boolean isEligible = eligibilityRepository.existsByOrderIdAndSkuAndUserIdAndCanRateTrue(
                    request.getOrderId(), request.getSku(), userId);

            if (!isEligible) {
                log.warn("User {} is not eligible to rate SKU {} for order {}",
                        userId, request.getSku(), request.getOrderId());
                throw new NotEligibleToRateException(userId, request.getOrderId(), request.getSku());
            }
            isVerifiedPurchase = true;
        } else if (requireVerifiedPurchase) {
            log.warn("Verified purchase required but no eligibility records for order {}", request.getOrderId());
            throw new NotEligibleToRateException("Verified purchase is required to rate this product");
        } else {
            log.info("No eligibility records for order {}. Allowing rating (backward compatibility)",
                    request.getOrderId());
        }

        // Check if rating already exists for this order and SKU
        if (ratingRepository.existsByOrderIdAndSkuAndUserId(request.getOrderId(), request.getSku(), userId)) {
            throw new DuplicateRatingException(request.getOrderId(), request.getSku());
        }

        Rating rating = Rating.builder()
                .sku(request.getSku())
                .orderId(request.getOrderId())
                .userId(userId)
                .rating(request.getRating())
                .message(sanitizedMessage)
                .isVerifiedPurchase(isVerifiedPurchase)
                .status(autoApprove ? RatingStatus.APPROVED : RatingStatus.PENDING)
                .build();

        Rating savedRating = ratingRepository.save(rating);
        log.info("Rating created with ID: {} for SKU: {} with status: {}",
                savedRating.getId(), savedRating.getSku(), savedRating.getStatus());

        // Mark eligibility as rated (if exists)
        if (!orderEligibility.isEmpty()) {
            eligibilityRepository.markAsRated(request.getOrderId(), request.getSku(), LocalDateTime.now());
        }

        // Publish rating event for search service (async, non-blocking)
        publishRatingEventAsync(savedRating.getSku());

        return ratingMapper.toRatingResponse(savedRating);
    }

    // ==================== UPDATE OPERATIONS ====================

    /**
     * Update an existing rating.
     * Only the owner can update their rating.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.RATING_BY_ID_CACHE, key = "#ratingId"),
            @CacheEvict(value = CacheConfig.USER_RATINGS_CACHE, key = "#userId")
    })
    public RatingResponse updateRating(Long ratingId, UpdateRatingRequest request, String userId) {
        log.info("Updating rating ID: {} by user: {}", ratingId, userId);

        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating", ratingId.toString()));

        // Verify ownership
        if (!rating.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("update", "ratings");
        }

        // Track if rating value changed for cache invalidation
        String sku = rating.getSku();
        boolean ratingValueChanged = false;

        if (request.getRating() != null && !request.getRating().equals(rating.getRating())) {
            rating.setRating(request.getRating());
            ratingValueChanged = true;
        }

        if (request.getMessage() != null) {
            rating.setMessage(sanitizeInput(request.getMessage()));
        }

        // If moderation is enabled and rating was approved, set back to pending on update
        if (!autoApprove && rating.getStatus() == RatingStatus.APPROVED) {
            rating.setStatus(RatingStatus.PENDING);
        }

        Rating updatedRating = ratingRepository.save(rating);
        log.info("Rating updated for SKU: {}", updatedRating.getSku());

        // Invalidate product caches if rating value changed
        if (ratingValueChanged) {
            publishRatingEventAsync(sku);
        }

        return ratingMapper.toRatingResponse(updatedRating);
    }

    // ==================== DELETE OPERATIONS ====================

    /**
     * Delete a rating.
     * Only the owner can delete their rating.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.RATING_BY_ID_CACHE, key = "#ratingId"),
            @CacheEvict(value = CacheConfig.USER_RATINGS_CACHE, key = "#userId")
    })
    public void deleteRating(Long ratingId, String userId) {
        log.info("Deleting rating ID: {} by user: {}", ratingId, userId);

        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating", ratingId.toString()));

        // Verify ownership
        if (!rating.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("delete", "ratings");
        }

        String sku = rating.getSku();
        String orderId = rating.getOrderId();

        ratingRepository.delete(rating);
        log.info("Rating deleted for SKU: {}", sku);

        // Unmark eligibility so user can rate again
        eligibilityRepository.unmarkAsRated(orderId, sku);

        // Publish event
        publishRatingEventAsync(sku);
    }

    // ==================== READ OPERATIONS ====================

    /**
     * Get rating by ID with caching.
     */
    @Cacheable(value = CacheConfig.RATING_BY_ID_CACHE, key = "#ratingId", unless = "#result == null")
    @Transactional(readOnly = true)
    public RatingResponse getRatingById(Long ratingId) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating", ratingId.toString()));
        return ratingMapper.toRatingResponse(rating);
    }

    /**
     * Get user's rating for a specific order and SKU.
     */
    @Transactional(readOnly = true)
    public RatingResponse getUserRatingForOrderAndSku(String orderId, String sku, String userId) {
        Optional<Rating> rating = ratingRepository.findByOrderIdAndSku(orderId, sku);

        if (rating.isPresent() && rating.get().getUserId().equals(userId)) {
            return ratingMapper.toRatingResponse(rating.get());
        }
        return null;
    }

    /**
     * Get all ratings by user with pagination.
     */
    @Cacheable(value = CacheConfig.USER_RATINGS_CACHE, key = "#userId + '-' + #page + '-' + #size",
            unless = "#result.content.isEmpty()")
    @Transactional(readOnly = true)
    public PagedResponse<RatingResponse> getUserRatingsPaged(String userId, int page, int size) {
        Pageable pageable = createPageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Rating> ratings = ratingRepository.findByUserId(userId, pageable);
        return PagedResponse.from(ratings, ratingMapper.toRatingResponseList(ratings.getContent()));
    }

    /**
     * Get all ratings by user (non-paginated, for backward compatibility).
     */
    @Transactional(readOnly = true)
    public List<RatingResponse> getUserRatings(String userId) {
        List<Rating> ratings = ratingRepository.findByUserId(userId);
        return ratingMapper.toRatingResponseList(ratings);
    }

    /**
     * Get all approved ratings for a product with pagination.
     */
    @Cacheable(value = CacheConfig.PRODUCT_RATINGS_CACHE, key = "#sku + '-' + #page + '-' + #size",
            unless = "#result.content.isEmpty()")
    @Transactional(readOnly = true)
    public PagedResponse<RatingResponse> getProductRatingsPaged(String sku, int page, int size) {
        Pageable pageable = createPageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Rating> ratings = ratingRepository.findBySkuAndStatus(sku, RatingStatus.APPROVED, pageable);
        return PagedResponse.from(ratings, ratingMapper.toRatingResponseList(ratings.getContent()));
    }

    /**
     * Get all approved ratings for a product (non-paginated).
     */
    @Transactional(readOnly = true)
    public List<RatingResponse> getProductRatings(String sku) {
        List<Rating> ratings = ratingRepository.findBySkuAndStatus(sku, RatingStatus.APPROVED);
        return ratingMapper.toRatingResponseList(ratings);
    }

    /**
     * Get rating summary for a product with caching.
     */
    @Cacheable(value = CacheConfig.PRODUCT_SUMMARY_CACHE, key = "#sku")
    @Transactional(readOnly = true)
    public ProductRatingSummary getProductRatingSummary(String sku) {
        Double averageRating = ratingRepository.getAverageRatingBySku(sku);
        Long totalRatings = ratingRepository.getApprovedRatingCountBySku(sku);
        List<Object[]> distribution = ratingRepository.getRatingDistributionBySku(sku);

        Map<Integer, Long> ratingCounts = new HashMap<>();
        for (Object[] row : distribution) {
            Integer ratingVal = (Integer) row[0];
            Long count = (Long) row[1];
            ratingCounts.put(ratingVal, count);
        }

        return ProductRatingSummary.builder()
                .sku(sku)
                .averageRating(averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0)
                .totalRatings(totalRatings != null ? totalRatings : 0L)
                .fiveStarCount(ratingCounts.getOrDefault(5, 0L))
                .fourStarCount(ratingCounts.getOrDefault(4, 0L))
                .threeStarCount(ratingCounts.getOrDefault(3, 0L))
                .twoStarCount(ratingCounts.getOrDefault(2, 0L))
                .oneStarCount(ratingCounts.getOrDefault(1, 0L))
                .build();
    }

    /**
     * Get user's ratings for a specific order.
     */
    @Transactional(readOnly = true)
    public List<RatingResponse> getUserRatingsForOrder(String orderId, String userId) {
        List<Rating> ratings = ratingRepository.findByOrderIdAndUserId(orderId, userId);
        return ratingMapper.toRatingResponseList(ratings);
    }

    // ==================== ELIGIBILITY OPERATIONS ====================

    /**
     * Check if user has already rated a product in an order.
     */
    @Transactional(readOnly = true)
    public boolean hasUserRatedProduct(String orderId, String sku, String userId) {
        return ratingRepository.existsByOrderIdAndSkuAndUserId(orderId, sku, userId);
    }

    /**
     * Check if user is eligible to rate a product for a specific order.
     */
    @Cacheable(value = CacheConfig.ELIGIBILITY_CACHE, key = "#orderId + '-' + #sku + '-' + #userId")
    @Transactional(readOnly = true)
    public boolean isEligibleToRate(String orderId, String sku, String userId) {
        return eligibilityRepository.existsByOrderIdAndSkuAndUserIdAndCanRateTrue(orderId, sku, userId);
    }

    /**
     * Get all products pending rating by a user (not yet rated).
     */
    @Transactional(readOnly = true)
    public List<EligibilityResponse> getPendingRatings(String userId) {
        List<RatingEligibility> pending = eligibilityRepository.findByUserIdAndCanRateTrueAndHasRatedFalse(userId);
        return ratingMapper.toEligibilityResponseList(pending);
    }

    /**
     * Get eligibility info for a specific order.
     */
    @Transactional(readOnly = true)
    public List<EligibilityResponse> getEligibilityForOrder(String orderId) {
        List<RatingEligibility> eligibility = eligibilityRepository.findByOrderId(orderId);
        return ratingMapper.toEligibilityResponseList(eligibility);
    }

    // ==================== ADMIN OPERATIONS ====================

    /**
     * Get all ratings with optional status filter (Admin).
     */
    @Transactional(readOnly = true)
    public PagedResponse<RatingResponse> getAllRatings(RatingStatus status, int page, int size) {
        Pageable pageable = createPageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Rating> ratings;

        if (status != null) {
            ratings = ratingRepository.findByStatus(status, pageable);
        } else {
            ratings = ratingRepository.findAll(pageable);
        }

        return PagedResponse.from(ratings, ratingMapper.toRatingResponseList(ratings.getContent()));
    }

    /**
     * Moderate a rating (Approve/Reject) - Admin only.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheConfig.RATING_BY_ID_CACHE, key = "#ratingId")
    })
    public RatingResponse moderateRating(Long ratingId, RatingStatus newStatus, String moderatorId) {
        log.info("Moderating rating ID: {} to status: {} by moderator: {}", ratingId, newStatus, moderatorId);

        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating", ratingId.toString()));

        RatingStatus oldStatus = rating.getStatus();
        rating.setStatus(newStatus);

        Rating updatedRating = ratingRepository.save(rating);
        log.info("Rating {} moderated from {} to {} by {}", ratingId, oldStatus, newStatus, moderatorId);

        // If status changed to/from APPROVED, update product summary
        if (oldStatus == RatingStatus.APPROVED || newStatus == RatingStatus.APPROVED) {
            publishRatingEventAsync(rating.getSku());
        }

        return ratingMapper.toRatingResponse(updatedRating);
    }

    /**
     * Delete a rating by admin (no ownership check).
     */
    @Transactional
    @CacheEvict(value = CacheConfig.RATING_BY_ID_CACHE, key = "#ratingId")
    public void adminDeleteRating(Long ratingId, String adminId) {
        log.info("Admin {} deleting rating ID: {}", adminId, ratingId);

        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating", ratingId.toString()));

        String sku = rating.getSku();
        String orderId = rating.getOrderId();

        ratingRepository.delete(rating);
        log.info("Rating {} deleted by admin {}", ratingId, adminId);

        // Unmark eligibility
        eligibilityRepository.unmarkAsRated(orderId, sku);

        // Publish event
        publishRatingEventAsync(sku);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Publish rating update event with circuit breaker and retry.
     */
    @CircuitBreaker(name = "kafkaProducer", fallbackMethod = "publishRatingEventFallback")
    @Retry(name = "kafkaProducer")
    public void publishRatingEventAsync(String sku) {
        try {
            ProductRatingSummary summary = getProductRatingSummaryInternal(sku);
            ratingEventProducer.publishRatingUpdated(sku, summary.getAverageRating(), summary.getTotalRatings());
        } catch (Exception e) {
            log.error("Failed to publish rating event for SKU: {}", sku, e);
            throw e;
        }
    }

    /**
     * Internal method to get summary without caching (to avoid circular cache issues).
     */
    private ProductRatingSummary getProductRatingSummaryInternal(String sku) {
        Double averageRating = ratingRepository.getAverageRatingBySku(sku);
        Long totalRatings = ratingRepository.getApprovedRatingCountBySku(sku);

        return ProductRatingSummary.builder()
                .sku(sku)
                .averageRating(averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : 0.0)
                .totalRatings(totalRatings != null ? totalRatings : 0L)
                .build();
    }

    /**
     * Fallback method when Kafka publishing fails.
     */
    public void publishRatingEventFallback(String sku, Exception e) {
        log.warn("Kafka publishing failed for SKU: {}. Event will be retried later. Error: {}",
                sku, e.getMessage());
    }

    /**
     * Create a pageable with size validation.
     */
    private Pageable createPageable(int page, int size, Sort sort) {
        int validatedSize = Math.min(Math.max(size, 1), maxPageSize);
        int validatedPage = Math.max(page, 0);
        return PageRequest.of(validatedPage, validatedSize, sort);
    }

    /**
     * Sanitize user input to prevent XSS and other attacks.
     */
    private String sanitizeInput(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }

        return input
                .replaceAll("<[^>]*>", "")
                .replaceAll("&", "&amp;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#x27;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .trim();
    }
}
