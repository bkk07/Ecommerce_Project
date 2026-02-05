package com.ecommerce.paymentservice.controller;

import com.ecommerce.order.OrderCreatedEvent;
import com.ecommerce.payment.PaymentCreateRequest;
import com.ecommerce.payment.PaymentInitiatedEvent;
import com.ecommerce.payment.VerifyPaymentRequest;
import com.ecommerce.paymentservice.dto.ErrorResponse;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.dto.PaymentVerifyResponse;
import com.ecommerce.paymentservice.dto.response.ApiErrorResponse;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.mapper.PaymentMapper;
import com.ecommerce.paymentservice.service.PaymentService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Payment Operations", description = "Core payment operations - create, verify, status, refund")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    @Operation(
            summary = "Create payment order (Legacy)",
            description = "Creates a new payment order. This is a legacy endpoint - prefer using event-driven flow."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/create-order")
    public ResponseEntity<String> createOrder(
            @Parameter(description = "Payment creation request")
            @Valid @RequestBody PaymentCreateRequest request) {
        log.info("Creating the order");
        return ResponseEntity.ok(paymentService.createOrder(request));
    }

    @Operation(
            summary = "Verify payment signature",
            description = """
                    Verifies the payment signature from Razorpay frontend callback.
                    
                    This endpoint is called after the user completes payment on Razorpay checkout.
                    It verifies the signature to ensure the payment wasn't tampered with.
                    
                    **Note**: Final payment confirmation is done via webhook. This verification
                    is for immediate frontend feedback only.
                    
                    Rate limited to 50 requests per second.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment verified successfully",
                    content = @Content(schema = @Schema(implementation = PaymentVerifyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Signature verification failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                        "timestamp": "2026-02-05T10:30:00",
                                        "status": 400,
                                        "errorCode": "PAYMENT_003",
                                        "error": "Payment Verification Failed",
                                        "message": "Signature verification failed for Razorpay order: order_xxx",
                                        "path": "/api/payments/verify"
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "Payment not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    headers = @Header(name = "Retry-After", description = "Seconds to wait before retrying"),
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Service unavailable (Circuit breaker open)",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/verify")
    public ResponseEntity<PaymentVerifyResponse> verifyPayment(
            @Parameter(description = "Payment verification request with Razorpay details")
            @Valid @RequestBody VerifyPaymentRequest request) {
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

    @Operation(
            summary = "Create payment from order event",
            description = """
                    Creates a Razorpay payment order from OrderCreatedEvent.
                    
                    This endpoint is used by the Order Service via Feign client to initiate
                    a payment for a newly created order.
                    
                    **Resilience Features:**
                    - Circuit breaker protection
                    - Automatic retry with exponential backoff
                    - Bulkhead isolation (max 25 concurrent calls)
                    - Rate limiting (100 requests/second)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment initiated successfully",
                    content = @Content(schema = @Schema(implementation = PaymentInitiatedEvent.class))),
            @ApiResponse(responseCode = "409", description = "Payment already exists for this order",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Payment gateway unavailable",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/create")
    public ResponseEntity<PaymentInitiatedEvent> createPayment(
            @Parameter(description = "Order created event with order details")
            @Valid @RequestBody OrderCreatedEvent request) {
        log.info("Creating payment for order: {}", request.getOrderId());
        return ResponseEntity.ok(paymentService.handleOrderCreated(request));
    }

    @Operation(
            summary = "Get payment by order ID",
            description = "Retrieves payment details for a specific order"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment found",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Payment not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(
            @Parameter(description = "Order ID", required = true, example = "ORD-2024-001")
            @PathVariable 
            @NotBlank(message = "Order ID is required")
            String orderId) {
        log.info("Getting payment for order: {}", orderId);
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(paymentMapper.toResponse(payment));
    }

    @Operation(
            summary = "Process refund",
            description = """
                    Processes a refund for a paid order.
                    
                    **Note**: Refund can only be processed for payments with status VERIFIED or PAID.
                    Rate limited to 10 requests per second.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Refund processed successfully"),
            @ApiResponse(responseCode = "404", description = "Payment not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Payment gateway unavailable",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/refund/{orderId}")
    @RateLimiter(name = "refundApi")
    public ResponseEntity<String> refundPayment(
            @Parameter(description = "Order ID", required = true, example = "ORD-2024-001")
            @PathVariable 
            @NotBlank(message = "Order ID is required")
            String orderId) {
        log.info("Processing refund for order: {}", orderId);
        paymentService.processRefund(orderId);
        return ResponseEntity.ok("Refund processed successfully");
    }

    @Operation(
            summary = "Get payment status",
            description = "Retrieves the current status of a payment by Razorpay order ID"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment status retrieved",
                    content = @Content(
                            schema = @Schema(type = "string", example = "PAID"),
                            examples = @ExampleObject(value = "PAID")
                    )),
            @ApiResponse(responseCode = "404", description = "Payment not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/status/{razorpayOrderId}")
    public ResponseEntity<String> getPaymentStatus(
            @Parameter(description = "Razorpay Order ID", required = true, example = "order_M1234567890abc")
            @PathVariable 
            @NotBlank(message = "Razorpay Order ID is required")
            @Pattern(regexp = "^order_[a-zA-Z0-9]{14,}$", message = "Invalid Razorpay Order ID format")
            String razorpayOrderId) {
        log.info("Getting payment status for Razorpay order: {}", razorpayOrderId);
        return ResponseEntity.ok(paymentService.getPaymentStatus(razorpayOrderId));
    }
}
