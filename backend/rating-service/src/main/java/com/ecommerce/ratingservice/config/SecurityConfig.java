package com.ecommerce.ratingservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the Rating Service.
 * Implements stateless security. CORS is handled by API Gateway.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Slf4j
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless REST API
            .csrf(csrf -> csrf.disable())
            
            // Disable CORS - API Gateway handles CORS to prevent duplicate headers
            .cors(cors -> cors.disable())
            
            // Stateless session management
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - reading product ratings and summaries
                .requestMatchers(HttpMethod.GET, "/api/v1/ratings/product/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/ratings/{ratingId}").permitAll()
                
                // Actuator endpoints (health checks, metrics)
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()
                .requestMatchers("/actuator/**").authenticated()
                
                // OpenAPI/Swagger documentation
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/api-docs/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                
                // Preflight requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Admin endpoints - require admin role header from gateway
                .requestMatchers("/api/v1/admin/**").permitAll() // Gateway validates admin role
                
                // All other endpoints are accessible (gateway validates authentication)
                .anyRequest().permitAll()
            )
            
            // Custom headers support
            .headers(headers -> headers
                    .contentSecurityPolicy(csp -> csp
                            .policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                    .frameOptions(frame -> frame.deny())
            );
        
        return http.build();
    }
}
