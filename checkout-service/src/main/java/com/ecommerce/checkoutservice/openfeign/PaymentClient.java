package com.ecommerce.checkoutservice.openfeign;
import com.ecommerce.checkoutservice.dto.PaymentVerificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "PAYMENT-SERVICE")
public interface PaymentClient {
    // Returns Razorpay Order ID (String)
    @PostMapping("/api/payments/create-order")
    String createOrder(@RequestParam Long amount);

    @PostMapping("/api/payments/verify")
    boolean verifyPayment(@RequestBody PaymentVerificationRequest request);
}