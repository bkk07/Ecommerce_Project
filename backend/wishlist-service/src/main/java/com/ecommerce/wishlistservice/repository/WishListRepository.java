package com.ecommerce.wishlistservice.repository;

import com.ecommerce.wishlistservice.entity.WishList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishListRepository extends JpaRepository<WishList, Long> {

    Optional<WishList> findByUserId(Long userId);

    @Query("SELECT w FROM WishList w LEFT JOIN FETCH w.items WHERE w.userId = :userId")
    Optional<WishList> findByUserIdWithItems(@Param("userId") Long userId);

    boolean existsByUserId(Long userId);
}
