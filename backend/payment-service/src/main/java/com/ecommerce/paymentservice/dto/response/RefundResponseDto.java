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
 * Response DTO for refund operations
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Refund response")
public class RefundResponseDto {

    @Schema(description = "Refund ID from Razorpay", example = "rfnd_M1234567890abc")
    private String refundId;

    @Schema(description = "Associated Payment ID", example = "pay_M1234567890abc")
    private String paymentId;

    @Schema(description = "Internal Order ID", example = "ORD-2024-001")
    private String orderId;

    @Schema(description = "Refund amount", example = "500.00")
    private BigDecimal amount;

    @Schema(description = "Currency code", example = "INR")
    private String currency;

    @Schema(description = "Refund status", example = "processed",
            allowableValues = {"pending", "processed", "failed"})
    private String status;

    @Schema(description = "Refund speed", example = "normal")
    private String speed;

    @Schema(description = "Reason for refund", example = "Customer requested cancellation")
    private String reason;

    @Schema(description = "Human-readable message", example = "Refund processed successfully")
    private String message;

    @Schema(description = "Timestamp of refund creation")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "Estimated settlement time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime estimatedSettlementAt;
}
