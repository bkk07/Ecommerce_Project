package com.ecommerce.paymentservice.repository;

import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    Optional<Payment> findByOrderId(String orderId);
    
    /**
     * Find payments with specific statuses created before a given time.
     * Used by reconciliation job to find payments that may need manual confirmation.
     */
    List<Payment> findByStatusInAndCreatedAtBefore(List<PaymentStatus> statuses, LocalDateTime before);
}
