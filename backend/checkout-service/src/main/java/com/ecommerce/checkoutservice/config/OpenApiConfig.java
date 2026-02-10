package com.ecommerce.checkoutservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for the Checkout Service.
 * Provides API documentation and testing interface.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI checkoutServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Checkout Service API")
                        .description("API for managing e-commerce checkout operations including " +
                                "cart checkout, direct purchase, and payment finalization.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("E-Commerce Team")
                                .email("support@ecommerce.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8086").description("Local Development"),
                        new Server().url("http://localhost:8080").description("API Gateway")))
                .addSecurityItem(new SecurityRequirement().addList("Gateway Auth Headers"))
                .schemaRequirement("Gateway Auth Headers", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-Auth-User-Id")
                        .description("User ID passed from API Gateway after JWT validation"));
    }
}
