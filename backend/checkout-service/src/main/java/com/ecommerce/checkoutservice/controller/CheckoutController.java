package com.ecommerce.checkoutservice.controller;

import com.ecommerce.checkoutservice.dto.CheckoutResponse;
import com.ecommerce.checkoutservice.dto.InitiateCheckoutRequest;
import com.ecommerce.checkoutservice.dto.PaymentCallbackRequest;
import com.ecommerce.checkoutservice.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
@Tag(name = "Checkout", description = "Checkout operations for e-commerce platform")
public class CheckoutController {
    
    private final CheckoutService checkoutService;
    
    @Operation(
            summary = "Initiate checkout",
            description = "Start the checkout process from cart or direct purchase. " +
                    "Validates products, calculates total, and creates a Razorpay order."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Checkout initiated successfully",
                    content = @Content(schema = @Schema(implementation = CheckoutResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request or validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid auth headers"),
            @ApiResponse(responseCode = "409", description = "Duplicate checkout request (idempotency conflict)"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
            @ApiResponse(responseCode = "503", description = "Service temporarily unavailable")
    })
    @PostMapping("/initiate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CheckoutResponse> initiateCheckout(
            @Parameter(description = "User ID from gateway", required = true)
            @RequestHeader("X-Auth-User-Id") String userId,
            @Valid @RequestBody InitiateCheckoutRequest request) {
        
        request.setUserId(Long.valueOf(userId));
        log.info("Checkout initiated for user: {}, idempotencyKey: {}", userId, request.getIdempotencyKey());
        
        CheckoutResponse response = checkoutService.initiateCheckout(request);
        log.info("Checkout response for user {}: status={}", userId, response.getStatus());
        
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Finalize checkout",
            description = "Complete the checkout after successful payment. " +
                    "Verifies payment signature and confirms the order."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order finalized successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payment callback data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "402", description = "Payment verification failed"),
            @ApiResponse(responseCode = "404", description = "Checkout session not found")
    })
    @PostMapping("/finalize")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> finalizeOrder(
            @Parameter(description = "User ID from gateway", required = true)
            @RequestHeader("X-Auth-User-Id") String userId,
            @Valid @RequestBody PaymentCallbackRequest callback) {
        
        log.info("Finalizing order for user: {}, razorpayOrderId: {}", userId, callback.getRazorpayOrderId());
        
        String result = checkoutService.finalizeOrder(Long.valueOf(userId), callback);
        
        log.info("Order finalized for user {}: {}", userId, result);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Get checkout session status",
            description = "Retrieve the current status of a checkout session"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session status retrieved"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @GetMapping("/status/{orderId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CheckoutResponse> getCheckoutStatus(
            @Parameter(description = "User ID from gateway", required = true)
            @RequestHeader("X-Auth-User-Id") String userId,
            @Parameter(description = "Razorpay Order ID")
            @PathVariable String orderId) {
        
        log.info("Getting checkout status for user: {}, orderId: {}", userId, orderId);
        
        CheckoutResponse response = checkoutService.getCheckoutStatus(Long.valueOf(userId), orderId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Cancel checkout",
            description = "Cancel an in-progress checkout session"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checkout cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot cancel - checkout already completed"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> cancelCheckout(
            @Parameter(description = "User ID from gateway", required = true)
            @RequestHeader("X-Auth-User-Id") String userId,
            @Parameter(description = "Razorpay Order ID")
            @PathVariable String orderId) {
        
        log.info("Cancelling checkout for user: {}, orderId: {}", userId, orderId);
        
        checkoutService.cancelCheckout(Long.valueOf(userId), orderId);
        return ResponseEntity.ok("Checkout cancelled successfully");
    }
}
