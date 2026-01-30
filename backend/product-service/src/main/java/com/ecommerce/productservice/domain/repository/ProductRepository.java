package com.ecommerce.productservice.domain.repository;

import com.ecommerce.productservice.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("SELECT p FROM Product p JOIN p.variants v WHERE v.sku = :sku")
    Optional<Product> findByVariantSku(@Param("sku") String sku);
}
