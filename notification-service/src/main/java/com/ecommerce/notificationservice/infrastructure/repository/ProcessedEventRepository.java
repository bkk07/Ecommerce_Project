package com.ecommerce.notificationservice.infrastructure.repository;

import com.ecommerce.notificationservice.infrastructure.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {
}
