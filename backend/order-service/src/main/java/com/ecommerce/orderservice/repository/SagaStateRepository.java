package com.ecommerce.orderservice.repository;
import com.ecommerce.orderservice.entity.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, String> {
    @Query("SELECT s FROM SagaState s WHERE s.updatedAt < :cutoffTime AND (s.inventoryReleased = false OR s.paymentRefunded = false)")
    List<SagaState> findStuckSagas(LocalDateTime cutoffTime);
}
