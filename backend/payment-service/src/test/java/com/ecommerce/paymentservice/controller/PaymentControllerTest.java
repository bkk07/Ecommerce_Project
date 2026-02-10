package com.ecommerce.paymentservice.controller;

import com.ecommerce.payment.VerifyPaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.enums.PaymentStatus;
import com.ecommerce.paymentservice.exception.PaymentNotFoundException;
import com.ecommerce.paymentservice.exception.PaymentVerificationException;
import com.ecommerce.paymentservice.mapper.PaymentMapper;
import com.ecommerce.paymentservice.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for PaymentController
 */
@WebMvcTest(PaymentController.class)
@DisplayName("PaymentController Unit Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private PaymentMapper paymentMapper;

    private Payment testPayment;
    private PaymentResponse testResponse;

    @BeforeEach
    void setUp() {
        testPayment = Payment.builder()
                .id(1L)
                .orderId("order123")
                .razorpayOrderId("order_razorpay123")
                .razorpayPaymentId("pay_123")
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
                .status(PaymentStatus.VERIFIED)
                .userId(456L)
                .build();

        testResponse = PaymentResponse.builder()
                .id(1L)
                .orderId("order123")
                .razorpayOrderId("order_razorpay123")
                .razorpayPaymentId("pay_123")
                .amount(new BigDecimal("1500.00"))
                .currency("INR")
                .status(PaymentStatus.VERIFIED.name())
                .build();
    }

    @Nested
    @DisplayName("POST /api/payments/verify Tests")
    class VerifyPaymentTests {

        @Test
        @DisplayName("Should verify payment successfully")
        void verifyPayment_Success() throws Exception {
            // Given
            VerifyPaymentRequest request = new VerifyPaymentRequest(
                    "order_razorpay123",
                    "pay_123",
                    "signature123");

            when(paymentService.verifyPayment(any(VerifyPaymentRequest.class)))
                    .thenReturn(testPayment);
            when(paymentMapper.toResponse(testPayment)).thenReturn(testResponse);

            // When & Then
            mockMvc.perform(post("/api/payments/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value("order123"))
                    .andExpect(jsonPath("$.razorpayOrderId").value("order_razorpay123"));

            verify(paymentService).verifyPayment(any(VerifyPaymentRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when request has null values")
        void verifyPayment_InvalidRequest_Returns400() throws Exception {
            // Given - request without required fields will cause service error
            VerifyPaymentRequest request = new VerifyPaymentRequest(null, null, null);

            // Service throws exception when receiving null values
            when(paymentService.verifyPayment(any(VerifyPaymentRequest.class)))
                    .thenThrow(new IllegalArgumentException("Invalid payment verification request"));

            // When & Then - GlobalExceptionHandler handles IllegalArgumentException as 400
            mockMvc.perform(post("/api/payments/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when payment not found")
        void verifyPayment_NotFound_Returns404() throws Exception {
            // Given
            VerifyPaymentRequest request = new VerifyPaymentRequest(
                    "order_nonexistent",
                    "pay_123",
                    "signature123");

            when(paymentService.verifyPayment(any(VerifyPaymentRequest.class)))
                    .thenThrow(PaymentNotFoundException.forRazorpayOrderId("order_nonexistent"));

            // When & Then
            mockMvc.perform(post("/api/payments/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Payment Not Found"));
        }

        @Test
        @DisplayName("Should return 400 when signature verification fails")
        void verifyPayment_SignatureFailed_Returns400() throws Exception {
            // Given
            VerifyPaymentRequest request = new VerifyPaymentRequest(
                    "order_razorpay123",
                    "pay_123",
                    "invalid_signature");

            when(paymentService.verifyPayment(any(VerifyPaymentRequest.class)))
                    .thenThrow(PaymentVerificationException.signatureInvalid("order_razorpay123"));

            // When & Then
            mockMvc.perform(post("/api/payments/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Payment Verification Failed"));
        }
    }

    @Nested
    @DisplayName("GET /api/payments/order/{orderId} Tests")
    class GetPaymentByOrderIdTests {

        @Test
        @DisplayName("Should get payment by order ID successfully")
        void getPaymentByOrderId_Success() throws Exception {
            // Given
            when(paymentService.getPaymentByOrderId("order123")).thenReturn(testPayment);
            when(paymentMapper.toResponse(testPayment)).thenReturn(testResponse);

            // When & Then
            mockMvc.perform(get("/api/payments/order/order123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value("order123"))
                    .andExpect(jsonPath("$.razorpayOrderId").value("order_razorpay123"));
        }

        @Test
        @DisplayName("Should return 404 when order not found")
        void getPaymentByOrderId_NotFound_Returns404() throws Exception {
            // Given
            when(paymentService.getPaymentByOrderId("nonexistent"))
                    .thenThrow(PaymentNotFoundException.forOrderId("nonexistent"));

            // When & Then
            mockMvc.perform(get("/api/payments/order/nonexistent"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Payment Not Found"));
        }
    }

    @Nested
    @DisplayName("GET /api/payments/status/{razorpayOrderId} Tests")
    class GetPaymentStatusTests {

        @Test
        @DisplayName("Should get payment status successfully")
        void getPaymentStatus_Success() throws Exception {
            // Given
            when(paymentService.getPaymentStatus("order_razorpay123")).thenReturn("VERIFIED");

            // When & Then
            mockMvc.perform(get("/api/payments/status/order_razorpay123"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("VERIFIED"));
        }

        @Test
        @DisplayName("Should return 404 when razorpay order not found")
        void getPaymentStatus_NotFound_Returns404() throws Exception {
            // Given
            when(paymentService.getPaymentStatus("order_nonexistent"))
                    .thenThrow(PaymentNotFoundException.forRazorpayOrderId("order_nonexistent"));

            // When & Then
            mockMvc.perform(get("/api/payments/status/order_nonexistent"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/payments/refund/{orderId} Tests")
    class RefundPaymentTests {

        @Test
        @DisplayName("Should process refund successfully")
        void refundPayment_Success() throws Exception {
            // Given
            doNothing().when(paymentService).processRefund("order123");

            // When & Then
            mockMvc.perform(post("/api/payments/refund/order123"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Refund processed successfully"));
        }

        @Test
        @DisplayName("Should return 404 when order not found for refund")
        void refundPayment_NotFound_Returns404() throws Exception {
            // Given
            doThrow(PaymentNotFoundException.forOrderId("nonexistent"))
                    .when(paymentService).processRefund("nonexistent");

            // When & Then
            mockMvc.perform(post("/api/payments/refund/nonexistent"))
                    .andExpect(status().isNotFound());
        }
    }
}
