package com.ecommerce.paymentservice.service;
import com.ecommerce.payment.PaymentCreateRequest;
import com.ecommerce.payment.VerifyPaymentRequest;
import com.ecommerce.paymentservice.cleint.CheckoutClient;
import com.ecommerce.paymentservice.dto.CreateOrderResponse;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.enums.PaymentMethodType;
import com.ecommerce.paymentservice.enums.PaymentStatus;
import com.ecommerce.paymentservice.kafka.PaymentEventProducer;
import com.ecommerce.paymentservice.kafka.PaymentSuccessEvent;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;
    private final CheckoutClient checkoutClient;
    private final PaymentEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    /**
     * Step 1: Create Order
     * TRUST: Checkout Service.
     * UNTRUSTED: Frontend inputs regarding price.
     */
    @Transactional
    public String createOrder(PaymentCreateRequest request) {
        try {
            // 3. Create Razorpay Order
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.getAmount());
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_rcptid_" + System.currentTimeMillis());

            Order order = razorpayClient.orders.create(orderRequest);
            return order.get("id");

        } catch (Exception e) {
            throw new RuntimeException("Failed to create payment order", e);
        }
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
            options.put("razorpay_order_id", req.getRazorpayOrderId());
            options.put("razorpay_payment_id", req.getRazorpayPaymentId());
            options.put("razorpay_signature", req.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);
            if (isValid) {
                payment.setRazorpayPaymentId(req.getRazorpayPaymentId());
                payment.setRazorpaySignature(req.getRazorpaySignature());
                payment.setStatus(PaymentStatus.VERIFIED);
                paymentRepository.save(payment);
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
                String orderId = entity.path("order_id").asText();
                String paymentId = entity.path("id").asText();

                // 2. Idempotency Check
                Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                        .orElseThrow(() -> new RuntimeException("Order not found for webhook"));

                if (payment.getStatus() == PaymentStatus.PAID) {
                    log.info("Payment already processed for order: {}", orderId);
                    return;
                }

                // 3. Extract & Store Payment Method Details
                enrichPaymentDetails(payment, entity);

                // 4. Mark PAID
                payment.setStatus(PaymentStatus.PAID);
                paymentRepository.save(payment);

                // 5. Notify Ecosystem (Saga)
                PaymentSuccessEvent successEvent = new PaymentSuccessEvent(
                        payment.getCheckoutId(),
                        paymentId,
                        payment.getAmount().toString(),
                        payment.getMethodType().toString()
                );
                eventProducer.publishPaymentSuccess(successEvent);

                log.info("Payment successfully captured and event published for checkout: {}", payment.getCheckoutId());
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
}