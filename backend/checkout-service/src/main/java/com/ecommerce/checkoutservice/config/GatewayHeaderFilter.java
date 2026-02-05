package com.ecommerce.checkoutservice.config;

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
import java.util.List;

@Slf4j
public class GatewayHeaderFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        log.debug("Processing request: {} {}", request.getMethod(), request.getRequestURI());
        
        // Only log non-sensitive headers at trace level for debugging
        if (log.isTraceEnabled()) {
            log.trace("Request headers present: Content-Type={}, Accept={}", 
                    request.getHeader("Content-Type"), 
                    request.getHeader("Accept"));
        }
        
        String userId = request.getHeader("X-User-Id");
        if (userId == null) userId = request.getHeader("X-Auth-User-Id");
        
        String userRole = request.getHeader("X-User-Role");
        if (userRole == null) userRole = request.getHeader("X-Auth-User-Role");

        log.debug("Resolved User ID: {}, Role: {}", userId, userRole != null ? "[PRESENT]" : "[ABSENT]");

        if (userId != null && userRole != null) {
            UsernamePasswordAuthenticationToken auth = getUsernamePasswordAuthenticationToken(userRole, userId);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        // Continue Filter Chain
        filterChain.doFilter(request, response);
    }

    private static UsernamePasswordAuthenticationToken getUsernamePasswordAuthenticationToken(String userRole, String userId) {
        String formattedRole = userRole.startsWith("ROLE_") ? userRole : "ROLE_" + userRole;

        // Create Authority List
        List<SimpleGrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(formattedRole));

        // Create Authentication Object
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        return auth;
    }
}