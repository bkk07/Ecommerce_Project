package com.ecommerce.userservice.service;

import com.ecommerce.userservice.domain.port.RefreshTokenRepositoryPort;
import com.ecommerce.userservice.infrastructure.entity.RefreshTokenEntity;
import org.springframework.stereotype.Service;
import java.util.List;
import java.time.temporal.ChronoUnit;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepositoryPort refreshTokenRepo;
    private final JwtService jwtService;

    public RefreshTokenService(RefreshTokenRepositoryPort refreshTokenRepo, JwtService jwtService) {
        this.refreshTokenRepo = refreshTokenRepo;
        this.jwtService = jwtService;
    }

    // Create a new refresh token
    public RefreshTokenEntity createRefreshToken(Long userId, String deviceInfo) {
        String token = jwtService.generateRefreshToken();
        long validityMs = jwtService.getRefreshTokenValidityMs();
        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .token(token)
                .userId(userId)
                .createdAt(java.time.LocalDateTime.now())
                .expiresAt(java.time.LocalDateTime.now().plus(validityMs, ChronoUnit.MILLIS))
                .revoked(false)
                .deviceInfo(deviceInfo != null ? deviceInfo : "unknown")
                .build();
        return refreshTokenRepo.save(entity);
    }

    // Validate a refresh token
    public RefreshTokenEntity validateRefreshToken(String token) {
        RefreshTokenEntity entity = refreshTokenRepo.findByToken(token);
        if (entity == null || entity.isRevoked() || entity.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new com.ecommerce.userservice.exception.CustomException("Invalid or expired refresh token", org.springframework.http.HttpStatus.UNAUTHORIZED);
        }
        return entity;
    }

    // Rotate (replace) a refresh token
    public RefreshTokenEntity rotateRefreshToken(RefreshTokenEntity oldToken) {
        revokeToken(oldToken.getToken());
        return createRefreshToken(oldToken.getUserId(), oldToken.getDeviceInfo());
    }

    // Revoke a single token
    public void revokeToken(String token) {
        RefreshTokenEntity entity = refreshTokenRepo.findByToken(token);
        if (entity != null && !entity.isRevoked()) {
            entity.setRevoked(true);
            refreshTokenRepo.save(entity);
        }
    }

    // Revoke all tokens for a user
    public void revokeAllUserTokens(Long userId) {
        java.util.List<RefreshTokenEntity> tokens = refreshTokenRepo.findAllByUserId(userId);
        for (RefreshTokenEntity token : tokens) {
            token.setRevoked(true);
            refreshTokenRepo.save(token);
        }
        refreshTokenRepo.deleteAllByUserId(userId);
    }

    // List all tokens for a user
    public java.util.List<RefreshTokenEntity> getUserTokens(Long userId) {
        return refreshTokenRepo.findAllByUserId(userId);
    }
}
