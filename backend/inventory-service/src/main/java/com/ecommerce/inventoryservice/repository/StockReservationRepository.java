package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.model.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    Optional<StockReservation> findByOrderIdAndSkuCode(String orderId, String skuCode);
}
