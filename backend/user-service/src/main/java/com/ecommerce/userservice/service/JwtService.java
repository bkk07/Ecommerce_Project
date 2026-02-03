package com.ecommerce.userservice.service;

import com.ecommerce.userservice.exception.CustomException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Service for token generation and validation.
 * 
 * SECURITY: Private key is loaded from environment variable or external source.
 * Never commit private keys to version control.
 * 
 * Configuration options (in order of precedence):
 * 1. Environment variable: JWT_PRIVATE_KEY (Base64-encoded PKCS8 key)
 * 2. Application property: jwt.private-key-base64
 * 3. File path: jwt.private-key-path (for development only)
 */
@Service
@Slf4j
public class JwtService {

    @Value("${jwt.private-key-base64:}")
    private String privateKeyBase64;

    @Value("${jwt.token-validity-hours:24}")
    private long tokenValidityHours;

    @Value("${jwt.issuer:user-service}")
    private String issuer;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void init() {
        try {
            loadKeys();
            log.info("JWT keys loaded successfully. Token validity: {} hours", tokenValidityHours);
        } catch (Exception e) {
            log.error("Failed to load JWT keys: {}", e.getMessage());
            throw new RuntimeException("Failed to load JWT keys. " +
                    "Set JWT_PRIVATE_KEY environment variable or jwt.private-key-base64 property.", e);
        }
    }

    private void loadKeys() throws Exception {
        String keyContent = resolvePrivateKey();
        
        if (keyContent == null || keyContent.isBlank()) {
            throw new IllegalStateException(
                    "No private key configured. Set JWT_PRIVATE_KEY env var or jwt.private-key-base64 property.");
        }

        // Clean up PEM formatting if present
        String cleanedKey = keyContent
                .replaceAll("-----BEGIN.*?-----", "")
                .replaceAll("-----END.*?-----", "")
                .replaceAll("\\s", "");

        // Decode Base64
        byte[] encoded = Base64.getDecoder().decode(cleanedKey);

        // Generate Private Key
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        this.privateKey = kf.generatePrivate(keySpec);

        // Derive Public Key from Private Key
        if (this.privateKey instanceof RSAPrivateCrtKey rsaKey) {
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(
                    rsaKey.getModulus(), 
                    rsaKey.getPublicExponent()
            );
            this.publicKey = kf.generatePublic(publicKeySpec);
        } else {
            throw new IllegalStateException("Private key must be an RSA key.");
        }
        
        log.debug("RSA key pair initialized successfully");
    }

    /**
     * Resolve private key from multiple sources in order of precedence:
     * 1. Environment variable JWT_PRIVATE_KEY
     * 2. Application property jwt.private-key-base64
     */
    private String resolvePrivateKey() {
        // 1. Check environment variable first (highest precedence)
        String envKey = System.getenv("JWT_PRIVATE_KEY");
        if (envKey != null && !envKey.isBlank()) {
            log.info("Loading JWT private key from environment variable");
            return envKey;
        }

        // 2. Check application property
        if (privateKeyBase64 != null && !privateKeyBase64.isBlank()) {
            log.info("Loading JWT private key from application property");
            return privateKeyBase64;
        }

        log.warn("No JWT private key found in environment or properties");
        return null;
    }

    public String generateToken(String userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);

        long now = System.currentTimeMillis();
        long validity = tokenValidityHours * 60 * 60 * 1000; // Convert hours to milliseconds

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuer(issuer)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + validity))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public Claims parseAndValidate(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            throw new CustomException("Invalid JWT Token", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Extract user ID from token without full validation.
     * Useful for logging/debugging purposes only.
     */
    public String extractUserIdUnsafe(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            // Simple extraction - for logging only
            return payload;
        } catch (Exception e) {
            return null;
        }
    }
}