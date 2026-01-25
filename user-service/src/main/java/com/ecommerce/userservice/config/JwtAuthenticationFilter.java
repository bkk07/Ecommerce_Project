package com.ecommerce.userservice.config;

import com.ecommerce.userservice.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            Claims claims = jwtService.parseAndValidate(jwt);
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Create Authentication Token
                List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        authorities
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set Authentication in Context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            // Token validation failed
            // You might want to log this or just let the chain continue (which will result in 403)
        }

        filterChain.doFilter(request, response);
    }
}
