package com.ecommerce.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct; // Use javax.annotation if using older Spring Boot
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.function.Function;

@Component
public class JwtUtil {

    private PublicKey publicKey;

    @PostConstruct
    public void init() {
        try {
            // Load Public Key from resources (public.pem)
            // We use getInputStream() because getFile() fails inside JARs
            InputStream inputStream = new ClassPathResource("public.pem").getInputStream();
            String key = new String(inputStream.readAllBytes())
                    .replaceAll("-----BEGIN PUBLIC KEY-----", "")
                    .replaceAll("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            KeyFactory kf = KeyFactory.getInstance("RSA");
            this.publicKey = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(key)));
        } catch (Exception e) {
            throw new RuntimeException("Could not load Public Key in Gateway", e);
        }
    }

    public Boolean validateToken(String token) {
        try {
            // Verify using the PUBLIC Key
            Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // Log specific error (Expired, Malformed, etc.)
            System.err.println("JWT Validation Failed: " + e.getMessage());
            return false;
        }
    }

    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> (String) claims.get("role"));
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token).getBody();
    }
}