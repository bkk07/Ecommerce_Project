package com.ecommerce.inventoryservice.model;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_table")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // This is the Business Key linking to Product Service
    @Column(nullable = false, unique = true)
    private String skuCode;

    // The total physical stock in the warehouse
    @Column(nullable = false)
    private Integer quantity;

    // Stock currently locked by users who are in the "Payment Pending" phase
    // Available Stock = quantity - reservedQuantity
    @Column(nullable = false)
    private Integer reservedQuantity = 0;

    // CRITICAL: Optimistic Locking version
    // If two requests try to update the same row, one will fail with OptimisticLockException
    @Version
    private Long version;

    public Integer getAvailableStock() {
        return quantity - reservedQuantity;
    }

}