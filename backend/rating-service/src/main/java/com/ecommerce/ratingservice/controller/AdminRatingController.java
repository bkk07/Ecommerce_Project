package com.ecommerce.ratingservice.controller;

import com.ecommerce.ratingservice.dto.PagedResponse;
import com.ecommerce.ratingservice.dto.RatingResponse;
import com.ecommerce.ratingservice.enums.RatingStatus;
import com.ecommerce.ratingservice.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Admin REST Controller for rating moderation and management.
 * All endpoints require ADMIN role (validated by API Gateway).
 */
@RestController
@RequestMapping("/api/v1/admin/ratings")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Admin Ratings", description = "Admin endpoints for rating moderation and management")
public class AdminRatingController {

    private final RatingService ratingService;

    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Get all ratings with optional status filter (Admin only)
     */
    @GetMapping
    @Operation(summary = "Get all ratings", 
               description = "Retrieve all ratings with optional status filter. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ratings retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<PagedResponse<RatingResponse>> getAllRatings(
            @RequestParam(required = false) @Parameter(description = "Filter by status") RatingStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestHeader("X-Auth-User-Id") String adminId,
            @RequestHeader(value = "X-Auth-User-Role", required = false) String role) {
        log.info("Admin {} fetching ratings with status filter: {}, page: {}, size: {}", 
                adminId, status, page, size);
        PagedResponse<RatingResponse> ratings = ratingService.getAllRatings(status, page, validateSize(size));
        return ResponseEntity.ok(ratings);
    }

    /**
     * Get pending ratings for moderation
     */
    @GetMapping("/pending")
    @Operation(summary = "Get pending ratings", 
               description = "Retrieve all ratings pending moderation. Admin only.")
    public ResponseEntity<PagedResponse<RatingResponse>> getPendingRatings(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestHeader("X-Auth-User-Id") String adminId) {
        log.info("Admin {} fetching pending ratings", adminId);
        PagedResponse<RatingResponse> ratings = ratingService.getAllRatings(
                RatingStatus.PENDING, page, validateSize(size));
        return ResponseEntity.ok(ratings);
    }

    /**
     * Approve a rating
     */
    @PostMapping("/{ratingId}/approve")
    @Operation(summary = "Approve rating", 
               description = "Approve a pending rating. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rating approved"),
            @ApiResponse(responseCode = "404", description = "Rating not found"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<RatingResponse> approveRating(
            @PathVariable Long ratingId,
            @RequestHeader("X-Auth-User-Id") String adminId) {
        log.info("Admin {} approving rating ID: {}", adminId, ratingId);
        RatingResponse response = ratingService.moderateRating(ratingId, RatingStatus.APPROVED, adminId);
        return ResponseEntity.ok(response);
    }

    /**
     * Reject a rating
     */
    @PostMapping("/{ratingId}/reject")
    @Operation(summary = "Reject rating", 
               description = "Reject a rating. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rating rejected"),
            @ApiResponse(responseCode = "404", description = "Rating not found"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<RatingResponse> rejectRating(
            @PathVariable Long ratingId,
            @RequestHeader("X-Auth-User-Id") String adminId) {
        log.info("Admin {} rejecting rating ID: {}", adminId, ratingId);
        RatingResponse response = ratingService.moderateRating(ratingId, RatingStatus.REJECTED, adminId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update rating status
     */
    @PatchMapping("/{ratingId}/status")
    @Operation(summary = "Update rating status", 
               description = "Update a rating's moderation status. Admin only.")
    public ResponseEntity<RatingResponse> updateRatingStatus(
            @PathVariable Long ratingId,
            @RequestParam RatingStatus status,
            @RequestHeader("X-Auth-User-Id") String adminId) {
        log.info("Admin {} updating rating ID: {} to status: {}", adminId, ratingId, status);
        RatingResponse response = ratingService.moderateRating(ratingId, status, adminId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete any rating (Admin override - no ownership check)
     */
    @DeleteMapping("/{ratingId}")
    @Operation(summary = "Delete rating (Admin)", 
               description = "Delete any rating regardless of ownership. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Rating deleted"),
            @ApiResponse(responseCode = "404", description = "Rating not found"),
            @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    public ResponseEntity<Void> deleteRating(
            @PathVariable Long ratingId,
            @RequestHeader("X-Auth-User-Id") String adminId) {
        log.info("Admin {} deleting rating ID: {}", adminId, ratingId);
        ratingService.adminDeleteRating(ratingId, adminId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get a specific rating (Admin can view any rating)
     */
    @GetMapping("/{ratingId}")
    @Operation(summary = "Get rating details (Admin)", 
               description = "Get full details of any rating. Admin only.")
    public ResponseEntity<RatingResponse> getRating(
            @PathVariable Long ratingId,
            @RequestHeader("X-Auth-User-Id") String adminId) {
        log.debug("Admin {} fetching rating ID: {}", adminId, ratingId);
        RatingResponse response = ratingService.getRatingById(ratingId);
        return ResponseEntity.ok(response);
    }

    private int validateSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
