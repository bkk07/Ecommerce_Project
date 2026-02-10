package com.ecommerce.orderservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j configuration for fault tolerance
 * Configures Circuit Breakers, Retry, and Time Limiters
 */
@Configuration
public class ResilienceConfig {

    /**
     * Default Circuit Breaker configuration
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 50% failure rate opens the circuit
                .slowCallRateThreshold(50) // 50% slow calls opens the circuit
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(5)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    /**
     * Default Retry configuration
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(Exception.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        return RetryRegistry.of(config);
    }

    /**
     * Default Time Limiter configuration
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))
                .cancelRunningFuture(true)
                .build();

        return TimeLimiterRegistry.of(config);
    }

    /**
     * Payment Service specific circuit breaker configuration
     */
    @Bean
    public CircuitBreakerConfigCustomizer paymentServiceCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer.of("paymentService",
                builder -> builder
                        .failureRateThreshold(40)
                        .slowCallRateThreshold(40)
                        .slowCallDurationThreshold(Duration.ofSeconds(3))
                        .waitDurationInOpenState(Duration.ofSeconds(60))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .slidingWindowSize(20)
                        .minimumNumberOfCalls(10));
    }

    /**
     * Database operations retry configuration
     */
    @Bean
    public RetryConfigCustomizer databaseRetryConfig() {
        return RetryConfigCustomizer.of("databaseRetry",
                builder -> builder
                        .maxAttempts(3)
                        .waitDuration(Duration.ofMillis(200))
                        .retryExceptions(Exception.class));
    }

    /**
     * Kafka operations retry configuration
     */
    @Bean
    public RetryConfigCustomizer kafkaRetryConfig() {
        return RetryConfigCustomizer.of("kafkaRetry",
                builder -> builder
                        .maxAttempts(5)
                        .waitDuration(Duration.ofMillis(1000))
                        .retryExceptions(Exception.class));
    }
}
