package com.ecommerce.userservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration with JWT authentication.
 * The JWT filter validates tokens for protected endpoints.
 * Admin registration is locked behind ADMIN role.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // === PUBLIC ENDPOINTS (No Authentication Required) ===
                        // User registration and login
                        .requestMatchers("/auth/register").permitAll()
                        .requestMatchers("/auth/login").permitAll()
                        // Email/Phone verification (user needs userId from registration)
                        .requestMatchers("/auth/verify-email/**").permitAll()
                        .requestMatchers("/auth/verify-phone/**").permitAll()
                        // Forgot password flow
                        .requestMatchers("/auth/forgot-password/**").permitAll()
                        
                        // === ADMIN-ONLY ENDPOINTS ===
                        // Admin registration - requires existing ADMIN role
                        .requestMatchers("/auth/register/admin").hasRole("ADMIN")
                        // Admin analytics
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        
                        // === AUTHENTICATED USER ENDPOINTS ===
                        // User profile operations
                        .requestMatchers("/users/**").authenticated()
                        
                        // === DOCUMENTATION & MONITORING (Conditional Access) ===
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/api-docs/**").permitAll()
                        // Actuator - health is public, others require auth
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/info").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}