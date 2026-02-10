package com.ecommerce.inventoryservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for Inventory Service API documentation.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8084}")
    private String serverPort;

    @Bean
    public OpenAPI inventoryServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory Service API")
                        .description("""
                                RESTful API for managing product inventory in the E-commerce platform.
                                
                                ## Features
                                - Stock initialization and updates
                                - Stock reservation for orders (prevents overselling)
                                - Stock release on order cancellation
                                - Idempotent operations for reliable Saga orchestration
                                - Outbox pattern for reliable event publishing
                                
                                ## Stock Operations
                                - **Reserve**: Temporarily holds stock for an order during checkout
                                - **Release**: Returns reserved stock if order fails or is cancelled
                                - **Update**: Sets absolute stock quantity (Admin only)
                                
                                ## Authentication
                                This service expects authentication headers from the API Gateway:
                                - `X-Auth-User-Id`: The authenticated user's ID
                                - `X-Auth-User-Role`: The user's role (USER, ADMIN)
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("E-commerce Team")
                                .email("support@ecommerce.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("http://localhost:8080/api/inventory")
                                .description("Via API Gateway")))
                .components(new Components()
                        .addSecuritySchemes("gateway-auth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Auth-User-Id")
                                .description("User ID header from API Gateway")))
                .addSecurityItem(new SecurityRequirement().addList("gateway-auth"));
    }
}
