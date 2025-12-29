package com.ecommerce.productservice.domain.repository;

import com.ecommerce.productservice.domain.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent,Long> {
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();
}
