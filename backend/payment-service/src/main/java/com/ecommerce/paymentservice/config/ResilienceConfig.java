package com.ecommerce.paymentservice.config;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
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
 * Resilience4j configuration for fault tolerance in payment operations
 */
@Configuration
public class ResilienceConfig {

    /**
     * Default Circuit Breaker configuration
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(50)
                .slowCallDurationThreshold(Duration.ofSeconds(3))
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
                .timeoutDuration(Duration.ofSeconds(10))
                .cancelRunningFuture(true)
                .build();

        return TimeLimiterRegistry.of(config);
    }

    /**
     * Razorpay API circuit breaker configuration - more strict for external API
     */
    @Bean
    public CircuitBreakerConfigCustomizer razorpayCircuitBreakerConfig() {
        return CircuitBreakerConfigCustomizer.of("razorpayApi",
                builder -> builder
                        .failureRateThreshold(40)
                        .slowCallRateThreshold(40)
                        .slowCallDurationThreshold(Duration.ofSeconds(5))
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

    /**
     * Razorpay API retry configuration - with exponential backoff
     */
    @Bean
    public RetryConfigCustomizer razorpayRetryConfig() {
        return RetryConfigCustomizer.of("razorpayRetry",
                builder -> builder
                        .maxAttempts(3)
                        .waitDuration(Duration.ofMillis(1000))
                        .retryExceptions(Exception.class)
                        .ignoreExceptions(IllegalArgumentException.class, SecurityException.class));
    }

    // ==================== Bulkhead Configuration ====================

    /**
     * Default Bulkhead configuration for thread isolation
     */
    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(50)
                .maxWaitDuration(Duration.ofMillis(100))
                .build();

        return BulkheadRegistry.of(config);
    }

    /**
     * Razorpay API Bulkhead - limited concurrent calls to external API
     */
    @Bean
    public BulkheadConfigCustomizer razorpayBulkheadConfig() {
        return BulkheadConfigCustomizer.of("razorpayApi",
                builder -> builder
                        .maxConcurrentCalls(25)
                        .maxWaitDuration(Duration.ZERO));
    }

    /**
     * Payment processing Bulkhead
     */
    @Bean
    public BulkheadConfigCustomizer paymentProcessingBulkheadConfig() {
        return BulkheadConfigCustomizer.of("paymentProcessing",
                builder -> builder
                        .maxConcurrentCalls(50)
                        .maxWaitDuration(Duration.ofMillis(100)));
    }

    /**
     * Webhook processing Bulkhead - higher concurrency for webhooks
     */
    @Bean
    public BulkheadConfigCustomizer webhookBulkheadConfig() {
        return BulkheadConfigCustomizer.of("webhookProcessing",
                builder -> builder
                        .maxConcurrentCalls(100)
                        .maxWaitDuration(Duration.ZERO));
    }

    // ==================== Rate Limiter Configuration ====================

    /**
     * Default Rate Limiter configuration
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(100)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ZERO)
                .build();

        return RateLimiterRegistry.of(config);
    }

    /**
     * Payment API Rate Limiter - protect against abuse
     */
    @Bean
    public RateLimiterConfigCustomizer paymentApiRateLimiterConfig() {
        return RateLimiterConfigCustomizer.of("paymentApi",
                builder -> builder
                        .limitForPeriod(100)
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .timeoutDuration(Duration.ZERO));
    }

    /**
     * Verification API Rate Limiter - stricter limits
     */
    @Bean
    public RateLimiterConfigCustomizer verificationRateLimiterConfig() {
        return RateLimiterConfigCustomizer.of("verificationApi",
                builder -> builder
                        .limitForPeriod(50)
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .timeoutDuration(Duration.ZERO));
    }

    /**
     * Refund API Rate Limiter - very strict limits
     */
    @Bean
    public RateLimiterConfigCustomizer refundRateLimiterConfig() {
        return RateLimiterConfigCustomizer.of("refundApi",
                builder -> builder
                        .limitForPeriod(10)
                        .limitRefreshPeriod(Duration.ofSeconds(1))
                        .timeoutDuration(Duration.ZERO));
    }
}
