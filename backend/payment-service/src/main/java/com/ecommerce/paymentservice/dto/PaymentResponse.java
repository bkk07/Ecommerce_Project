package com.ecommerce.paymentservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Payment entity
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payment response containing payment details")
public class PaymentResponse {

    @Schema(description = "Payment ID", example = "1")
    private Long id;

    @Schema(description = "Internal Order ID", example = "order123")
    private String orderId;

    @Schema(description = "Razorpay Order ID", example = "order_razorpay123")
    private String razorpayOrderId;

    @Schema(description = "Razorpay Payment ID", example = "pay_123")
    private String razorpayPaymentId;

    @Schema(description = "Payment amount", example = "1500.00")
    private BigDecimal amount;

    @Schema(description = "Currency code", example = "INR")
    private String currency;

    @Schema(description = "Payment status", example = "PAID")
    private String status;

    @Schema(description = "Payment method type", example = "UPI")
    private String methodType;

    @Schema(description = "User ID", example = "456")
    private Long userId;

    @Schema(description = "UPI VPA (if UPI payment)", example = "user@upi")
    private String vpa;

    @Schema(description = "Card network (if card payment)", example = "VISA")
    private String cardNetwork;

    @Schema(description = "Card last 4 digits (if card payment)", example = "1234")
    private String cardLast4;

    @Schema(description = "Bank name (if netbanking)", example = "HDFC")
    private String bank;

    @Schema(description = "Wallet name (if wallet payment)", example = "PhonePe")
    private String wallet;

    @Schema(description = "Customer email", example = "user@example.com")
    private String email;

    @Schema(description = "Customer contact", example = "+919876543210")
    private String contact;

    @Schema(description = "Payment creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Payment last update timestamp")
    private LocalDateTime updatedAt;
}
