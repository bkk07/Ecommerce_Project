package com.ecommerce.checkoutservice.openfeign;

import com.ecommerce.checkout.CreateOrderCommand;
import com.ecommerce.checkout.OrderCheckoutResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ORDER-SERVICE")
public interface OrderClient {
    @PostMapping("/api/orders/create/order")
    OrderCheckoutResponse createCheckoutOrder(@RequestBody CreateOrderCommand command);
}
