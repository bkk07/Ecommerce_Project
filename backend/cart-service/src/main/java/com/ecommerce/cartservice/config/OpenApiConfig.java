package com.ecommerce.cartservice.config;

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
 * OpenAPI/Swagger configuration for Cart Service API documentation.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:cart-service}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cart Service API")
                        .version("1.0.0")
                        .description("Shopping cart management service for E-commerce platform. " +
                                "Provides endpoints for managing user shopping carts including " +
                                "adding, removing, and updating cart items.")
                        .contact(new Contact()
                                .name("E-commerce Team")
                                .email("support@ecommerce.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("API Gateway"),
                        new Server()
                                .url("http://localhost:8082")
                                .description("Direct Cart Service")))
                .components(new Components()
                        .addSecuritySchemes("Gateway-Auth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-Auth-User-Id")
                                        .description("User ID passed by API Gateway after JWT validation")))
                .addSecurityItem(new SecurityRequirement().addList("Gateway-Auth"));
    }
}
