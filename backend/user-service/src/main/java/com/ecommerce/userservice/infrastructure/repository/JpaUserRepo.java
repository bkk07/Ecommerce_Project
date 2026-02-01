package com.ecommerce.userservice.infrastructure.repository;

import com.ecommerce.userservice.domain.model.enums.Role;
import com.ecommerce.userservice.infrastructure.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface JpaUserRepo extends JpaRepository<UserEntity, Long> {
    
    Optional<UserEntity> findByEmail(String email);
    
    // Count by role
    long countByRole(Role role);
    
    // Count verified users
    long countByIsEmailVerifiedTrue();
    long countByIsPhoneVerifiedTrue();
    
    // Count users registered after a specific date
    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.createdAt >= :since")
    long countUsersRegisteredSince(@Param("since") LocalDateTime since);
}
