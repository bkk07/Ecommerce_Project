package com.ecommerce.ratingservice.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis caching configuration for the Rating Service.
 * Provides cache management with configurable TTLs and error handling.
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig implements CachingConfigurer {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.timeout:2000}")
    private int redisTimeout;

    // Cache names
    public static final String PRODUCT_RATINGS_CACHE = "productRatings";
    public static final String PRODUCT_SUMMARY_CACHE = "productSummary";
    public static final String USER_RATINGS_CACHE = "userRatings";
    public static final String RATING_BY_ID_CACHE = "ratingById";
    public static final String ELIGIBILITY_CACHE = "eligibility";

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(redisTimeout))
                .build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }

    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = 
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer))
                .disableCachingNullValues();

        // Cache-specific configurations with different TTLs
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Product ratings summary - cache for 5 minutes (frequently accessed, needs freshness)
        cacheConfigurations.put(PRODUCT_SUMMARY_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Product ratings list - cache for 10 minutes
        cacheConfigurations.put(PRODUCT_RATINGS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // User ratings - cache for 15 minutes
        cacheConfigurations.put(USER_RATINGS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // Individual rating by ID - cache for 30 minutes
        cacheConfigurations.put(RATING_BY_ID_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // Eligibility info - cache for 1 hour (less frequently changing)
        cacheConfigurations.put(ELIGIBILITY_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        GenericJackson2JsonRedisSerializer serializer = 
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.setEnableTransactionSupport(true);
        template.afterPropertiesSet();
        
        return template;
    }

    /**
     * Custom cache error handler that logs errors but doesn't fail the operation.
     * This ensures the application continues to work even if Redis is unavailable.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, 
                    org.springframework.cache.Cache cache, Object key) {
                log.warn("Cache GET error for cache '{}' key '{}': {}", 
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, 
                    org.springframework.cache.Cache cache, Object key, Object value) {
                log.warn("Cache PUT error for cache '{}' key '{}': {}", 
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, 
                    org.springframework.cache.Cache cache, Object key) {
                log.warn("Cache EVICT error for cache '{}' key '{}': {}", 
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, 
                    org.springframework.cache.Cache cache) {
                log.warn("Cache CLEAR error for cache '{}': {}", 
                        cache.getName(), exception.getMessage());
            }
        };
    }
}
