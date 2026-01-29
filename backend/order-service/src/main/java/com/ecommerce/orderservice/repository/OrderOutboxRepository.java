package com.ecommerce.orderservice.repository;

import com.ecommerce.orderservice.entity.OrderOutbox;
import com.ecommerce.orderservice.entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, String> {
    List<OrderOutbox> findByStatus(OutboxStatus status);
}
