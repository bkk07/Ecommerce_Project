package com.ecommerce.userservice.service;

import com.ecommerce.userservice.exception.CustomException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;

import jakarta.annotation.PostConstruct;

import java.io.InputStream;
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

@Service
public class JwtService {

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @PostConstruct
    public void init() {
        try {
            loadKeys();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Keys. Ensure private_key.pem is in 'src/main/resources' and formatted correctly.", e);
        }
    }

    private void loadKeys() throws Exception {
        // 1. Read the private_key.pem file
        InputStream inputStream = new ClassPathResource("private_key.pem").getInputStream();
        String keyContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

        // 2. Clean up the PEM string
        String privateKeyPEM = keyContent
                .replaceAll("-----BEGIN.*?-----", "")
                .replaceAll("-----END.*?-----", "")
                .replaceAll("\\s", "");

        // 3. Decode Base64
        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

        // 4. Generate Private Key
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        this.privateKey = kf.generatePrivate(keySpec);

        // 5. Derive Public Key from Private Key
        // Since we only have private_key.pem, we can extract the public key from it
        // if it is an RSA key (which it is, based on RS256 usage).
        if (this.privateKey instanceof RSAPrivateCrtKey) {
            RSAPrivateCrtKey privk = (RSAPrivateCrtKey) this.privateKey;
            RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(privk.getModulus(), privk.getPublicExponent());
            this.publicKey = kf.generatePublic(publicKeySpec);
        } else {
            throw new IllegalStateException("Could not derive public key from private key. Ensure it is an RSA Private Key.");
        }
    }

    public String generateToken(String userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 100)) // 100 hours
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
            throw new CustomException("Invalid JWT Token", HttpStatus.UNAUTHORIZED);
        }
    }
}