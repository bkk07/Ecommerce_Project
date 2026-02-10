package com.ecommerce.orderservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for Order Service
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8089}")
    private String serverPort;

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .description("""
                                Order Service API for the E-commerce Platform.
                                
                                This service handles:
                                - Order creation and management
                                - Order status tracking
                                - Payment integration via Razorpay
                                - Order cancellation with Saga pattern
                                - Admin order management and statistics
                                
                                ## Authentication
                                All endpoints require authentication via API Gateway.
                                User ID and Role are passed via headers: `X-Auth-User-Id`, `X-Auth-User-Role`
                                
                                ## Order Status Flow
                                ```
                                PENDING -> PAYMENT_READY -> PLACED -> PACKED -> SHIPPED -> DELIVERED
                                                                   \\-> CANCEL_REQUESTED -> CANCELLED
                                ```
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("E-commerce Team")
                                .email("support@ecommerce.com")
                                .url("https://ecommerce.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("API Gateway")))
                .tags(List.of(
                        new Tag()
                                .name("Orders")
                                .description("Customer order operations"),
                        new Tag()
                                .name("Admin Orders")
                                .description("Admin order management operations")))
                .components(new Components()
                        .addSecuritySchemes("UserIdHeader", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Auth-User-Id")
                                .description("User ID passed from API Gateway"))
                        .addSecuritySchemes("UserRoleHeader", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Auth-User-Role")
                                .description("User Role passed from API Gateway")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("UserIdHeader")
                        .addList("UserRoleHeader"));
    }
}
