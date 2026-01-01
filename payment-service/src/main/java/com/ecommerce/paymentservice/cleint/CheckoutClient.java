package com.ecommerce.paymentservice.cleint;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.math.BigDecimal;

@FeignClient(name = "CHECKOUT-SERVICE")
public interface CheckoutClient {

    @GetMapping("/api/checkout/{checkoutId}/summary")
    CheckoutSummary getCheckoutSummary(@PathVariable("checkoutId") String checkoutId);
}