package com.ecommerce.wishlistservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // Add Gateway Header Filter before Spring Security authentication filter
                .addFilterBefore(new GatewayHeaderFilter(), UsernamePasswordAuthenticationFilter.class)
                
                .authorizeHttpRequests(auth -> auth
                        // Allow health check endpoint without authentication
                        .requestMatchers("/api/v1/wishlist/ping").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                );
        
        return http.build();
    }
}
