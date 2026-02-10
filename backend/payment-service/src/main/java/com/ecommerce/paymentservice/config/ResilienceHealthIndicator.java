package com.ecommerce.paymentservice.config;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Resilience status monitoring component.
 * Provides detailed status information about all Resilience4j components.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResilienceHealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final BulkheadRegistry bulkheadRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    /**
     * Get comprehensive resilience health details
     */
    public Map<String, Object> getHealthDetails() {
        Map<String, Object> details = new HashMap<>();

        try {
            // Circuit Breakers Status
            Map<String, String> circuitBreakers = new HashMap<>();
            circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb ->
                    circuitBreakers.put(cb.getName(), cb.getState().name())
            );
            details.put("circuitBreakers", circuitBreakers);

            // Check if any circuit breaker is open
            boolean anyOpen = circuitBreakerRegistry.getAllCircuitBreakers()
                    .stream()
                    .anyMatch(cb -> cb.getState() == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN);

            // Bulkheads Status
            Map<String, Object> bulkheads = new HashMap<>();
            bulkheadRegistry.getAllBulkheads().forEach(bh -> {
                Map<String, Object> bhDetails = new HashMap<>();
                bhDetails.put("availableConcurrentCalls", bh.getMetrics().getAvailableConcurrentCalls());
                bhDetails.put("maxAllowedConcurrentCalls", bh.getMetrics().getMaxAllowedConcurrentCalls());
                bulkheads.put(bh.getName(), bhDetails);
            });
            details.put("bulkheads", bulkheads);

            // Rate Limiters Status
            Map<String, Object> rateLimiters = new HashMap<>();
            rateLimiterRegistry.getAllRateLimiters().forEach(rl -> {
                Map<String, Object> rlDetails = new HashMap<>();
                rlDetails.put("availablePermissions", rl.getMetrics().getAvailablePermissions());
                rlDetails.put("numberOfWaitingThreads", rl.getMetrics().getNumberOfWaitingThreads());
                rateLimiters.put(rl.getName(), rlDetails);
            });
            details.put("rateLimiters", rateLimiters);

            if (anyOpen) {
                details.put("status", "DEGRADED");
                details.put("message", "One or more circuit breakers are open");
            } else {
                details.put("status", "HEALTHY");
                details.put("message", "All resilience components are healthy");
            }

        } catch (Exception e) {
            log.error("Error getting resilience health details", e);
            details.put("status", "ERROR");
            details.put("error", e.getMessage());
        }

        return details;
    }

    /**
     * Check if all circuit breakers are closed (healthy)
     */
    public boolean isHealthy() {
        return circuitBreakerRegistry.getAllCircuitBreakers()
                .stream()
                .noneMatch(cb -> cb.getState() == io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN);
    }
}
