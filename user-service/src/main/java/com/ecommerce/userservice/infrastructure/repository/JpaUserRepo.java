package com.ecommerce.userservice.infrastructure.repository;

import com.ecommerce.userservice.infrastructure.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaUserRepo  extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
}
