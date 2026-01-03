package com.ecommerce.checkoutservice.openfeign;
import com.ecommerce.payment.PaymentCreateRequest;
import com.ecommerce.payment.VerifyPaymentRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "PAYMENT-SERVICE")
public interface PaymentClient {
    // Returns Razorpay Order ID (String)
    // DEPRECATED: Use Event Driven Flow
    @PostMapping("/api/payments/create-order")
    String createOrder(@RequestBody PaymentCreateRequest request);

    @PostMapping("/api/payments/verify")
    boolean verifyPayment(@RequestBody VerifyPaymentRequest request);
}
