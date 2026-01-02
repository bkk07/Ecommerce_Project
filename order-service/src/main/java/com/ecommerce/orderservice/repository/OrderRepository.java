package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(String userId);
    boolean existsByPaymentId(String paymentId);
    Optional<Order> findByOrderId(String orderId);
}