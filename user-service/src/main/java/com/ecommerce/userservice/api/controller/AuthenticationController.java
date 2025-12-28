package com.ecommerce.userservice.api.controller;

import com.ecommerce.userservice.api.dto.*;
import com.ecommerce.userservice.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authService;

    @PostMapping("/register")
    public ResponseEntity<UserAuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<UserAuthResponse> login(@RequestBody LoginRequest request) {
        // No try-catch needed here anymore!
        // If authService throws CustomException, GlobalExceptionHandler catches it.
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam Long userId) {
        authService.verifyEmail(userId);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/verify-phone")
    public ResponseEntity<?> verifyPhone(@RequestParam Long userId) {
        authService.verifyPhone(userId);
        return ResponseEntity.ok(Map.of("message", "Phone verified successfully"));
    }
}