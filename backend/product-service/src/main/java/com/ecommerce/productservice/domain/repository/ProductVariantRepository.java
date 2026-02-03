package com.ecommerce.productservice.domain.repository;

import com.ecommerce.productservice.domain.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    Optional<ProductVariant> findBySku(String sku);

    @Modifying
    @Query("UPDATE ProductVariant pv SET pv.averageRating = :averageRating, pv.ratingCount = :ratingCount WHERE pv.sku = :sku")
    int updateRatingBySku(@Param("sku") String sku, 
                          @Param("averageRating") Double averageRating, 
                          @Param("ratingCount") Long ratingCount);
}
