package com.ecommerce.paymentservice.mapper;

import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.entity.Payment;
import org.springframework.stereotype.Component;

/**
 * Mapper for Payment entity to DTO conversions
 */
@Component
public class PaymentMapper {

    /**
     * Convert Payment entity to PaymentResponse DTO
     */
    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus() != null ? payment.getStatus().name() : null)
                .methodType(payment.getMethodType() != null ? payment.getMethodType().name() : null)
                .userId(payment.getUserId())
                .vpa(payment.getVpa())
                .cardNetwork(payment.getCardNetwork())
                .cardLast4(payment.getCardLast4())
                .bank(payment.getBank())
                .wallet(payment.getWallet())
                .email(payment.getEmail())
                .contact(payment.getContact())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
