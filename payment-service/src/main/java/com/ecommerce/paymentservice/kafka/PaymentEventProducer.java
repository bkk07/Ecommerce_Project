package com.ecommerce.paymentservice.kafka;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentEventProducer {
    private final KafkaTemplate<String, PaymentSuccessEvent> kafkaTemplate;

    public void publishPaymentSuccess(PaymentSuccessEvent event) {
        // Topic: payment-success
        // Key: checkoutId (ensures ordering if needed)
        kafkaTemplate.send("payment-success", event.getCheckoutId(), event);
    }
}