package com.ecommerce.checkoutservice.controller;

import com.ecommerce.checkoutservice.dto.CheckoutResponse;
import com.ecommerce.checkoutservice.dto.InitiateCheckoutRequest;
import com.ecommerce.checkoutservice.dto.PaymentCallbackRequest;
import com.ecommerce.checkoutservice.service.CheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping("/initiate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CheckoutResponse> initiateCheckout(@RequestHeader("X-Auth-User-Id") String userId,@RequestBody InitiateCheckoutRequest request) {
        request.setUserId(Long.valueOf(userId));
        return ResponseEntity.ok(checkoutService.initiateCheckout(request));
    }
    @PostMapping("/finalize")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> finalizeOrder(@RequestBody PaymentCallbackRequest callback) {
        return ResponseEntity.ok(checkoutService.finalizeOrder(callback));
    }
}
