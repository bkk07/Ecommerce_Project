package com.ecommerce.wishlistservice.repository;

import com.ecommerce.wishlistservice.entity.WishListItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishListItemRepository extends JpaRepository<WishListItem, Long> {

    @Query("SELECT i FROM WishListItem i WHERE i.wishList.userId = :userId AND i.skuCode = :skuCode")
    Optional<WishListItem> findByUserIdAndSkuCode(@Param("userId") Long userId, @Param("skuCode") String skuCode);

    @Query("SELECT COUNT(i) > 0 FROM WishListItem i WHERE i.wishList.userId = :userId AND i.skuCode = :skuCode")
    boolean existsByUserIdAndSkuCode(@Param("userId") Long userId, @Param("skuCode") String skuCode);

    void deleteByWishListUserIdAndSkuCode(Long userId, String skuCode);
}
