package com.ecommerce.paymentservice.kafka;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentSuccessEvent {
    private String checkoutId;
    private String razorpayPaymentId;
    private String amount;
    private String paymentMethod; // e.g., "UPI", "VISA-1234"
}