package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUserId(String userId);
    boolean existsByPaymentId(String paymentId);
    Optional<Order> findByOrderId(String orderId);
    
    // ==================== ADMIN QUERIES ====================
    
    // Count by status
    long countByStatus(OrderStatus status);
    
    // Count orders since a specific date
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :since")
    long countOrdersSince(@Param("since") LocalDateTime since);
    
    // Sum total revenue (all placed/delivered orders)
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status IN :statuses")
    BigDecimal sumRevenueByStatuses(@Param("statuses") List<OrderStatus> statuses);
    
    // Sum revenue since a specific date
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status IN :statuses AND o.createdAt >= :since")
    BigDecimal sumRevenueSince(@Param("statuses") List<OrderStatus> statuses, @Param("since") LocalDateTime since);
    
    // Paginated list with optional status filter
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    
    // Paginated list with date range
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    Page<Order> findByCreatedAtBetween(
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate, 
            Pageable pageable
    );
    
    // Search by orderId or userId
    @Query("SELECT o FROM Order o WHERE o.orderId LIKE %:keyword% OR o.userId LIKE %:keyword%")
    Page<Order> searchOrders(@Param("keyword") String keyword, Pageable pageable);
    
    // All orders paginated (sorted by createdAt desc)
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
}