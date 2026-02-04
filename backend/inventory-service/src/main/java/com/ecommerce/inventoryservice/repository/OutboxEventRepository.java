package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.model.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    
    /**
     * Find unprocessed events with pagination for batch processing.
     * Orders by createdAt to ensure FIFO processing.
     */
    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc(Pageable pageable);
    
    /**
     * Legacy method - use paginated version for production.
     */
    List<OutboxEvent> findByProcessedFalse();
    
    /**
     * Count pending events for health monitoring.
     */
    long countByProcessedFalse();
    
    /**
     * Delete old processed events for cleanup.
     * @param cutoffDate Events processed before this date will be deleted
     * @return Number of deleted records
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.processed = true AND e.createdAt < :cutoffDate")
    int deleteProcessedEventsBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}
