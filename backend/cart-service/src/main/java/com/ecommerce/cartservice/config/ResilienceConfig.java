package com.ecommerce.cartservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j configuration for circuit breaker, retry, rate limiter, and time limiter patterns.
 * These patterns help the cart service handle failures gracefully and prevent cascading failures.
 */
@Configuration
public class ResilienceConfig {

    /**
     * Circuit Breaker Registry with custom configurations.
     * - Opens circuit after 50% failure rate in 10 calls
     * - Waits 30 seconds before trying half-open state
     * - Allows 5 calls in half-open state to test recovery
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        // Default configuration for most services
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(80)
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(5)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .build();

        // Stricter configuration for external services (like search-service)
        CircuitBreakerConfig externalServiceConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(40)
                .slowCallRateThreshold(70)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .permittedNumberOfCallsInHalfOpenState(3)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);
        registry.addConfiguration("externalService", externalServiceConfig);
        return registry;
    }

    /**
     * Retry Registry with custom configurations.
     * - Retries up to 3 times
     * - Waits 500ms between retries with exponential backoff
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig defaultConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(Exception.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        // Configuration for external service calls
        RetryConfig externalServiceConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(1000))
                .retryExceptions(Exception.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(defaultConfig);
        registry.addConfiguration("externalService", externalServiceConfig);
        return registry;
    }

    /**
     * Rate Limiter Registry for API throttling.
     * - Allows 100 requests per second by default
     * - Cart operations have specific limits
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        // Default rate limiter
        RateLimiterConfig defaultConfig = RateLimiterConfig.custom()
                .limitForPeriod(100)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(500))
                .build();

        // Stricter rate limit for cart modifications
        RateLimiterConfig cartModificationConfig = RateLimiterConfig.custom()
                .limitForPeriod(20)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(500))
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(defaultConfig);
        registry.addConfiguration("cartModification", cartModificationConfig);
        return registry;
    }

    /**
     * Time Limiter Registry for timeout handling.
     * - Default timeout of 5 seconds
     * - External service calls have longer timeout
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig defaultConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))
                .cancelRunningFuture(true)
                .build();

        TimeLimiterConfig externalServiceConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))
                .cancelRunningFuture(true)
                .build();

        TimeLimiterRegistry registry = TimeLimiterRegistry.of(defaultConfig);
        registry.addConfiguration("externalService", externalServiceConfig);
        return registry;
    }
}
