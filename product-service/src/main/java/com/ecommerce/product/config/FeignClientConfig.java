package com.ecommerce.product.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null) {
                        template.header("Authorization", authHeader);
                    }
                    
                    // Optional: Relay the custom headers added by the Gateway
                    String userId = request.getHeader("X-Auth-User-Id");
                    if (userId != null) {
                        template.header("X-Auth-User-Id", userId);
                    }
                    
                    String userRole = request.getHeader("X-Auth-User-Role");
                    if (userRole != null) {
                        template.header("X-Auth-User-Role", userRole);
                    }
                }
            }
        };
    }
}