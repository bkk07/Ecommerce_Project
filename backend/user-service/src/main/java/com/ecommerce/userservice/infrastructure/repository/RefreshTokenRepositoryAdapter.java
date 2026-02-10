package com.ecommerce.userservice.infrastructure.repository;

import com.ecommerce.userservice.domain.port.RefreshTokenRepositoryPort;
import com.ecommerce.userservice.infrastructure.entity.RefreshTokenEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {
    @Autowired
    private JpaRefreshTokenRepo jpaRepo;

    @Override
    public RefreshTokenEntity save(RefreshTokenEntity token) {
        return jpaRepo.save(token);
    }

    @Override
    public RefreshTokenEntity findByToken(String token) {
        return jpaRepo.findByToken(token).orElse(null);
    }

    @Override
    public List<RefreshTokenEntity> findAllByUserId(Long userId) {
        return jpaRepo.findAllByUserId(userId);
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        jpaRepo.deleteAllByUserId(userId);
    }
}
