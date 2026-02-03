package com.ecommerce.ratingservice.config;

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
 * OpenAPI/Swagger documentation configuration for the Rating Service.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:rating-service}")
    private String applicationName;

    @Value("${server.port:8096}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Rating Service API")
                        .description("""
                            Production-grade Rating Service for E-commerce Platform.
                            
                            ## Features
                            - Create, update, and delete product ratings
                            - Verified purchase validation
                            - Rating summaries and statistics
                            - Admin moderation capabilities
                            - Pagination and filtering
                            
                            ## Authentication
                            This service expects the `X-Auth-User-Id` header to be set by the API Gateway
                            after JWT validation. Public endpoints (product ratings) don't require authentication.
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Ecommerce Team")
                                .email("team@ecommerce.com")
                                .url("https://github.com/ecommerce"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("http://api-gateway:8080")
                                .description("API Gateway")))
                .components(new Components()
                        .addSecuritySchemes("gateway-auth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Auth-User-Id")
                                .description("User ID injected by API Gateway after JWT validation")))
                .addSecurityItem(new SecurityRequirement().addList("gateway-auth"));
    }
}
