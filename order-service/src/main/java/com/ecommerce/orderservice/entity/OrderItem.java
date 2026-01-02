package com.ecommerce.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String skuCode;
    private String productName; // Snapshot name (if product changes name later)
    private BigDecimal price;   // Snapshot price (if price changes later)
    private Integer quantity;
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}