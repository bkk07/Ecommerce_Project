package com.ecommerce.userservice.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for APIs
                .cors(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Allow Auth endpoints (Gateway forwards public login requests here)
                        .requestMatchers("/auth/**").permitAll()
                        // Allow User Profile endpoints (Gateway has already checked the token)
                        // We trust the X-Auth-User-Id header injected by the Gateway
                        .requestMatchers("/users/**").permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }

}