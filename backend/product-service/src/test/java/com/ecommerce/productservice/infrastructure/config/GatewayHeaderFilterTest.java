package com.ecommerce.productservice.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GatewayHeaderFilter Unit Tests")
class GatewayHeaderFilterTest {

    private GatewayHeaderFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private static final String SHARED_SECRET = "test-shared-secret-key-123";
    private static final String USER_ID = "123";
    private static final String USER_ROLE = "USER";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new GatewayHeaderFilter();
        ReflectionTestUtils.setField(filter, "sharedSecret", SHARED_SECRET);
        ReflectionTestUtils.setField(filter, "signatureVerificationEnabled", true);
    }

    @Nested
    @DisplayName("With Signature Verification Enabled")
    class SignatureVerificationEnabledTests {

        @Test
        @DisplayName("Should authenticate with valid signature")
        void shouldAuthenticateWithValidSignature() throws Exception {
            // Given
            String timestamp = String.valueOf(System.currentTimeMillis());
            String signature = computeSignature(USER_ID, USER_ROLE, timestamp);

            when(request.getHeader("X-Auth-User-Id")).thenReturn(USER_ID);
            when(request.getHeader("X-Auth-User-Role")).thenReturn(USER_ROLE);
            when(request.getHeader("X-Gateway-Signature")).thenReturn(signature);
            when(request.getHeader("X-Gateway-Timestamp")).thenReturn(timestamp);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getPrincipal()).isEqualTo(USER_ID);
            assertThat(auth.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("Should reject request with invalid signature")
        void shouldRejectInvalidSignature() throws Exception {
            // Given
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            when(request.getHeader("X-Auth-User-Id")).thenReturn(USER_ID);
            when(request.getHeader("X-Auth-User-Role")).thenReturn(USER_ROLE);
            when(request.getHeader("X-Gateway-Signature")).thenReturn("invalid-signature");
            when(request.getHeader("X-Gateway-Timestamp")).thenReturn(String.valueOf(System.currentTimeMillis()));
            when(response.getWriter()).thenReturn(printWriter);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            verify(filterChain, never()).doFilter(request, response);
        }

        @Test
        @DisplayName("Should reject request with expired timestamp")
        void shouldRejectExpiredTimestamp() throws Exception {
            // Given
            String oldTimestamp = String.valueOf(System.currentTimeMillis() - 120000); // 2 minutes old
            String signature = computeSignature(USER_ID, USER_ROLE, oldTimestamp);
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            when(request.getHeader("X-Auth-User-Id")).thenReturn(USER_ID);
            when(request.getHeader("X-Auth-User-Role")).thenReturn(USER_ROLE);
            when(request.getHeader("X-Gateway-Signature")).thenReturn(signature);
            when(request.getHeader("X-Gateway-Timestamp")).thenReturn(oldTimestamp);
            when(response.getWriter()).thenReturn(printWriter);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should reject request without signature")
        void shouldRejectMissingSignature() throws Exception {
            // Given
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);

            when(request.getHeader("X-Auth-User-Id")).thenReturn(USER_ID);
            when(request.getHeader("X-Auth-User-Role")).thenReturn(USER_ROLE);
            when(request.getHeader("X-Gateway-Signature")).thenReturn(null);
            when(response.getWriter()).thenReturn(printWriter);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("With Signature Verification Disabled")
    class SignatureVerificationDisabledTests {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(filter, "signatureVerificationEnabled", false);
        }

        @Test
        @DisplayName("Should authenticate without signature when disabled")
        void shouldAuthenticateWithoutSignatureWhenDisabled() throws Exception {
            // Given
            when(request.getHeader("X-Auth-User-Id")).thenReturn(USER_ID);
            when(request.getHeader("X-Auth-User-Role")).thenReturn("ADMIN");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_ADMIN");
        }
    }

    @Nested
    @DisplayName("Without Auth Headers")
    class NoAuthHeadersTests {

        @Test
        @DisplayName("Should continue filter chain without authentication when no headers")
        void shouldContinueWithoutAuthWhenNoHeaders() throws Exception {
            // Given
            when(request.getHeader("X-Auth-User-Id")).thenReturn(null);
            when(request.getHeader("X-Auth-User-Role")).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNull();
        }
    }

    @Nested
    @DisplayName("Role Formatting Tests")
    class RoleFormattingTests {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(filter, "signatureVerificationEnabled", false);
        }

        @Test
        @DisplayName("Should prepend ROLE_ if not present")
        void shouldPrependRolePrefix() throws Exception {
            // Given
            when(request.getHeader("X-Auth-User-Id")).thenReturn(USER_ID);
            when(request.getHeader("X-Auth-User-Role")).thenReturn("ADMIN");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("Should not double-prepend ROLE_")
        void shouldNotDoublePrependRolePrefix() throws Exception {
            // Given
            when(request.getHeader("X-Auth-User-Id")).thenReturn(USER_ID);
            when(request.getHeader("X-Auth-User-Role")).thenReturn("ROLE_ADMIN");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_ADMIN");
        }
    }

    private String computeSignature(String userId, String role, String timestamp) throws Exception {
        String payload = userId + ":" + role + ":" + timestamp;
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(
                SHARED_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }
}
