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
import com.ecommerce.paymentservice.kafka.PaymentEventProducer;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import com.razorpay.Utils;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    /**
     * Step 1: Handle OrderCreatedEvent (Async from Order Service)
     * Creates Razorpay Order and publishes PaymentInitiatedEvent
     */
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        // Idempotency Check
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(event.getOrderId());
        if (existingPayment.isPresent()) {
            log.info("Payment already initiated for Order: {}", event.getOrderId());
            return;
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

            // Publish PaymentInitiatedEvent
            PaymentInitiatedEvent initiatedEvent = new PaymentInitiatedEvent(
                    event.getOrderId(),
                    razorpayOrderId,
                    event.getTotalAmount(),
                    event.getUserId()
            );
            eventProducer.publishPaymentInitiated(initiatedEvent);

        } catch (Exception e) {
            log.error("Failed to create Razorpay order for Order: {}", event.getOrderId(), e);
            throw new RuntimeException("Failed to create payment order", e);
        }
    }

    /**
     * Step 1 (Legacy/Direct): Create Order
     * Kept for backward compatibility if needed, but prefer handleOrderCreated
     */
    @Transactional
    public String createOrder(PaymentCreateRequest request) {
        // This method might be deprecated or removed if we fully switch to event-driven
        // For now, leaving it but it's not the primary flow anymore.
        throw new UnsupportedOperationException("Direct createOrder is deprecated. Use Event Driven flow.");
    }

    /**
     * Step 2: Verify Signature (Frontend Callback)
     * This is a quick check, but NOT the final confirmation.
     */
    @Transactional
    public void verifyPayment(VerifyPaymentRequest req) {
        Payment payment = paymentRepository.findByRazorpayOrderId(req.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        try {
            JSONObject options = new JSONObject();
            String generatedSignature = calculateSignature(req.getRazorpayOrderId(),req.getRazorpayPaymentId());
            options.put("razorpay_order_id", req.getRazorpayOrderId());
            options.put("razorpay_payment_id", req.getRazorpayPaymentId());
            options.put("razorpay_signature", generatedSignature);
            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);
            if (isValid) {
                payment.setRazorpayPaymentId(req.getRazorpayPaymentId());
                payment.setRazorpaySignature(req.getRazorpaySignature());
                payment.setStatus(PaymentStatus.VERIFIED);
                paymentRepository.save(payment);


                PaymentSuccessEvent paymentSuccessEvent = new PaymentSuccessEvent();
                paymentSuccessEvent.setPaymentId(req.getRazorpayPaymentId());
                paymentSuccessEvent.setOrderId(payment.getOrderId()); // Use stored orderId
                paymentSuccessEvent.setPaymentMethod("");
                eventProducer.publishPaymentSuccess(paymentSuccessEvent);
            } else {
                throw new RuntimeException("Signature verification failed");
            }
        } catch (Exception e) {
            throw new RuntimeException("Payment verification failed", e);
        }
    }

    /**
     * Step 3: Webhook Processing (Final Source of Truth)
     * Captures specific payment methods (UPI/Card/etc)
     */

    @Transactional
    public void processWebhook(String payload, String signature) {
        try {
            // 1. Verify Webhook Signature
            if (!Utils.verifyWebhookSignature(payload, signature, webhookSecret)) {
                log.error("Invalid Webhook Signature");
                throw new SecurityException("Invalid Signature");
            }

            JsonNode root = objectMapper.readTree(payload);
            String event = root.path("event").asText();

            // We only care about "payment.captured"
            if ("payment.captured".equals(event)) {
                JsonNode entity = root.path("payload").path("payment").path("entity");
                String razorpayOrderId = entity.path("order_id").asText();
                String paymentId = entity.path("id").asText();

                // 2. Idempotency Check
                Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                        .orElseThrow(() -> new RuntimeException("Order not found for webhook"));

                if (payment.getStatus() == PaymentStatus.PAID) {
                    log.info("Payment already processed for order: {}", razorpayOrderId);
                    return;
                }

                // 3. Extract & Store Payment Method Details
                enrichPaymentDetails(payment, entity);

                // 4. Mark PAID
                payment.setStatus(PaymentStatus.PAID);
                paymentRepository.save(payment);

                // 5. Notify Ecosystem (Saga)
//                PaymentSuccessEvent successEvent = new PaymentSuccessEvent(
//                        payment.getOrderId(),
//                        paymentId,
//                        payment.getAmount().toString(),
//                        payment.getMethodType().toString()
//                );
//                eventProducer.publishPaymentSuccess(successEvent);

//                log.info("Payment successfully captured and event published for order: {}", payment.getOrderId());
            }

        } catch (Exception e) {
            log.error("Error processing webhook", e);
            throw new RuntimeException("Webhook processing failed");
        }
    }

    /**
     * Helper to extract detailed method info from Razorpay JSON
     */
    private void enrichPaymentDetails(Payment payment, JsonNode entity) {
        String method = entity.path("method").asText(); // "card", "upi", "netbanking", "wallet"
        payment.setEmail(entity.path("email").asText());
        payment.setContact(entity.path("contact").asText());

        switch (method) {
            case "card":
                payment.setMethodType(PaymentMethodType.CARD);
                JsonNode card = entity.path("card");
                payment.setCardNetwork(card.path("network").asText()); // Visa/Mastercard
                payment.setCardLast4(card.path("last4").asText());
                break;
            case "upi":
                payment.setMethodType(PaymentMethodType.UPI);
                payment.setVpa(entity.path("vpa").asText()); // user@upi
                break;
            case "netbanking":
                payment.setMethodType(PaymentMethodType.NETBANKING);
                payment.setBank(entity.path("bank").asText()); // HDFC, SBI
                break;
            case "wallet":
                payment.setMethodType(PaymentMethodType.WALLET);
                payment.setWallet(entity.path("wallet").asText()); // AmazonPay, PhonePe
                break;
            default:
                payment.setMethodType(PaymentMethodType.UNKNOWN);
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
            e.printStackTrace();
            return "Error calculating signature";
        }
    }

    @Transactional
    public void processRefund(String orderId) {
        // Here orderId refers to our internal orderId, so we need to find payment by orderId
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            log.info("Payment already refunded for order: {}", orderId);
            return;
        }

        // Only refund if payment was successful (VERIFIED or PAID)
        if (payment.getStatus() == PaymentStatus.VERIFIED || payment.getStatus() == PaymentStatus.PAID) {
            try {
                JSONObject refundRequest = new JSONObject();
                refundRequest.put("payment_id", payment.getRazorpayPaymentId());
                refundRequest.put("amount", payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue()); // Refund full amount
                refundRequest.put("speed", "normal");

                Refund refund = razorpayClient.payments.refund(refundRequest);
                
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);

                PaymentRefundedEvent event = new PaymentRefundedEvent(orderId, payment.getRazorpayPaymentId(), refund.get("id"));
                eventProducer.publishPaymentRefunded(event);
                
                log.info("Refund processed successfully for order: {}", orderId);

            } catch (Exception e) {
                log.error("Error processing refund for order: {}", orderId, e);
                throw new RuntimeException("Refund failed", e);
            }
        } else {
            log.info("Payment status {} does not require refund for order: {}", payment.getStatus(), orderId);
            // Even if no refund needed, we should probably emit event so Saga can complete
            PaymentRefundedEvent event = new PaymentRefundedEvent(orderId, null, null);
            eventProducer.publishPaymentRefunded(event);
        }
    }
}
