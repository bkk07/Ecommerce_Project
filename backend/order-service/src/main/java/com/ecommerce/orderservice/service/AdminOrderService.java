package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.AdminOrderPageResponse;
import com.ecommerce.orderservice.dto.AdminOrderResponse;
import com.ecommerce.orderservice.dto.OrderItemResponse;
import com.ecommerce.orderservice.dto.OrderStatsResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.enums.OrderStatus;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.repository.OrderRepository;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminOrderService {

    private final OrderRepository orderRepository;

    // Revenue-eligible statuses (orders that count towards revenue)
    private static final List<OrderStatus> REVENUE_STATUSES = Arrays.asList(
            OrderStatus.PLACED,
            OrderStatus.PACKED,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED
    );

    /**
     * Get comprehensive order statistics for admin dashboard
     */
    @Cacheable(value = "orderStats", key = "'admin_order_stats'")
    public OrderStatsResponse getOrderStats() {
        log.info("Fetching order statistics for admin dashboard");

        // Get current time references
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.with(LocalTime.MIN);
        LocalDateTime startOfWeek = now.minusDays(7);
        LocalDateTime startOfMonth = now.minusDays(30);
        LocalDateTime startOfYear = now.minusDays(365);

        // Build stats response
        return OrderStatsResponse.builder()
                .totalOrders(orderRepository.count())
                .ordersByStatus(buildOrdersByStatus())
                .revenueStats(buildRevenueStats(startOfToday, startOfWeek, startOfMonth, startOfYear))
                .newOrdersStats(buildNewOrdersStats(startOfToday, startOfWeek, startOfMonth))
                .fulfillmentStats(buildFulfillmentStats())
                .generatedAt(now)
                .build();
    }

    /**
     * Get paginated list of all orders
     */
    public AdminOrderPageResponse getAllOrders(int page, int size, String sortBy, String sortDirection) {
        Sort sort = sortDirection.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Order> orderPage = orderRepository.findAll(pageable);
        
        return buildPageResponse(orderPage);
    }

    /**
     * Get orders filtered by status
     */
    public AdminOrderPageResponse getOrdersByStatus(OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findByStatus(status, pageable);
        
        return buildPageResponse(orderPage);
    }

    /**
     * Get orders within a date range
     */
    public AdminOrderPageResponse getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.findByCreatedAtBetween(startDate, endDate, pageable);
        
        return buildPageResponse(orderPage);
    }

    /**
     * Search orders by orderId or userId
     */
    public AdminOrderPageResponse searchOrders(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Order> orderPage = orderRepository.searchOrders(keyword, pageable);
        
        return buildPageResponse(orderPage);
    }

    /**
     * Get detailed order by ID
     */
    @Retry(name = "databaseRetry")
    public AdminOrderResponse getOrderById(String orderId) {
        Order order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        return mapToAdminOrderResponse(order);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private OrderStatsResponse.OrdersByStatus buildOrdersByStatus() {
        return OrderStatsResponse.OrdersByStatus.builder()
                .pending(orderRepository.countByStatus(OrderStatus.PENDING))
                .paymentReady(orderRepository.countByStatus(OrderStatus.PAYMENT_READY))
                .placed(orderRepository.countByStatus(OrderStatus.PLACED))
                .packed(orderRepository.countByStatus(OrderStatus.PACKED))
                .shipped(orderRepository.countByStatus(OrderStatus.SHIPPED))
                .delivered(orderRepository.countByStatus(OrderStatus.DELIVERED))
                .cancelRequested(orderRepository.countByStatus(OrderStatus.CANCEL_REQUESTED))
                .cancelled(orderRepository.countByStatus(OrderStatus.CANCELLED))
                .build();
    }

    private OrderStatsResponse.RevenueStats buildRevenueStats(
            LocalDateTime startOfToday,
            LocalDateTime startOfWeek,
            LocalDateTime startOfMonth,
            LocalDateTime startOfYear) {
        
        return OrderStatsResponse.RevenueStats.builder()
                .totalRevenue(orderRepository.sumRevenueByStatuses(REVENUE_STATUSES))
                .todayRevenue(orderRepository.sumRevenueSince(REVENUE_STATUSES, startOfToday))
                .weekRevenue(orderRepository.sumRevenueSince(REVENUE_STATUSES, startOfWeek))
                .monthRevenue(orderRepository.sumRevenueSince(REVENUE_STATUSES, startOfMonth))
                .yearRevenue(orderRepository.sumRevenueSince(REVENUE_STATUSES, startOfYear))
                .build();
    }

    private OrderStatsResponse.NewOrdersStats buildNewOrdersStats(
            LocalDateTime startOfToday,
            LocalDateTime startOfWeek,
            LocalDateTime startOfMonth) {
        
        return OrderStatsResponse.NewOrdersStats.builder()
                .today(orderRepository.countOrdersSince(startOfToday))
                .thisWeek(orderRepository.countOrdersSince(startOfWeek))
                .thisMonth(orderRepository.countOrdersSince(startOfMonth))
                .build();
    }

    private OrderStatsResponse.FulfillmentStats buildFulfillmentStats() {
        long pending = orderRepository.countByStatus(OrderStatus.PENDING) 
                     + orderRepository.countByStatus(OrderStatus.PAYMENT_READY);
        long toShip = orderRepository.countByStatus(OrderStatus.PLACED) 
                    + orderRepository.countByStatus(OrderStatus.PACKED);
        long inTransit = orderRepository.countByStatus(OrderStatus.SHIPPED);
        long delivered = orderRepository.countByStatus(OrderStatus.DELIVERED);
        long cancelled = orderRepository.countByStatus(OrderStatus.CANCELLED) 
                       + orderRepository.countByStatus(OrderStatus.CANCEL_REQUESTED);

        return OrderStatsResponse.FulfillmentStats.builder()
                .pendingPayment(pending)
                .toShip(toShip)
                .inTransit(inTransit)
                .delivered(delivered)
                .cancelled(cancelled)
                .build();
    }

    private AdminOrderPageResponse buildPageResponse(Page<Order> orderPage) {
        List<AdminOrderResponse> orders = orderPage.getContent().stream()
                .map(this::mapToAdminOrderResponse)
                .collect(Collectors.toList());

        return AdminOrderPageResponse.builder()
                .orders(orders)
                .currentPage(orderPage.getNumber())
                .totalPages(orderPage.getTotalPages())
                .totalElements(orderPage.getTotalElements())
                .pageSize(orderPage.getSize())
                .hasNext(orderPage.hasNext())
                .hasPrevious(orderPage.hasPrevious())
                .build();
    }

    private AdminOrderResponse mapToAdminOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::mapToOrderItemResponse)
                .collect(Collectors.toList());

        return AdminOrderResponse.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .paymentId(order.getPaymentId())
                .razorpayOrderId(order.getRazorpayOrderId())
                .shippingAddress(order.getShippingAddress())
                .items(items)
                .itemCount(items.size())
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderItemResponse mapToOrderItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .skuCode(item.getSkuCode())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .imageUrl(item.getImageUrl())
                .build();
    }
}
