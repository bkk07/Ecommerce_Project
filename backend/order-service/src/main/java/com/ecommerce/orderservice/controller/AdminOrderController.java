package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.AdminOrderPageResponse;
import com.ecommerce.orderservice.dto.AdminOrderResponse;
import com.ecommerce.orderservice.dto.OrderStatsResponse;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@Slf4j
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    /**
     * GET /admin/orders/stats
     * Get comprehensive order statistics for admin dashboard
     */
    @GetMapping("/stats")
    public ResponseEntity<OrderStatsResponse> getOrderStats() {
        log.info("Admin request: fetching order statistics");
        OrderStatsResponse stats = adminOrderService.getOrderStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /admin/orders
     * Get paginated list of all orders
     */
    @GetMapping
    public ResponseEntity<AdminOrderPageResponse> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        log.info("Admin request: fetching all orders - page={}, size={}, sortBy={}, sortDirection={}", 
                page, size, sortBy, sortDirection);
        
        AdminOrderPageResponse response = adminOrderService.getAllOrders(page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /admin/orders/status/{status}
     * Get orders filtered by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<AdminOrderPageResponse> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Admin request: fetching orders by status={} - page={}, size={}", status, page, size);
        
        AdminOrderPageResponse response = adminOrderService.getOrdersByStatus(status, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /admin/orders/date-range
     * Get orders within a date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<AdminOrderPageResponse> getOrdersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Admin request: fetching orders from {} to {} - page={}, size={}", 
                startDate, endDate, page, size);
        
        AdminOrderPageResponse response = adminOrderService.getOrdersByDateRange(startDate, endDate, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /admin/orders/search
     * Search orders by orderId or userId
     */
    @GetMapping("/search")
    public ResponseEntity<AdminOrderPageResponse> searchOrders(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("Admin request: searching orders with keyword='{}' - page={}, size={}", keyword, page, size);
        
        AdminOrderPageResponse response = adminOrderService.searchOrders(keyword, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /admin/orders/{orderId}
     * Get detailed order information by ID
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderResponse> getOrderById(@PathVariable String orderId) {
        log.info("Admin request: fetching order details for orderId={}", orderId);
        
        AdminOrderResponse order = adminOrderService.getOrderById(orderId);
        return ResponseEntity.ok(order);
    }
}
