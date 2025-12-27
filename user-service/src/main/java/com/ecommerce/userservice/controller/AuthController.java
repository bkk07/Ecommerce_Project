package com.ecommerce.userservice.controller;
import com.ecommerce.userservice.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final JwtService jwtService;
    // Simple DTO for Login
    public static class     LoginRequest {
        public String username;
        public String password;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // 1. Authenticate against DB (Mock logic here for demo)
        // In real life: authenticationManager.authenticate(...)
        if ("user".equals(request.username) && "password".equals(request.password)) {

            // 2. Generate Token using RSA Private Key
            String token = jwtService.generateToken("USER_ID_12345", "USER");

            return ResponseEntity.ok(Map.of("token", token));
        } else if ("admin".equals(request.username) && "admin".equals(request.password)) {
            String token = jwtService.generateToken("ADMIN_ID_999", "ADMIN");
            return ResponseEntity.ok(Map.of("token", token));
        }

        return ResponseEntity.status(401).body("Invalid Credentials");
    }
}
