package com.ecommerce.productservice.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * Filter to authenticate requests from the API Gateway.
 * 
 * Security: Validates a shared secret signature to prevent header spoofing.
 * Requests that bypass the gateway cannot forge valid signatures.
 */
@Component
@Slf4j
public class GatewayHeaderFilter extends OncePerRequestFilter {

    @Value("${gateway.shared-secret:${GATEWAY_SHARED_SECRET:}}")
    private String sharedSecret;

    @Value("${gateway.signature-verification-enabled:true}")
    private boolean signatureVerificationEnabled;

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_HEADER = "X-Gateway-Signature";
    private static final String TIMESTAMP_HEADER = "X-Gateway-Timestamp";
    private static final long MAX_TIMESTAMP_DRIFT_MS = 60000; // 1 minute

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Read Headers from Gateway
        String userId = request.getHeader("X-Auth-User-Id");
        String userRole = request.getHeader("X-Auth-User-Role");
        String signature = request.getHeader(SIGNATURE_HEADER);
        String timestamp = request.getHeader(TIMESTAMP_HEADER);

        // 2. Validate headers exist and signature is valid
        if (userId != null && userRole != null) {
            
            // Verify gateway signature if enabled and secret is configured
            if (signatureVerificationEnabled && sharedSecret != null && !sharedSecret.isEmpty()) {
                if (!verifySignature(userId, userRole, timestamp, signature)) {
                    log.warn("Invalid gateway signature for userId={}, possible spoofing attempt", userId);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\": \"Invalid gateway signature\"}");
                    return;
                }
            } else if (signatureVerificationEnabled) {
                log.warn("Gateway signature verification enabled but no shared secret configured!");
            }

            // 3. Format Role (Spring Security expects "ROLE_ADMIN", not just "ADMIN")
            UsernamePasswordAuthenticationToken auth = getUsernamePasswordAuthenticationToken(userRole, userId);

            // 4. Set Context
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        // 5. Continue Filter Chain
        filterChain.doFilter(request, response);
    }

    private boolean verifySignature(String userId, String userRole, String timestamp, String signature) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }

        // Validate timestamp to prevent replay attacks
        if (timestamp != null) {
            try {
                long requestTime = Long.parseLong(timestamp);
                long currentTime = System.currentTimeMillis();
                if (Math.abs(currentTime - requestTime) > MAX_TIMESTAMP_DRIFT_MS) {
                    log.warn("Request timestamp too old or in future: {}", timestamp);
                    return false;
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid timestamp format: {}", timestamp);
                return false;
            }
        }

        try {
            // Compute expected signature: HMAC-SHA256(userId:userRole:timestamp)
            String payload = userId + ":" + userRole + ":" + (timestamp != null ? timestamp : "");
            String expectedSignature = computeHmac(payload);
            
            // Constant-time comparison to prevent timing attacks
            return constantTimeEquals(expectedSignature, signature);
        } catch (Exception e) {
            log.error("Error verifying signature", e);
            return false;
        }
    }

    private String computeHmac(String data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec secretKey = new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(secretKey);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private static UsernamePasswordAuthenticationToken getUsernamePasswordAuthenticationToken(String userRole, String userId) {
        String formattedRole = userRole.startsWith("ROLE_") ? userRole : "ROLE_" + userRole;

        // Create Authority List
        List<SimpleGrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(formattedRole));

        // Create Authentication Object
        // Principal = userId, Credentials = null (already auth'd), Authorities = role
        return new UsernamePasswordAuthenticationToken(userId, null, authorities);
    }
}