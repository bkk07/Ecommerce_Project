package com.ecommerce.userservice.api.controller;

import com.ecommerce.userservice.api.dto.UserStatsResponse;
import com.ecommerce.userservice.service.AdminUserService;
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
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * Get comprehensive user statistics for admin dashboard.
     * Includes total counts and time-based growth metrics.
     * 
     * @param userId The admin user ID (injected by API Gateway)
     * @param userRole The user role (injected by API Gateway, must be ADMIN)
     * @return UserStatsResponse with all analytics data
     */
    @GetMapping("/stats")
    public ResponseEntity<UserStatsResponse> getUserStats(
            @RequestHeader(value = "X-Auth-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Auth-User-Role", required = false) String userRole) {
        
        log.info("Admin user {} requesting user statistics", userId);
        
        UserStatsResponse stats = adminUserService.getUserStats();
        
        return ResponseEntity.ok(stats);
    }
}
