package com.ecommerce.userservice.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;

import jakarta.annotation.PostConstruct;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private PrivateKey privateKey;

    @PostConstruct
    public void init() {
        try {
            // 1. Read the private_key.pem file
            InputStream inputStream = new ClassPathResource("private_key.pem").getInputStream();
            // Read the entire file as a String
            String keyContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            // 2. CLEANUP: Remove the headers, footers, and newlines
            // This Regex removes ANY header starting with -----BEGIN and ending with -----
            String privateKeyPEM = keyContent
                    .replaceAll("-----BEGIN.*?-----", "")
                    .replaceAll("-----END.*?-----", "")
                    .replaceAll("\\s", ""); // Removes newlines and spaces

            // 3. Decode the clean Base64 string
            byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

            // 4. Generate the Private Key object
            KeyFactory kf = KeyFactory.getInstance("RSA");
            this.privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(encoded));

        } catch (Exception e) {
            throw new RuntimeException("Failed to load Private Key. Ensure file is in 'src/main/resources' and formatted correctly.", e);
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
                .signWith(privateKey, SignatureAlgorithm.RS256) // SIGNING WITH PRIVATE KEY
                .compact();
    }
}