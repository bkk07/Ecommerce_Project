package com.ecommerce.productservice.infrastructure.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true) // 🚀 Enables @PreAuthorize
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("Configuring SecurityFilterChain... I am in SecurityConfig");
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable) // Disable Spring Security CORS
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // No sessions

                // 🚀 Add our Custom Filter BEFORE the standard Spring Security filter
                .addFilterBefore(new GatewayHeaderFilter(), UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        // Allow public access to GET endpoints (as discussed)
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**", "/api/v1/categories/**").permitAll()
                        // All other requests must have valid headers (checked by filter)
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}