package com.ecommerce.paymentservice.integration;

import com.ecommerce.payment.VerifyPaymentRequest;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.enums.PaymentStatus;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Payment Controller with Testcontainers
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Payment Controller Integration Tests")
class PaymentControllerIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("payment_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("razorpay.key.id", () -> "rzp_test_key");
        registry.add("razorpay.key.secret", () -> "rzp_test_secret");
        registry.add("razorpay.webhook.secret", () -> "webhook_secret");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Nested
    @DisplayName("GET /api/payments/order/{orderId} Integration Tests")
    class GetPaymentByOrderIdIntegrationTests {

        @Test
        @DisplayName("Should return payment when found")
        void getPaymentByOrderId_Found_ReturnsPayment() throws Exception {
            // Given
            Payment payment = createAndSavePayment("order123", "order_razorpay123");

            // When & Then
            mockMvc.perform(get("/api/payments/order/order123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value("order123"))
                    .andExpect(jsonPath("$.razorpayOrderId").value("order_razorpay123"))
                    .andExpect(jsonPath("$.amount").value(1500.00))
                    .andExpect(jsonPath("$.currency").value("INR"));
        }

        @Test
        @DisplayName("Should return 404 when payment not found")
        void getPaymentByOrderId_NotFound_Returns404() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/payments/order/nonexistent"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/payments/status/{razorpayOrderId} Integration Tests")
    class GetPaymentStatusIntegrationTests {

        @Test
        @DisplayName("Should return payment status when found")
        void getPaymentStatus_Found_ReturnsStatus() throws Exception {
            // Given
            Payment payment = createAndSavePayment("order123", "order_razorpay123");
            payment.setStatus(PaymentStatus.PAID);
            paymentRepository.save(payment);

            // When & Then
            mockMvc.perform(get("/api/payments/status/order_razorpay123"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("PAID"));
        }

        @Test
        @DisplayName("Should return 404 when razorpay order not found")
        void getPaymentStatus_NotFound_Returns404() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/payments/status/order_nonexistent"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/payments/refund/{orderId} Integration Tests")
    class RefundPaymentIntegrationTests {

        @Test
        @DisplayName("Should return 404 when payment not found for refund")
        void refundPayment_NotFound_Returns404() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/payments/refund/nonexistent"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not Found"));
        }
    }

    @Nested
    @DisplayName("POST /api/payments/verify Integration Tests")
    class VerifyPaymentIntegrationTests {

        @Test
        @DisplayName("Should return 404 when payment not found for verification")
        void verifyPayment_PaymentNotFound_Returns404() throws Exception {
            // Given
            VerifyPaymentRequest request = new VerifyPaymentRequest(
                    "order_nonexistent",
                    "pay_123",
                    "signature123");

            // When & Then
            mockMvc.perform(post("/api/payments/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should validate request body")
        void verifyPayment_InvalidRequest_Returns400() throws Exception {
            // Given - empty request
            String invalidRequest = "{}";

            // When & Then
            mockMvc.perform(post("/api/payments/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Database Integration Tests")
    class DatabaseIntegrationTests {

        @Test
        @DisplayName("Should persist and retrieve payment correctly")
        void payment_Persistence_Works() {
            // Given
            Payment payment = Payment.builder()
                    .orderId("order_db_test")
                    .razorpayOrderId("order_razorpay_db")
                    .amount(new BigDecimal("2500.00"))
                    .currency("INR")
                    .status(PaymentStatus.CREATED)
                    .userId(123L)
                    .build();

            // When
            Payment saved = paymentRepository.save(payment);
            Payment found = paymentRepository.findByOrderId("order_db_test").orElseThrow();

            // Then
            assertThat(found.getId()).isEqualTo(saved.getId());
            assertThat(found.getAmount()).isEqualByComparingTo(new BigDecimal("2500.00"));
            assertThat(found.getStatus()).isEqualTo(PaymentStatus.CREATED);
        }

        @Test
        @DisplayName("Should update payment status correctly")
        void payment_StatusUpdate_Works() {
            // Given
            Payment payment = createAndSavePayment("order_update", "order_razorpay_update");

            // When
            payment.setStatus(PaymentStatus.VERIFIED);
            payment.setRazorpayPaymentId("pay_new_123");
            paymentRepository.save(payment);

            // Then
            Payment updated = paymentRepository.findByOrderId("order_update").orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(PaymentStatus.VERIFIED);
            assertThat(updated.getRazorpayPaymentId()).isEqualTo("pay_new_123");
        }
    }

    private Payment createAndSavePayment(String orderId, String razorpayOrderId) {
        Payment payment = Payment.builder()
                .orderId(orderId)
                .razorpayOrderId(razorpayOrderId)
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
                .status(PaymentStatus.CREATED)
                .userId(456L)
                .build();
        return paymentRepository.save(payment);
    }
}
