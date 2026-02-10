package com.ecommerce.paymentservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new payment
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payment creation request")
public class PaymentCreateRequestDto {

    @NotBlank(message = "Order ID is required")
    @Size(min = 1, max = 100, message = "Order ID must be between 1 and 100 characters")
    @Schema(description = "Unique order identifier", example = "ORD-2024-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderId;

    @NotBlank(message = "User ID is required")
    @Schema(description = "User identifier", example = "USR-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum payment amount is ₹1.00")
    @DecimalMax(value = "10000000.00", message = "Maximum payment amount is ₹1,00,00,000.00")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 10 integer digits and 2 decimal places")
    @Schema(description = "Payment amount in INR", example = "1500.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    @Size(max = 3, message = "Currency code must be 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
    @Schema(description = "Currency code (ISO 4217)", example = "INR", defaultValue = "INR")
    @Builder.Default
    private String currency = "INR";

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Schema(description = "Customer email for payment receipt", example = "customer@example.com")
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Invalid phone number format")
    @Schema(description = "Customer phone number", example = "+919876543210")
    private String contact;
}
