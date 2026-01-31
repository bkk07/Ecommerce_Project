package com.ecommerce.wishlistservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wish_list_items", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"wish_list_id", "sku_code"}))
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WishListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wish_list_id", nullable = false)
    private WishList wishList;

    @Column(name = "sku_code", nullable = false)
    private String skuCode;

    @Column(name = "product_id")
    private Long productId;

    @Column(nullable = false)
    private String name;

    private String imageUrl;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WishListItem that)) return false;
        return skuCode != null && skuCode.equals(that.skuCode);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
