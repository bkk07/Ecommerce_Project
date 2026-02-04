package com.ecommerce.inventoryservice.repository;

import com.ecommerce.inventoryservice.model.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    /**
     * Find inventory by SKU code.
     */
    Optional<Inventory> findBySkuCode(String skuCode);
    
    /**
     * Find inventory by SKU code with pessimistic write lock.
     * Use this for high-contention scenarios to prevent excessive optimistic lock retries.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.skuCode = :skuCode")
    Optional<Inventory> findBySkuCodeForUpdate(@Param("skuCode") String skuCode);
    
    /**
     * Find multiple inventories by SKU codes.
     */
    List<Inventory> findBySkuCodeIn(List<String> skuCodes);
    
    /**
     * Check if SKU exists.
     */
    boolean existsBySkuCode(String skuCode);
}
