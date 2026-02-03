package com.ecommerce.userservice.api.controller;

import com.ecommerce.userservice.api.dto.UserStatsResponse;
import com.ecommerce.userservice.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin controller for user analytics and statistics.
 * All endpoints require ADMIN role (validated by API Gateway).
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - User Management", description = "Admin endpoints for user analytics and statistics")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * Get comprehensive user statistics for admin dashboard.
     * Includes total counts and time-based growth metrics.
     */
    @Operation(summary = "Get user statistics", 
               description = "Retrieves comprehensive user statistics for admin dashboard including total counts and growth metrics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserStatsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    @GetMapping("/stats")
    public ResponseEntity<UserStatsResponse> getUserStats(
            @Parameter(description = "Admin user ID (injected by API Gateway)", hidden = true)
            @RequestHeader(value = "X-Auth-User-Id", required = false) String userId,
            @Parameter(description = "User role (injected by API Gateway)", hidden = true)
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole) {
        
        log.info("Admin user {} requesting user statistics", userId);
        
        UserStatsResponse stats = adminUserService.getUserStats();
        
        return ResponseEntity.ok(stats);
    }
}
