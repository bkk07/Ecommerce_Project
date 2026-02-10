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
 * Response DTO for payment verification
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Payment verification response")
public class PaymentVerifyResponseDto {

    @Schema(description = "Human-readable response message", example = "Payment verified successfully")
    private String message;

    @Schema(description = "Whether the payment signature was verified", example = "true")
    private boolean verified;

    @Schema(description = "Internal Order ID", example = "ORD-2024-001")
    private String orderId;

    @Schema(description = "Razorpay Order ID", example = "order_M1234567890abc")
    private String razorpayOrderId;

    @Schema(description = "Razorpay Payment ID", example = "pay_M1234567890abc")
    private String razorpayPaymentId;

    @Schema(description = "Payment amount", example = "1500.00")
    private BigDecimal amount;

    @Schema(description = "Currency code", example = "INR")
    private String currency;

    @Schema(description = "Current payment status", example = "VERIFIED")
    private String status;

    @Schema(description = "Timestamp of verification")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime verifiedAt;

    @Schema(description = "Additional notes or instructions for frontend")
    private String notes;
}
