package com.ecommerce.checkoutservice.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories(basePackages = "com.ecommerce.checkout.repository")
public class RedisConfig {
    // Spring Boot auto-configures the connection from application.yml
}