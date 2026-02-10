package com.ecommerce.checkoutservice.config;

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
 * These patterns help the checkout service handle failures gracefully and prevent cascading failures.
 */
@Configuration
public class ResilienceConfig {

    /**
     * Circuit Breaker Registry with custom configurations for different services.
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        // Default configuration for internal operations
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

        // Configuration for external service calls (product, order, cart)
        CircuitBreakerConfig externalServiceConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(40)
                .slowCallRateThreshold(70)
                .slowCallDurationThreshold(Duration.ofSeconds(5))
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
     * Retry Registry with configurations for transient failures.
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
                .waitDuration(Duration.ofSeconds(1))
                .retryExceptions(Exception.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        RetryRegistry registry = RetryRegistry.of(defaultConfig);
        registry.addConfiguration("externalService", externalServiceConfig);
        return registry;
    }

    /**
     * Rate Limiter Registry for checkout abuse prevention.
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        // Default rate limiter
        RateLimiterConfig defaultConfig = RateLimiterConfig.custom()
                .limitForPeriod(100)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(500))
                .build();

        // Stricter rate limit for checkout operations (prevent abuse)
        RateLimiterConfig checkoutConfig = RateLimiterConfig.custom()
                .limitForPeriod(10) // 10 checkouts per second per user
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(2))
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(defaultConfig);
        registry.addConfiguration("checkout", checkoutConfig);
        return registry;
    }

    /**
     * Time Limiter Registry for timeout handling.
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig defaultConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(10))
                .cancelRunningFuture(true)
                .build();

        TimeLimiterConfig externalServiceConfig = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(15))
                .cancelRunningFuture(true)
                .build();

        TimeLimiterRegistry registry = TimeLimiterRegistry.of(defaultConfig);
        registry.addConfiguration("externalService", externalServiceConfig);
        return registry;
    }
}
