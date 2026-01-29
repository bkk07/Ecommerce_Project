package com.ecommerce.notificationservice.infrastructure.repository;

import com.ecommerce.notificationservice.infrastructure.entity.UserProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaUserProfileRepository extends JpaRepository<UserProfileEntity, Long> {
}
