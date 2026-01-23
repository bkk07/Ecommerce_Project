package com.ecommerce.productservice.domain.repository;

import com.ecommerce.productservice.domain.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvent, String> {
    // Added Pageable to support batching
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc(Pageable pageable);
}
