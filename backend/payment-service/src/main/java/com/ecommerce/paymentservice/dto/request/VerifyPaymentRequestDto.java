package com.ecommerce.paymentservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for verifying a payment signature
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payment verification request with Razorpay callback details")
public class VerifyPaymentRequestDto {

    @NotBlank(message = "Razorpay Order ID is required")
    @Pattern(regexp = "^order_[a-zA-Z0-9]{14,}$", message = "Invalid Razorpay Order ID format")
    @Schema(
            description = "Razorpay Order ID received during order creation",
            example = "order_M1234567890abc",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay Payment ID is required")
    @Pattern(regexp = "^pay_[a-zA-Z0-9]{14,}$", message = "Invalid Razorpay Payment ID format")
    @Schema(
            description = "Razorpay Payment ID received after successful payment",
            example = "pay_M1234567890abc",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay Signature is required")
    @Size(min = 64, max = 64, message = "Signature must be 64 characters (SHA256 hex)")
    @Pattern(regexp = "^[a-f0-9]{64}$", message = "Invalid signature format - must be 64 hex characters")
    @Schema(
            description = "HMAC SHA256 signature for verification",
            example = "9ef4dffbfd84f1318f6739a3ce19f9d85851857ae648f114332d8401e0949a3d",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String razorpaySignature;
}
