package com.ecommerce.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Complete order statistics response for admin dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatsResponse {
    
    private long totalOrders;
    private OrdersByStatus ordersByStatus;
    private RevenueStats revenueStats;
    private NewOrdersStats newOrdersStats;
    private FulfillmentStats fulfillmentStats;
    private LocalDateTime generatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrdersByStatus {
        private long pending;
        private long paymentReady;
        private long placed;
        private long packed;
        private long shipped;
        private long delivered;
        private long cancelRequested;
        private long cancelled;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueStats {
        private BigDecimal totalRevenue;
        private BigDecimal todayRevenue;
        private BigDecimal weekRevenue;
        private BigDecimal monthRevenue;
        private BigDecimal yearRevenue;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewOrdersStats {
        private long today;
        private long thisWeek;
        private long thisMonth;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FulfillmentStats {
        private long pendingPayment;
        private long toShip;
        private long inTransit;
        private long delivered;
        private long cancelled;
    }
}
