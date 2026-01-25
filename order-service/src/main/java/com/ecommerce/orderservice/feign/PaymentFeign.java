package com.ecommerce.orderservice.feign;

import com.ecommerce.order.OrderCreatedEvent;
import com.ecommerce.payment.PaymentInitiatedEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
@FeignClient(name="PAYMENT-SERVICE")
public interface PaymentFeign {
    @PostMapping("/api/payments/create")
    PaymentInitiatedEvent createPayment(@RequestBody OrderCreatedEvent request);
}