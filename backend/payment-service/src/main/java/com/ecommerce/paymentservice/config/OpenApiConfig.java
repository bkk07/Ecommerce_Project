package com.ecommerce.paymentservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * OpenAPI/Swagger configuration for Payment Service API documentation
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8088}")
    private String serverPort;

    @Value("${spring.application.name:payment-service}")
    private String applicationName;

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Service API")
                        .description("""
                                # Payment Service API for E-commerce Platform
                                
                                A robust, resilient payment service integrated with Razorpay payment gateway.
                                
                                ## 🚀 Features
                                
                                | Feature | Description |
                                |---------|-------------|
                                | **Razorpay Integration** | Full payment lifecycle management |
                                | **Circuit Breaker** | Resilience4j circuit breaker for fault tolerance |
                                | **Rate Limiting** | API rate limiting to prevent abuse |
                                | **Bulkhead Isolation** | Thread isolation for critical operations |
                                | **Retry Mechanism** | Automatic retry with exponential backoff |
                                | **Distributed Tracing** | OpenTelemetry integration |
                                | **Webhook Support** | Real-time payment status updates |
                                
                                ## 📋 Payment Flow
                                
                                ```
                                1. Order Service → POST /api/payments/create → Payment Initiated
                                2. Frontend receives Razorpay order ID and key
                                3. User completes payment on Razorpay checkout
                                4. Frontend → POST /api/payments/verify → Signature Verified
                                5. Razorpay → POST /api/webhooks/razorpay → Payment Confirmed
                                6. PaymentSuccessEvent published → Saga continues
                                ```
                                
                                ## 🔒 Security
                                - All endpoints require JWT authentication (via API Gateway)
                                - Webhook signature verification
                                - PCI-DSS compliant - no card data stored
                                
                                ## 📊 Monitoring
                                - Health check: `/actuator/health`
                                - Metrics: `/actuator/prometheus`
                                - Circuit breaker status: `/actuator/circuitbreakers`
                                """)
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("E-commerce Platform Team")
                                .email("support@ecommerce.com")
                                .url("https://ecommerce.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Razorpay API Documentation")
                        .url("https://razorpay.com/docs/api/"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("http://payment-service:8088")
                                .description("Docker/Kubernetes Internal Server"),
                        new Server()
                                .url("http://localhost:8080/payment-service")
                                .description("Via API Gateway")
                ))
                .tags(List.of(
                        new Tag()
                                .name("Payment Operations")
                                .description("Core payment operations - create orders, verify payments, check status"),
                        new Tag()
                                .name("Webhooks")
                                .description("Payment gateway webhook endpoints for async payment confirmations"),
                        new Tag()
                                .name("Refunds")
                                .description("Refund processing operations"),
                        new Tag()
                                .name("Health")
                                .description("Service health and readiness endpoints")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token obtained from authentication service"))
                        .addSchemas("PaymentStatus", new Schema<String>()
                                .type("string")
                                .description("Payment status enumeration")
                                ._enum(List.of("CREATED", "PENDING", "VERIFIED", "PAID", "FAILED", "REFUNDED", "EXPIRED")))
                        .addSchemas("PaymentMethodType", new Schema<String>()
                                .type("string")
                                .description("Payment method type")
                                ._enum(List.of("CARD", "UPI", "NETBANKING", "WALLET", "UNKNOWN"))))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
