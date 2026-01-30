package com.ecommerce.paymentservice.controller;
import com.ecommerce.order.OrderCreatedEvent;
import com.ecommerce.payment.PaymentCreateRequest;
import com.ecommerce.payment.PaymentInitiatedEvent;
import com.ecommerce.payment.VerifyPaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentVerifyResponse;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 1. Create Order
    @PostMapping("/create-order")
    public ResponseEntity<String> createOrder(@RequestBody PaymentCreateRequest request) {
        log.info("Creating the order");
        return ResponseEntity.ok(paymentService.createOrder(request));
    }

    // 2. Verify Payment (Frontend Callback)
    @PostMapping("/verify")
    public ResponseEntity<PaymentVerifyResponse> verifyPayment(@RequestBody VerifyPaymentRequest request) {
        log.info("========================================");
        log.info("Payment verification request received");
        log.info("Razorpay Order ID: {}", request.getRazorpayOrderId());
        log.info("Razorpay Payment ID: {}", request.getRazorpayPaymentId());
        log.info("========================================");
        
        Payment payment = paymentService.verifyPayment(request);
        
        PaymentVerifyResponse response = PaymentVerifyResponse.builder()
                .message("Payment Verified Successfully")
                .verified(true)
                .orderId(payment.getOrderId())
                .razorpayOrderId(request.getRazorpayOrderId())
                .razorpayPaymentId(request.getRazorpayPaymentId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name())
                .build();
        
        log.info("Payment verification successful for Order ID: {}", payment.getOrderId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<PaymentInitiatedEvent> createPayment(@RequestBody OrderCreatedEvent request) {
        return ResponseEntity.ok(paymentService.handleOrderCreated(request));
    }
}
