package com.ecommerce.inventoryservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

/**
 * Resilience4j configuration for fault tolerance patterns.
 * 
 * Patterns implemented:
 * - Circuit Breaker: Prevents cascade failures
 * - Retry: Handles transient failures
 * - Rate Limiter: Protects against overload
 */
@Configuration
@Slf4j
public class ResilienceConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        // Default configuration for most services
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(Exception.class)
                .build();

        // Stricter configuration for Kafka operations
        CircuitBreakerConfig kafkaConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(60)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(2)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        return CircuitBreakerRegistry.of(Map.of(
                "default", defaultConfig,
                "kafkaProducer", kafkaConfig
        ));
    }

    @Bean
    public RetryRegistry retryRegistry() {
        // Default retry configuration
        RetryConfig defaultConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(Exception.class)
                .build();

        // Kafka-specific retry with longer waits
        RetryConfig kafkaConfig = RetryConfig.custom()
                .maxAttempts(5)
                .waitDuration(Duration.ofSeconds(1))
                .retryExceptions(Exception.class)
                .build();

        return RetryRegistry.of(Map.of(
                "default", defaultConfig,
                "kafkaProducer", kafkaConfig
        ));
    }

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        // Default rate limiter
        RateLimiterConfig defaultConfig = RateLimiterConfig.custom()
                .limitForPeriod(100)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(500))
                .build();

        // Rate limiter for read operations (more permissive)
        RateLimiterConfig readConfig = RateLimiterConfig.custom()
                .limitForPeriod(200)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(300))
                .build();

        // Stricter rate limiter for inventory write operations
        RateLimiterConfig inventoryConfig = RateLimiterConfig.custom()
                .limitForPeriod(50)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofMillis(1000))
                .build();

        return RateLimiterRegistry.of(Map.of(
                "default", defaultConfig,
                "inventoryRead", readConfig,
                "inventoryEndpoints", inventoryConfig
        ));
    }
}
