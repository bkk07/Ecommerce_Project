package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.config.RazorpayHealthIndicator;
import com.ecommerce.paymentservice.config.ResilienceHealthIndicator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health and status endpoints for payment service monitoring
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Service health and status monitoring endpoints")
public class HealthController {

    private final RazorpayHealthIndicator razorpayHealthIndicator;
    private final ResilienceHealthIndicator resilienceHealthIndicator;

    @Operation(
            summary = "Get Razorpay gateway health",
            description = "Returns detailed health information about the Razorpay payment gateway integration"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Health information retrieved successfully")
    })
    @GetMapping("/razorpay")
    public ResponseEntity<Map<String, Object>> getRazorpayHealth() {
        return ResponseEntity.ok(razorpayHealthIndicator.getHealthDetails());
    }

    @Operation(
            summary = "Get resilience components status",
            description = "Returns status information about all Resilience4j components (circuit breakers, bulkheads, rate limiters)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resilience status retrieved successfully")
    })
    @GetMapping("/resilience")
    public ResponseEntity<Map<String, Object>> getResilienceHealth() {
        return ResponseEntity.ok(resilienceHealthIndicator.getHealthDetails());
    }

    @Operation(
            summary = "Get overall service status",
            description = "Returns a combined health status of all payment service components"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service status retrieved successfully")
    })
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();
        
        Map<String, Object> razorpayHealth = razorpayHealthIndicator.getHealthDetails();
        Map<String, Object> resilienceHealth = resilienceHealthIndicator.getHealthDetails();
        
        status.put("razorpay", razorpayHealth);
        status.put("resilience", resilienceHealth);
        
        // Determine overall status
        String razorpayStatus = (String) razorpayHealth.get("status");
        boolean resilienceHealthy = resilienceHealthIndicator.isHealthy();
        
        if ("UP".equals(razorpayStatus) && resilienceHealthy) {
            status.put("overall", "HEALTHY");
        } else if ("DOWN".equals(razorpayStatus)) {
            status.put("overall", "UNHEALTHY");
        } else {
            status.put("overall", "DEGRADED");
        }
        
        return ResponseEntity.ok(status);
    }
}
