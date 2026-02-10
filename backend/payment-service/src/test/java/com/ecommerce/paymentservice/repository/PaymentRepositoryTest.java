package com.ecommerce.paymentservice.repository;

import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.enums.PaymentMethodType;
import com.ecommerce.paymentservice.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for PaymentRepository
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=LEGACY",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=true"
})
@DisplayName("PaymentRepository Tests")
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        
        testPayment = Payment.builder()
                .orderId("order123")
                .razorpayOrderId("order_razorpay123")
                .razorpayPaymentId("pay_123")
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
                .status(PaymentStatus.CREATED)
                .userId(456L)
                .methodType(PaymentMethodType.UPI)
                .vpa("user@upi")
                .email("test@example.com")
                .contact("+919876543210")
                .build();
    }

    @Nested
    @DisplayName("findByOrderId Tests")
    class FindByOrderIdTests {

        @Test
        @DisplayName("Should find payment by order ID")
        void findByOrderId_Exists_ReturnsPayment() {
            // Given
            paymentRepository.save(testPayment);

            // When
            Optional<Payment> result = paymentRepository.findByOrderId("order123");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getOrderId()).isEqualTo("order123");
            assertThat(result.get().getRazorpayOrderId()).isEqualTo("order_razorpay123");
        }

        @Test
        @DisplayName("Should return empty when order ID not found")
        void findByOrderId_NotExists_ReturnsEmpty() {
            // When
            Optional<Payment> result = paymentRepository.findByOrderId("nonexistent");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByRazorpayOrderId Tests")
    class FindByRazorpayOrderIdTests {

        @Test
        @DisplayName("Should find payment by Razorpay order ID")
        void findByRazorpayOrderId_Exists_ReturnsPayment() {
            // Given
            paymentRepository.save(testPayment);

            // When
            Optional<Payment> result = paymentRepository.findByRazorpayOrderId("order_razorpay123");

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getRazorpayOrderId()).isEqualTo("order_razorpay123");
            assertThat(result.get().getStatus()).isEqualTo(PaymentStatus.CREATED);
        }

        @Test
        @DisplayName("Should return empty when Razorpay order ID not found")
        void findByRazorpayOrderId_NotExists_ReturnsEmpty() {
            // When
            Optional<Payment> result = paymentRepository.findByRazorpayOrderId("order_nonexistent");

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Payment Persistence Tests")
    class PaymentPersistenceTests {

        @Test
        @DisplayName("Should save and retrieve payment with all fields")
        void save_AllFields_Persisted() {
            // Given
            testPayment.setStatus(PaymentStatus.PAID);
            testPayment.setCardNetwork("VISA");
            testPayment.setCardLast4("1234");

            // When
            Payment saved = paymentRepository.save(testPayment);
            Payment retrieved = paymentRepository.findById(saved.getId()).orElseThrow();

            // Then
            assertThat(retrieved.getOrderId()).isEqualTo("order123");
            assertThat(retrieved.getRazorpayOrderId()).isEqualTo("order_razorpay123");
            assertThat(retrieved.getRazorpayPaymentId()).isEqualTo("pay_123");
            assertThat(retrieved.getAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
            assertThat(retrieved.getCurrency()).isEqualTo("INR");
            assertThat(retrieved.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(retrieved.getMethodType()).isEqualTo(PaymentMethodType.UPI);
            assertThat(retrieved.getVpa()).isEqualTo("user@upi");
            assertThat(retrieved.getEmail()).isEqualTo("test@example.com");
            assertThat(retrieved.getContact()).isEqualTo("+919876543210");
        }

        @Test
        @DisplayName("Should update payment status")
        void update_Status_Persisted() {
            // Given
            Payment saved = paymentRepository.save(testPayment);
            
            // When
            saved.setStatus(PaymentStatus.VERIFIED);
            paymentRepository.save(saved);
            Payment retrieved = paymentRepository.findById(saved.getId()).orElseThrow();

            // Then
            assertThat(retrieved.getStatus()).isEqualTo(PaymentStatus.VERIFIED);
        }
    }
}
