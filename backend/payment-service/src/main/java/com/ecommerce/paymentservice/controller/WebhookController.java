package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.dto.ErrorResponse;
import com.ecommerce.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Payment gateway webhook endpoints")
public class WebhookController {

    private final PaymentService paymentService;

    @Operation(
            summary = "Handle Razorpay webhook",
            description = """
                    Receives and processes webhook events from Razorpay.
                    
                    Supported events:
                    - `payment.captured`: Payment was successfully captured
                    - `payment.failed`: Payment failed
                    - `refund.processed`: Refund was processed
                    
                    **Important**: Always returns 200 OK to Razorpay to acknowledge receipt.
                    The signature is verified to ensure webhook authenticity.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Webhook received and processed"),
            @ApiResponse(responseCode = "400", description = "Invalid webhook signature",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/razorpay")
    public ResponseEntity<Void> handleRazorpayWebhook(
            @Parameter(description = "Raw webhook payload from Razorpay", required = true)
            @RequestBody String payload,
            @Parameter(description = "Razorpay signature for verification", required = true)
            @RequestHeader("X-Razorpay-Signature") String signature) {
        
        log.info("Received Razorpay Webhook. Signature: {}", signature);
        paymentService.processWebhook(payload, signature);
        return ResponseEntity.ok().build(); // Always return 200 OK to Razorpay
    }
}
