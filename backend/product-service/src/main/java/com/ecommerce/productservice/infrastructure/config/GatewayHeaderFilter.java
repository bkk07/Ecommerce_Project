package com.ecommerce.productservice.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
public class GatewayHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Read Headers from Gateway
        String userId = request.getHeader("X-Auth-User-Id");
        String userRole = request.getHeader("X-Auth-User-Role");

        // 2. Validate (Ensure headers exist)
        if (userId != null && userRole != null) {

            // 3. Format Role (Spring Security expects "ROLE_ADMIN", not just "ADMIN")
            // If Gateway sends "ADMIN", we prepend "ROLE_"
            UsernamePasswordAuthenticationToken auth = getUsernamePasswordAuthenticationToken(userRole, userId);

            // 6. Set Context
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        // 7. Continue Filter Chain
        filterChain.doFilter(request, response);
    }

    private static UsernamePasswordAuthenticationToken getUsernamePasswordAuthenticationToken(String userRole, String userId) {
        String formattedRole = userRole.startsWith("ROLE_") ? userRole : "ROLE_" + userRole;

        // 4. Create Authority List
        List<SimpleGrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(formattedRole));

        // 5. Create Authentication Object
        // Principal = userId, Credentials = null (already auth'd), Authorities = role
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        return auth;
    }
}