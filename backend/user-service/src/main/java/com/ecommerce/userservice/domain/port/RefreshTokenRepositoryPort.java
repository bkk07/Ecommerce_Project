package com.ecommerce.userservice.domain.port;

import com.ecommerce.userservice.infrastructure.entity.RefreshTokenEntity;
import java.util.List;

public interface RefreshTokenRepositoryPort {
    RefreshTokenEntity save(RefreshTokenEntity token);
    RefreshTokenEntity findByToken(String token);
    List<RefreshTokenEntity> findAllByUserId(Long userId);
    void deleteAllByUserId(Long userId);
}
