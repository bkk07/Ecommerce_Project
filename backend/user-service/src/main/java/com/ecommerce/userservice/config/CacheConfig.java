package com.ecommerce.userservice.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;

/**
 * Cache configuration for admin analytics.
 * Caches user stats for 60 seconds to improve performance.
 */
@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    private CacheManager cacheManager;

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager manager = new ConcurrentMapCacheManager("userStats");
        this.cacheManager = manager;
        return manager;
    }

    /**
     * Evict the userStats cache every 60 seconds.
     * This ensures fresh data while still providing performance benefits.
     */
    @Scheduled(fixedRate = 60000) // 60 seconds
    public void evictUserStatsCache() {
        if (cacheManager != null && cacheManager.getCache("userStats") != null) {
            Objects.requireNonNull(cacheManager.getCache("userStats")).clear();
        }
    }
}
