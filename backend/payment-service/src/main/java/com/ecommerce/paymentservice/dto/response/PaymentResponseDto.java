package com.ecommerce.paymentservice.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for Payment details
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Payment details response")
public class PaymentResponseDto {

    @Schema(description = "Payment ID", example = "1")
    private Long id;

    @Schema(description = "Internal Order ID", example = "ORD-2024-001")
    private String orderId;

    @Schema(description = "Razorpay Order ID", example = "order_M1234567890abc")
    private String razorpayOrderId;

    @Schema(description = "Razorpay Payment ID (available after payment)", example = "pay_M1234567890abc")
    private String razorpayPaymentId;

    @Schema(description = "Payment amount", example = "1500.00")
    private BigDecimal amount;

    @Schema(description = "Currency code", example = "INR")
    private String currency;

    @Schema(description = "Payment status", example = "PAID", 
            allowableValues = {"CREATED", "PENDING", "VERIFIED", "PAID", "FAILED", "REFUNDED", "EXPIRED"})
    private String status;

    @Schema(description = "Payment method type", example = "UPI",
            allowableValues = {"CARD", "UPI", "NETBANKING", "WALLET", "UNKNOWN"})
    private String methodType;

    @Schema(description = "User ID", example = "456")
    private Long userId;

    @Schema(description = "UPI VPA (if UPI payment)", example = "user@okicici")
    private String vpa;

    @Schema(description = "Card network (if card payment)", example = "Visa")
    private String cardNetwork;

    @Schema(description = "Card last 4 digits (if card payment)", example = "4242")
    private String cardLast4;

    @Schema(description = "Bank name (if netbanking)", example = "HDFC")
    private String bank;

    @Schema(description = "Wallet name (if wallet payment)", example = "PhonePe")
    private String wallet;

    @Schema(description = "Customer email", example = "customer@example.com")
    private String email;

    @Schema(description = "Customer contact number", example = "+919876543210")
    private String contact;

    @Schema(description = "Payment creation timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Payment last update timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @Schema(description = "Razorpay Key ID for frontend integration", example = "rzp_test_xxx")
    private String razorpayKeyId;

    @Schema(description = "Human-readable status message", example = "Payment completed successfully")
    private String statusMessage;
}
