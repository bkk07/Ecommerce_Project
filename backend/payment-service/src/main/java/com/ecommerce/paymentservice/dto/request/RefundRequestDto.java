package com.ecommerce.paymentservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for processing a refund
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Refund request")
public class RefundRequestDto {

    @NotBlank(message = "Order ID is required")
    @Schema(description = "Order ID for the payment to refund", example = "ORD-2024-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderId;

    @DecimalMin(value = "1.00", message = "Minimum refund amount is ₹1.00")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 10 integer digits and 2 decimal places")
    @Schema(description = "Partial refund amount (optional, full refund if not provided)", example = "500.00")
    private BigDecimal amount;

    @Size(max = 255, message = "Reason must not exceed 255 characters")
    @Schema(description = "Reason for refund", example = "Customer requested cancellation")
    private String reason;

    @Schema(description = "Refund speed: 'normal' or 'optimum'", example = "normal", defaultValue = "normal")
    @Pattern(regexp = "^(normal|optimum)$", message = "Speed must be 'normal' or 'optimum'")
    @Builder.Default
    private String speed = "normal";
}
