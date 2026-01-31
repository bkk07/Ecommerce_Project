package com.ecommerce.wishlistservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Filter to extract user information from headers injected by API Gateway.
 * The API Gateway validates the JWT and forwards user details via headers:
 * - X-Auth-User-Id: The authenticated user's ID
 * - X-Auth-User-Role: The authenticated user's role
 */
@Slf4j
public class GatewayHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        log.debug("Processing request: {} {}", request.getMethod(), request.getRequestURI());

        // Debug: Log all headers in debug mode
        if (log.isDebugEnabled()) {
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                log.debug("Header: {} = {}", headerName, request.getHeader(headerName));
            }
        }

        // Read Headers from Gateway (Try both standard and Auth variants)
        String userId = request.getHeader("X-User-Id");
        if (userId == null) {
            userId = request.getHeader("X-Auth-User-Id");
        }

        String userRole = request.getHeader("X-User-Role");
        if (userRole == null) {
            userRole = request.getHeader("X-Auth-User-Role");
        }

        log.debug("Resolved User ID: {}, Role: {}", userId, userRole);

        // Validate and set authentication context
        if (userId != null && userRole != null) {
            // Format Role (Spring Security expects "ROLE_ADMIN", not just "ADMIN")
            String formattedRole = userRole.startsWith("ROLE_") ? userRole : "ROLE_" + userRole;

            // Create Authority List
            List<SimpleGrantedAuthority> authorities =
                    Collections.singletonList(new SimpleGrantedAuthority(formattedRole));

            // Create Authentication Object - userId is the principal
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            // Set Context
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            log.debug("Authentication set for user: {} with role: {}", userId, formattedRole);
        }

        // Continue Filter Chain
        filterChain.doFilter(request, response);
    }
}
