package com.ecommerce.paymentservice.kafka;
import com.ecommerce.payment.PaymentSuccessEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import static com.ecommerce.common.KafkaProperties.PAYMENTS_EVENTS_SUCCESS_TOPIC;
import static com.ecommerce.common.KafkaProperties.PAYMENT_EVENTS_TOPIC;

@Service
@RequiredArgsConstructor
public class PaymentEventProducer {
    private final KafkaTemplate<String, PaymentSuccessEvent> kafkaTemplate;
    public void publishPaymentSuccess(PaymentSuccessEvent event) {
        kafkaTemplate.send(PAYMENTS_EVENTS_SUCCESS_TOPIC,event.getOrderId(),event);
    }
}