package com.ecommerce.orderservice.controller;

import com.ecommerce.checkout.CreateOrderCommand;
import com.ecommerce.checkout.OrderCheckoutResponse;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    private final OrderService orderService;
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getMyOrders(@PathVariable String userId) {
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderDetails(@PathVariable String orderNumber) {
        return ResponseEntity.ok(orderService.getOrderDetails(orderNumber));
    }

    @PutMapping("/{orderNumber}")
    public ResponseEntity<String>updateOrderStatus(@PathVariable String orderNumber, @RequestParam OrderStatus status){
        return ResponseEntity.ok(orderService.updateStateOfTheOrder(orderNumber,status));

    }
    @PostMapping("/create/order")
    public ResponseEntity<OrderCheckoutResponse> createCheckoutOrder(@RequestBody CreateOrderCommand createOrderCommand) {
        log.info("Creating Order");
        return ResponseEntity.ok(orderService.createOrder(createOrderCommand));
    }

}