package com.ecommerce.apigateway.filter;

import com.ecommerce.apigateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    private final JwtUtil jwtUtil;

    public AuthFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    public static class Config {
        private String requiredRole;

        public String getRequiredRole() {
            return requiredRole;
        }

        public void setRequiredRole(String requiredRole) {
            this.requiredRole = requiredRole;
        }
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            logger.info("AuthFilter: Incoming request path: {}", path);

            // 1. Bypass Public Endpoints
            if (path.startsWith("/auth") ||
                    path.equals("/api/users/register") ||
                    path.equals("/api/users/validate")) {
                logger.info("AuthFilter: Bypassing JWT validation for: {}", path);
                return chain.filter(exchange);
            }

            // 2. Validate Header Existence
            List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
            if (authHeaders == null || authHeaders.isEmpty()) {
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = authHeaders.get(0);
            if (!authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization header format", HttpStatus.UNAUTHORIZED);
            }

            // 3. Validate Token
            String token = authHeader.substring(7).trim();
            if (!jwtUtil.validateToken(token)) {
                return onError(exchange, "Invalid or Expired Token", HttpStatus.UNAUTHORIZED);
            }

            // 4. Extract Claims & Check Role
            String role = jwtUtil.extractRole(token);
            String userId = jwtUtil.extractUserId(token);

            if (config.getRequiredRole() != null) {
                boolean hasAccess = Arrays.stream(config.getRequiredRole().split(","))
                        .map(String::trim)
                        .anyMatch(requiredRole -> requiredRole.equalsIgnoreCase(role));

                if (!hasAccess) {
                    logger.error("Access Denied. Required: {}, Found: {}", config.getRequiredRole(), role);
                    return onError(exchange, "Insufficient permissions", HttpStatus.FORBIDDEN);
                }
            }

            // 5. Forward with New Headers
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-Auth-User-Id", userId)
                    .header("X-Auth-User-Role", role)
                    .header("X-Auth-Token", token)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        logger.error("Auth Error: {}", err);
        return response.setComplete();
    }
}