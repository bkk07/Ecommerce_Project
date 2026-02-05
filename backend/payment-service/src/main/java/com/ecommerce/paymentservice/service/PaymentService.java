package com.ecommerce.paymentservice.service;

import com.ecommerce.order.OrderCreatedEvent;
import com.ecommerce.payment.PaymentCreateRequest;
import com.ecommerce.payment.PaymentInitiatedEvent;
import com.ecommerce.payment.PaymentRefundedEvent;
import com.ecommerce.payment.PaymentSuccessEvent;
import com.ecommerce.payment.VerifyPaymentRequest;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.enums.PaymentMethodType;
import com.ecommerce.paymentservice.enums.PaymentStatus;
import com.ecommerce.paymentservice.exception.*;
import com.ecommerce.paymentservice.kafka.PaymentEventProducer;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import com.razorpay.Utils;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Payment Service handling all payment operations with Razorpay integration.
 * 
 * Features:
 * - Resilience4j Circuit Breaker, Retry, Bulkhead, and Rate Limiter
 * - Comprehensive metrics with Micrometer
 * - Event-driven architecture with Kafka
 * - Idempotent operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer eventProducer;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    // Metrics counters
    private Counter paymentCreatedCounter;
    private Counter paymentVerifiedCounter;
    private Counter paymentFailedCounter;
    private Counter refundProcessedCounter;

    @PostConstruct
    public void initMetrics() {
        paymentCreatedCounter = Counter.builder("payment.created")
                .description("Number of payments created")
                .tag("service", "payment")
                .register(meterRegistry);

        paymentVerifiedCounter = Counter.builder("payment.verified")
                .description("Number of payments verified")
                .tag("service", "payment")
                .register(meterRegistry);

        paymentFailedCounter = Counter.builder("payment.failed")
                .description("Number of failed payments")
                .tag("service", "payment")
                .register(meterRegistry);

        refundProcessedCounter = Counter.builder("payment.refund")
                .description("Number of refunds processed")
                .tag("service", "payment")
                .register(meterRegistry);
    }

    /**
     * Step 1: Handle OrderCreatedEvent (Async from Order Service)
     * Creates Razorpay Order and publishes PaymentInitiatedEvent
     */
    @Transactional
    @CircuitBreaker(name = "razorpayApi", fallbackMethod = "handleOrderCreatedFallback")
    @Retry(name = "razorpayRetry")
    @Bulkhead(name = "razorpayApi", fallbackMethod = "handleOrderCreatedFallback")
    @RateLimiter(name = "paymentApi")
    @Timed(value = "payment.create.time", description = "Time taken to create payment order")
    public PaymentInitiatedEvent handleOrderCreated(OrderCreatedEvent event) {
        log.info("Processing OrderCreatedEvent for order: {}", event.getOrderId());
        
        // Idempotency Check
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(event.getOrderId());
        if (existingPayment.isPresent()) {
            log.info("Payment already initiated for Order: {}", event.getOrderId());
            throw PaymentAlreadyExistsException.forOrderId(event.getOrderId());
        }

        try {
            // Convert to minor units (Paise)
            long amountInPaise = event.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            // Truncate receipt to 40 chars to satisfy Razorpay API limit
            String receipt = "order_rcptid_" + event.getOrderId();
            if (receipt.length() > 40) {
                receipt = receipt.substring(0, 40);
            }
            orderRequest.put("receipt", receipt);
            orderRequest.put("notes", new JSONObject().put("order_id", event.getOrderId()));

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            // Save initial payment record
            Payment payment = Payment.builder()
                    .razorpayOrderId(razorpayOrderId)
                    .orderId(event.getOrderId())
                    .amount(event.getTotalAmount())
                    .userId(Long.valueOf(event.getUserId())) 
                    .currency("INR")
                    .status(PaymentStatus.CREATED)
                    .build();
            paymentRepository.save(payment);

            log.info("Razorpay Order Created: {} for Order: {}", razorpayOrderId, event.getOrderId());
            paymentCreatedCounter.increment();

            // Create PaymentInitiatedEvent
            PaymentInitiatedEvent initiatedEvent = new PaymentInitiatedEvent(
                    event.getOrderId(),
                    razorpayOrderId,
                    event.getTotalAmount(),
                    event.getUserId()
            );
            
            return initiatedEvent;

        } catch (PaymentAlreadyExistsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create Razorpay order for Order: {}", event.getOrderId(), e);
            paymentFailedCounter.increment();
            throw PaymentGatewayException.orderCreationFailed(event.getOrderId(), e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     */
    public PaymentInitiatedEvent handleOrderCreatedFallback(OrderCreatedEvent event, Throwable t) {
        log.error("Circuit breaker fallback triggered for order: {}. Error: {}", event.getOrderId(), t.getMessage());
        throw new PaymentGatewayException("Payment gateway is currently unavailable. Please try again later.", t);
    }

    /**
     * Step 1 (Legacy/Direct): Create Order
     * Kept for backward compatibility if needed, but prefer handleOrderCreated
     */
    @Transactional
    public String createOrder(PaymentCreateRequest request) {
        throw new UnsupportedOperationException("Direct createOrder is deprecated. Use Event Driven flow.");
    }

    /**
     * Step 2: Verify Signature (Frontend Callback)
     * This is a quick check, but NOT the final confirmation.
     */
    @Transactional
    @Retry(name = "databaseRetry")
    @RateLimiter(name = "verificationApi")
    @Bulkhead(name = "paymentProcessing")
    @Timed(value = "payment.verify.time", description = "Time taken to verify payment")
    public Payment verifyPayment(VerifyPaymentRequest req) {
        log.info("Verifying payment for Razorpay Order ID: {}, Payment ID: {}", 
                req.getRazorpayOrderId(), req.getRazorpayPaymentId());
        
        Payment payment = paymentRepository.findByRazorpayOrderId(req.getRazorpayOrderId())
                .orElseThrow(() -> PaymentNotFoundException.forRazorpayOrderId(req.getRazorpayOrderId()));
        
        try {
            JSONObject options = new JSONObject();
            String generatedSignature = calculateSignature(req.getRazorpayOrderId(), req.getRazorpayPaymentId());
            options.put("razorpay_order_id", req.getRazorpayOrderId());
            options.put("razorpay_payment_id", req.getRazorpayPaymentId());
            options.put("razorpay_signature", generatedSignature);
            
            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);
            
            if (isValid) {
                payment.setRazorpayPaymentId(req.getRazorpayPaymentId());
                payment.setRazorpaySignature(req.getRazorpaySignature());
                payment.setStatus(PaymentStatus.VERIFIED);
                paymentRepository.save(payment);

                log.info("========================================");
                log.info("PAYMENT VERIFICATION SUCCESSFUL");
                log.info("Order ID: {}", payment.getOrderId());
                log.info("Razorpay Order ID: {}", req.getRazorpayOrderId());
                log.info("Razorpay Payment ID: {}", req.getRazorpayPaymentId());
                log.info("Amount: {} {}", payment.getAmount(), payment.getCurrency());
                log.info("User ID: {}", payment.getUserId());
                log.info("Status: {}", payment.getStatus());
                log.info("NOTE: PaymentSuccessEvent will be published by webhook/reconciliation job");
                log.info("========================================");
                
                paymentVerifiedCounter.increment();
                return payment;
            } else {
                log.error("Payment signature verification FAILED for Razorpay Order ID: {}", req.getRazorpayOrderId());
                paymentFailedCounter.increment();
                throw PaymentVerificationException.signatureInvalid(req.getRazorpayOrderId());
            }
        } catch (PaymentVerificationException | PaymentNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Payment verification failed for Razorpay Order ID: {}", req.getRazorpayOrderId(), e);
            paymentFailedCounter.increment();
            throw new PaymentVerificationException("Payment verification failed: " + e.getMessage(), e);
        }
    }

    /**
     * Step 3: Webhook Processing (Final Source of Truth)
     */
    @Transactional
    @Bulkhead(name = "webhookProcessing")
    @Timed(value = "payment.webhook.time", description = "Time taken to process webhook")
    public void processWebhook(String payload, String signature) {
        try {
            log.info("========================================");
            log.info("PROCESSING RAZORPAY WEBHOOK");
            log.info("========================================");
            
            // 1. Verify Webhook Signature (skip if secret not configured - dev mode)
            if (webhookSecret != null && !webhookSecret.isBlank()) {
                if (!Utils.verifyWebhookSignature(payload, signature, webhookSecret)) {
                    log.error("Invalid Webhook Signature");
                    throw WebhookProcessingException.invalidSignature();
                }
                log.info("Webhook signature verified successfully");
            } else {
                log.warn("Webhook secret not configured - skipping signature verification (DEV MODE)");
            }

            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText();
            log.info("Webhook event type: {}", event);

            if ("payment.captured".equals(event)) {
                processPaymentCaptured(root);
            } else {
                log.info("Ignoring webhook event type: {}", event);
            }

        } catch (WebhookProcessingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            throw WebhookProcessingException.processingFailed(e);
        }
    }

    /**
     * Process payment.captured webhook event
     */
    private void processPaymentCaptured(JsonNode root) {
        JsonNode entity = root.path("payload").path("payment").path("entity");
        String razorpayOrderId = entity.path("order_id").asText();
        String paymentId = entity.path("id").asText();
        
        log.info("Processing payment.captured - Razorpay Order ID: {}, Payment ID: {}", razorpayOrderId, paymentId);

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> {
                    log.error("Payment record not found for Razorpay Order ID: {}", razorpayOrderId);
                    return PaymentNotFoundException.forRazorpayOrderId(razorpayOrderId);
                });
        
        log.info("Found payment record - Order ID: {}, Current Status: {}", payment.getOrderId(), payment.getStatus());

        // Idempotency check
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("Payment already processed for order: {}", razorpayOrderId);
            return;
        }

        // Extract & Store Payment Method Details
        enrichPaymentDetails(payment, entity);

        // Mark PAID
        payment.setStatus(PaymentStatus.PAID);
        if (payment.getRazorpayPaymentId() == null || payment.getRazorpayPaymentId().isBlank()) {
            payment.setRazorpayPaymentId(paymentId);
        }
        paymentRepository.save(payment);
        log.info("Payment status updated to PAID for Order ID: {}", payment.getOrderId());

        // Notify Ecosystem (Saga)
        publishPaymentSuccess(payment, paymentId);

        log.info("========================================");
        log.info("WEBHOOK PROCESSING COMPLETE");
        log.info("Order ID: {}", payment.getOrderId());
        log.info("Razorpay Order ID: {}", razorpayOrderId);
        log.info("Payment ID: {}", paymentId);
        log.info("========================================");
    }

    /**
     * Publish payment success event with retry
     */
    @Retry(name = "kafkaRetry")
    private void publishPaymentSuccess(Payment payment, String paymentId) {
        PaymentSuccessEvent successEvent = new PaymentSuccessEvent(
                payment.getOrderId(),
                paymentId,
                payment.getMethodType() != null ? payment.getMethodType().name() : ""
        );
        eventProducer.publishPaymentSuccess(successEvent);
        log.info("PaymentSuccessEvent published successfully for order: {}", payment.getOrderId());
    }

    /**
     * Helper to extract detailed method info from Razorpay JSON
     */
    private void enrichPaymentDetails(Payment payment, JsonNode entity) {
        String method = entity.path("method").asText();
        payment.setEmail(entity.path("email").asText());
        payment.setContact(entity.path("contact").asText());

        switch (method) {
            case "card" -> {
                payment.setMethodType(PaymentMethodType.CARD);
                JsonNode card = entity.path("card");
                payment.setCardNetwork(card.path("network").asText());
                payment.setCardLast4(card.path("last4").asText());
            }
            case "upi" -> {
                payment.setMethodType(PaymentMethodType.UPI);
                payment.setVpa(entity.path("vpa").asText());
            }
            case "netbanking" -> {
                payment.setMethodType(PaymentMethodType.NETBANKING);
                payment.setBank(entity.path("bank").asText());
            }
            case "wallet" -> {
                payment.setMethodType(PaymentMethodType.WALLET);
                payment.setWallet(entity.path("wallet").asText());
            }
            default -> payment.setMethodType(PaymentMethodType.UNKNOWN);
        }
    }

    private String calculateSignature(String orderId, String paymentId) {
        try {
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(keySecret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error calculating signature", e);
            throw new PaymentVerificationException("Error calculating signature", e);
        }
    }

    /**
     * Process refund for an order
     */
    @Transactional
    @CircuitBreaker(name = "razorpayApi", fallbackMethod = "processRefundFallback")
    @Retry(name = "razorpayRetry")
    @Bulkhead(name = "razorpayApi")
    @RateLimiter(name = "refundApi")
    @Timed(value = "payment.refund.time", description = "Time taken to process refund")
    public void processRefund(String orderId) {
        log.info("Processing refund for order: {}", orderId);
        
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> PaymentNotFoundException.forOrderId(orderId));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            log.info("Payment already refunded for order: {}", orderId);
            return;
        }

        if (payment.getStatus() == PaymentStatus.VERIFIED || payment.getStatus() == PaymentStatus.PAID) {
            try {
                JSONObject refundRequest = new JSONObject();
                refundRequest.put("payment_id", payment.getRazorpayPaymentId());
                refundRequest.put("amount", payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue());
                refundRequest.put("speed", "normal");

                Refund refund = razorpayClient.payments.refund(refundRequest);
                
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);

                PaymentRefundedEvent event = new PaymentRefundedEvent(orderId, payment.getRazorpayPaymentId(), refund.get("id"));
                eventProducer.publishPaymentRefunded(event);
                
                refundProcessedCounter.increment();
                log.info("Refund processed successfully for order: {}", orderId);

            } catch (Exception e) {
                log.error("Error processing refund for order: {}", orderId, e);
                paymentFailedCounter.increment();
                throw PaymentGatewayException.refundFailed(orderId, e);
            }
        } else {
            log.info("Payment status {} does not require refund for order: {}", payment.getStatus(), orderId);
            PaymentRefundedEvent event = new PaymentRefundedEvent(orderId, null, null);
            eventProducer.publishPaymentRefunded(event);
        }
    }

    /**
     * Fallback for refund when circuit breaker is open
     */
    public void processRefundFallback(String orderId, Throwable t) {
        log.error("Circuit breaker fallback triggered for refund. Order: {}. Error: {}", orderId, t.getMessage());
        throw new PaymentGatewayException("Refund service is currently unavailable. Please try again later.", t);
    }

    /**
     * Get payment by order ID
     */
    @Retry(name = "databaseRetry")
    @Timed(value = "payment.get.time", description = "Time taken to get payment")
    public Payment getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> PaymentNotFoundException.forOrderId(orderId));
    }

    /**
     * Get payment status by Razorpay order ID
     */
    @Retry(name = "databaseRetry")
    public String getPaymentStatus(String razorpayOrderId) {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> PaymentNotFoundException.forRazorpayOrderId(razorpayOrderId));
        return payment.getStatus().name();
    }
}
