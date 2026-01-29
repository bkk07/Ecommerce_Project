package com.ecommerce.checkoutservice.config;

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
import java.util.Enumeration;
import java.util.List;

public class GatewayHeaderFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        System.out.println("Processing request: " + request.getMethod() + " " + request.getRequestURI());
        
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            System.out.println("Header: " + headerName + " = " + request.getHeader(headerName));
        }
        String userId = request.getHeader("X-User-Id");
        if (userId == null) userId = request.getHeader("X-Auth-User-Id");
        
        String userRole = request.getHeader("X-User-Role");
        if (userRole == null) userRole = request.getHeader("X-Auth-User-Role");

        System.out.println("Resolved User ID: " + userId + ", Role: " + userRole);

        if (userId != null && userRole != null) {
            UsernamePasswordAuthenticationToken auth = getUsernamePasswordAuthenticationToken(userRole, userId);
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
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        return auth;
    }
}