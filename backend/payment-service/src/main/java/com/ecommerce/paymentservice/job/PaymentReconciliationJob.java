package com.ecommerce.paymentservice.job;

import com.ecommerce.payment.PaymentSuccessEvent;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.enums.PaymentStatus;
import com.ecommerce.paymentservice.kafka.PaymentEventProducer;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reconciliation job that ensures payments verified by frontend
 * but pending webhook confirmation are properly confirmed.
 * 
 * This handles cases where:
 * 1. User closes browser before frontend verification call completes
 * 2. Webhook delivery fails or is delayed
 * 3. Network issues during payment flow
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationJob {

    private final PaymentRepository paymentRepository;
    private final RazorpayClient razorpayClient;
    private final PaymentEventProducer eventProducer;

    /**
     * Runs every 2 minutes to check for payments that are in CREATED or VERIFIED status
     * and confirms them with Razorpay if payment was actually captured.
     */
    @Scheduled(fixedDelay = 120000) // 2 minutes
    @Transactional
    public void reconcilePendingPayments() {
        log.debug("Running payment reconciliation job...");
        
        // Find payments that are CREATED or VERIFIED (not yet confirmed as PAID)
        // and were created more than 1 minute ago (give webhook time to arrive)
        List<Payment> pendingPayments = paymentRepository.findByStatusInAndCreatedAtBefore(
                List.of(PaymentStatus.CREATED, PaymentStatus.VERIFIED),
                LocalDateTime.now().minusMinutes(1)
        );

        if (pendingPayments.isEmpty()) {
            log.debug("No pending payments to reconcile");
            return;
        }

        log.info("Found {} pending payments to reconcile", pendingPayments.size());

        for (Payment payment : pendingPayments) {
            try {
                reconcilePayment(payment);
            } catch (Exception e) {
                log.error("Failed to reconcile payment for order: {}", payment.getOrderId(), e);
            }
        }
    }

    private void reconcilePayment(Payment payment) {
        try {
            // Re-fetch to avoid race conditions (payment might have been updated by webhook)
            Payment currentPayment = paymentRepository.findById(payment.getId()).orElse(null);
            if (currentPayment == null) {
                log.warn("Payment no longer exists: {}", payment.getId());
                return;
            }
            
            // Skip if already processed
            if (currentPayment.getStatus() == PaymentStatus.PAID) {
                log.info("Payment already PAID for Order: {}, skipping reconciliation", currentPayment.getOrderId());
                return;
            }
            
            log.info("Reconciling payment for Order: {}, Razorpay Order ID: {}", 
                    currentPayment.getOrderId(), currentPayment.getRazorpayOrderId());

            // Fetch order status from Razorpay
            com.razorpay.Order razorpayOrder = razorpayClient.orders.fetch(currentPayment.getRazorpayOrderId());
            String status = razorpayOrder.get("status");
            
            log.info("Razorpay order status: {} for Order: {}", status, currentPayment.getOrderId());

            if ("paid".equals(status)) {
                // Double-check status again to avoid race condition
                Payment latestPayment = paymentRepository.findById(currentPayment.getId()).orElse(null);
                if (latestPayment != null && latestPayment.getStatus() == PaymentStatus.PAID) {
                    log.info("Payment was processed by webhook while we were checking, skipping");
                    return;
                }
                
                // Payment was captured - update our records and publish event
                String razorpayPaymentId = null;
                
                // Try to get payment ID from Razorpay
                try {
                    // Fetch payments for this order
                    JSONObject params = new JSONObject();
                    List<com.razorpay.Payment> payments = razorpayClient.orders.fetchPayments(currentPayment.getRazorpayOrderId());
                    if (!payments.isEmpty()) {
                        razorpayPaymentId = payments.get(0).get("id");
                    }
                } catch (Exception e) {
                    log.warn("Could not fetch payment ID for order: {}", currentPayment.getOrderId());
                    razorpayPaymentId = currentPayment.getRazorpayPaymentId(); // Use existing if available
                }

                if (razorpayPaymentId == null) {
                    razorpayPaymentId = "reconciled_" + currentPayment.getRazorpayOrderId();
                }

                // Update payment status
                currentPayment.setStatus(PaymentStatus.PAID);
                if (currentPayment.getRazorpayPaymentId() == null || currentPayment.getRazorpayPaymentId().isBlank()) {
                    currentPayment.setRazorpayPaymentId(razorpayPaymentId);
                }
                paymentRepository.save(currentPayment);

                // Publish success event
                PaymentSuccessEvent successEvent = new PaymentSuccessEvent(
                        currentPayment.getOrderId(),
                        razorpayPaymentId,
                        currentPayment.getMethodType() != null ? currentPayment.getMethodType().name() : ""
                );
                eventProducer.publishPaymentSuccess(successEvent);

                log.info("========================================");
                log.info("PAYMENT RECONCILED SUCCESSFULLY");
                log.info("Order ID: {}", currentPayment.getOrderId());
                log.info("Razorpay Order ID: {}", currentPayment.getRazorpayOrderId());
                log.info("Payment ID: {}", razorpayPaymentId);
                log.info("PaymentSuccessEvent published");
                log.info("========================================");

            } else if ("attempted".equals(status)) {
                // Payment attempted but not captured yet - check if it failed
                log.info("Payment still pending for Order: {}", currentPayment.getOrderId());
            } else if ("created".equals(status)) {
                // Order created but payment not attempted yet
                log.debug("Payment not yet attempted for Order: {}", currentPayment.getOrderId());
            }

        } catch (Exception e) {
            log.error("Error reconciling payment for Order: {}", payment.getOrderId(), e);
        }
    }
}
