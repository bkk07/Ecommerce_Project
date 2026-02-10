package com.ecommerce.paymentservice.config;

import com.razorpay.RazorpayClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom Health monitoring component for Razorpay Payment Gateway connectivity.
 * Publishes liveness state changes based on circuit breaker status.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RazorpayHealthIndicator {

    private final RazorpayClient razorpayClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ApplicationEventPublisher eventPublisher;

    private CircuitBreaker.State lastKnownState = CircuitBreaker.State.CLOSED;

    /**
     * Periodically check Razorpay circuit breaker status and publish liveness events
     */
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void checkRazorpayHealth() {
        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("razorpayApi");
            CircuitBreaker.State currentState = circuitBreaker.getState();

            if (currentState != lastKnownState) {
                log.info("Razorpay circuit breaker state changed from {} to {}", lastKnownState, currentState);
                lastKnownState = currentState;

                if (currentState == CircuitBreaker.State.OPEN) {
                    log.warn("Razorpay circuit breaker is OPEN - publishing BROKEN liveness state");
                    AvailabilityChangeEvent.publish(eventPublisher, this, LivenessState.BROKEN);
                } else if (currentState == CircuitBreaker.State.CLOSED) {
                    log.info("Razorpay circuit breaker is CLOSED - publishing CORRECT liveness state");
                    AvailabilityChangeEvent.publish(eventPublisher, this, LivenessState.CORRECT);
                }
            }
        } catch (Exception e) {
            log.error("Error checking Razorpay health", e);
        }
    }

    /**
     * Get current health details as a map (can be used by custom endpoints)
     */
    public Map<String, Object> getHealthDetails() {
        Map<String, Object> details = new HashMap<>();

        try {
            CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("razorpayApi");
            CircuitBreaker.State state = circuitBreaker.getState();
            details.put("circuitBreakerState", state.name());
            details.put("gateway", "Razorpay");

            // Get circuit breaker metrics
            CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            details.put("failureRate", String.format("%.2f%%", metrics.getFailureRate()));
            details.put("slowCallRate", String.format("%.2f%%", metrics.getSlowCallRate()));
            details.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());
            details.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
            details.put("numberOfSlowCalls", metrics.getNumberOfSlowCalls());

            if (state == CircuitBreaker.State.OPEN) {
                details.put("status", "DOWN");
                details.put("message", "Circuit breaker is open - Razorpay API experiencing issues");
            } else if (state == CircuitBreaker.State.HALF_OPEN) {
                details.put("status", "UNKNOWN");
                details.put("message", "Circuit breaker is half-open - Testing Razorpay API");
            } else {
                details.put("status", "UP");
                details.put("message", "Razorpay API is healthy");
            }

        } catch (Exception e) {
            log.error("Error getting Razorpay health details", e);
            details.put("status", "DOWN");
            details.put("error", e.getMessage());
        }

        return details;
    }
}
