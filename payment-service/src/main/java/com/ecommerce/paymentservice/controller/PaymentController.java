package com.ecommerce.paymentservice.controller;
import com.ecommerce.payment.PaymentCreateRequest;
import com.ecommerce.payment.VerifyPaymentRequest;
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
    public ResponseEntity<String> verifyPayment(@RequestBody VerifyPaymentRequest request) {
        paymentService.verifyPayment(request);
        return ResponseEntity.ok("Payment Verified. Waiting for confirmation.");
    }
}
