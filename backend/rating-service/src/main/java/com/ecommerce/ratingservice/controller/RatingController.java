package com.ecommerce.ratingservice.controller;

import com.ecommerce.ratingservice.dto.*;
import com.ecommerce.ratingservice.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing product ratings.
 * Provides endpoints for creating, reading, updating, and deleting ratings.
 */
@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Ratings", description = "Product rating management endpoints")
public class RatingController {

    private final RatingService ratingService;

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * Get all approved ratings for a product (public endpoint)
     */
    @GetMapping("/product/{sku}")
    @Operation(summary = "Get product ratings", 
               description = "Retrieves all approved ratings for a product. Public endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ratings retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PagedResponse<RatingResponse>> getProductRatings(
            @PathVariable @Parameter(description = "Product SKU") String sku,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.debug("Getting ratings for product SKU: {}, page: {}, size: {}", sku, page, size);
        PagedResponse<RatingResponse> ratings = ratingService.getProductRatingsPaged(sku, page, validateSize(size));
        return ResponseEntity.ok(ratings);
    }

    /**
     * Get rating summary for a product (public endpoint)
     */
    @GetMapping("/product/{sku}/summary")
    @Operation(summary = "Get product rating summary", 
               description = "Retrieves rating statistics for a product. Public endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Summary retrieved successfully")
    })
    public ResponseEntity<ProductRatingSummary> getProductRatingSummary(
            @PathVariable @Parameter(description = "Product SKU") String sku) {
        log.debug("Getting rating summary for product SKU: {}", sku);
        ProductRatingSummary summary = ratingService.getProductRatingSummary(sku);
        return ResponseEntity.ok(summary);
    }

    /**
     * Get rating by ID (public endpoint)
     */
    @GetMapping("/{ratingId}")
    @Operation(summary = "Get rating by ID", description = "Retrieves a specific rating by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rating found"),
            @ApiResponse(responseCode = "404", description = "Rating not found")
    })
    public ResponseEntity<RatingResponse> getRatingById(
            @PathVariable @Parameter(description = "Rating ID") Long ratingId) {
        log.debug("Getting rating by ID: {}", ratingId);
        RatingResponse response = ratingService.getRatingById(ratingId);
        return ResponseEntity.ok(response);
    }

    // ==================== AUTHENTICATED ENDPOINTS ====================

    /**
     * Create a new rating for a product
     */
    @PostMapping
    @Operation(summary = "Create rating", 
               description = "Create a new rating for a product. Requires authentication.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rating created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "403", description = "Not eligible to rate"),
            @ApiResponse(responseCode = "409", description = "Rating already exists")
    })
    public ResponseEntity<RatingResponse> createRating(
            @Valid @RequestBody CreateRatingRequest request,
            @RequestHeader("X-Auth-User-Id") @Parameter(description = "User ID from gateway") String userId) {
        log.info("Creating rating for SKU: {} by user: {}", request.getSku(), userId);
        RatingResponse response = ratingService.createRating(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing rating
     */
    @PutMapping("/{ratingId}")
    @Operation(summary = "Update rating", 
               description = "Update an existing rating. Only the owner can update.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rating updated successfully"),
            @ApiResponse(responseCode = "403", description = "Not authorized to update"),
            @ApiResponse(responseCode = "404", description = "Rating not found")
    })
    public ResponseEntity<RatingResponse> updateRating(
            @PathVariable Long ratingId,
            @Valid @RequestBody UpdateRatingRequest request,
            @RequestHeader("X-Auth-User-Id") String userId) {
        log.info("Updating rating ID: {} by user: {}", ratingId, userId);
        RatingResponse response = ratingService.updateRating(ratingId, request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a rating
     */
    @DeleteMapping("/{ratingId}")
    @Operation(summary = "Delete rating", 
               description = "Delete an existing rating. Only the owner can delete.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Rating deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete"),
            @ApiResponse(responseCode = "404", description = "Rating not found")
    })
    public ResponseEntity<Void> deleteRating(
            @PathVariable Long ratingId,
            @RequestHeader("X-Auth-User-Id") String userId) {
        log.info("Deleting rating ID: {} by user: {}", ratingId, userId);
        ratingService.deleteRating(ratingId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get user's rating for a specific order and SKU
     */
    @GetMapping("/order/{orderId}/sku/{sku}")
    @Operation(summary = "Get user rating for order item", 
               description = "Get the current user's rating for a specific order and product")
    public ResponseEntity<RatingResponse> getUserRatingForOrderAndSku(
            @PathVariable String orderId,
            @PathVariable String sku,
            @RequestHeader("X-Auth-User-Id") String userId) {
        log.debug("Getting user rating for order: {}, sku: {}", orderId, sku);
        RatingResponse response = ratingService.getUserRatingForOrderAndSku(orderId, sku, userId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Get all ratings by current user
     */
    @GetMapping("/my-ratings")
    @Operation(summary = "Get my ratings", 
               description = "Get all ratings submitted by the current user")
    public ResponseEntity<List<RatingResponse>> getMyRatings(
            @RequestHeader("X-Auth-User-Id") String userId) {
        log.debug("Getting all ratings for user: {}", userId);
        List<RatingResponse> ratings = ratingService.getUserRatings(userId);
        return ResponseEntity.ok(ratings);
    }

    /**
     * Get all ratings by current user (paginated)
     */
    @GetMapping("/my-ratings/paged")
    @Operation(summary = "Get my ratings (paginated)", 
               description = "Get all ratings submitted by the current user with pagination")
    public ResponseEntity<PagedResponse<RatingResponse>> getMyRatingsPaged(
            @RequestHeader("X-Auth-User-Id") String userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.debug("Getting paginated ratings for user: {}, page: {}, size: {}", userId, page, size);
        PagedResponse<RatingResponse> ratings = ratingService.getUserRatingsPaged(userId, page, validateSize(size));
        return ResponseEntity.ok(ratings);
    }

    /**
     * Get user's ratings for a specific order
     */
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get ratings for order", 
               description = "Get all ratings submitted by the current user for a specific order")
    public ResponseEntity<List<RatingResponse>> getUserRatingsForOrder(
            @PathVariable String orderId,
            @RequestHeader("X-Auth-User-Id") String userId) {
        log.debug("Getting ratings for order: {} by user: {}", orderId, userId);
        List<RatingResponse> ratings = ratingService.getUserRatingsForOrder(orderId, userId);
        return ResponseEntity.ok(ratings);
    }

    // ==================== ELIGIBILITY ENDPOINTS ====================

    /**
     * Check if user has rated a product in an order
     */
    @GetMapping("/check")
    @Operation(summary = "Check if rated", 
               description = "Check if the current user has already rated a product in an order")
    public ResponseEntity<Boolean> hasUserRatedProduct(
            @RequestParam String orderId,
            @RequestParam String sku,
            @RequestHeader("X-Auth-User-Id") String userId) {
        boolean hasRated = ratingService.hasUserRatedProduct(orderId, sku, userId);
        return ResponseEntity.ok(hasRated);
    }

    /**
     * Check if user is eligible to rate a product for a specific order
     */
    @GetMapping("/eligible")
    @Operation(summary = "Check eligibility", 
               description = "Check if the current user is eligible to rate a product")
    public ResponseEntity<Boolean> isEligibleToRate(
            @RequestParam String orderId,
            @RequestParam String sku,
            @RequestHeader("X-Auth-User-Id") String userId) {
        boolean isEligible = ratingService.isEligibleToRate(orderId, sku, userId);
        return ResponseEntity.ok(isEligible);
    }

    /**
     * Get all products pending rating by user (not yet rated)
     */
    @GetMapping("/pending")
    @Operation(summary = "Get pending ratings", 
               description = "Get all products the user is eligible to rate but hasn't rated yet")
    public ResponseEntity<List<EligibilityResponse>> getPendingRatings(
            @RequestHeader("X-Auth-User-Id") String userId) {
        log.debug("Getting pending ratings for user: {}", userId);
        List<EligibilityResponse> response = ratingService.getPendingRatings(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get eligibility info for a specific order
     */
    @GetMapping("/eligibility/order/{orderId}")
    @Operation(summary = "Get order eligibility", 
               description = "Get rating eligibility info for all items in an order")
    public ResponseEntity<List<EligibilityResponse>> getEligibilityForOrder(
            @PathVariable String orderId,
            @RequestHeader("X-Auth-User-Id") String userId) {
        log.debug("Getting eligibility for order: {}", orderId);
        List<EligibilityResponse> response = ratingService.getEligibilityForOrder(orderId);
        return ResponseEntity.ok(response);
    }

    // ==================== HELPER METHODS ====================

    private int validateSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
