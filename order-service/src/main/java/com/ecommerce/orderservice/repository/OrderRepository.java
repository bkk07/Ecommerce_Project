package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // 1. Used for "My Orders" page
    // Fetches all orders belonging to a specific user
    List<Order> findByUserId(String userId);

    // 2. Used for "Track Order" or "Order Details" page
    // Finds a specific order by its public UUID (not the database ID)
    Optional<Order> findByOrderNumber(String orderNumber);

    // 3. CRITICAL: Used for Idempotency in Kafka Listener
    // Checks if we have already processed a payment ID to prevent duplicate orders
    boolean existsByPaymentId(String paymentId);
}