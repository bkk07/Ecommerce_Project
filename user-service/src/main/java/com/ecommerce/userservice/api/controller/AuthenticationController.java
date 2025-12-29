package com.ecommerce.userservice.api.controller;
import com.ecommerce.Common;
import com.ecommerce.userservice.api.dto.*;
import com.ecommerce.userservice.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.apache.hc.core5.reactor.Command;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authService;
    public String hello(){
        Common common= new Common("Kiran","RED");
        return common.toString();
    }

    // --- 1. Register & Login ---

    @PostMapping("/register")
    public ResponseEntity<UserAuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<UserAuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // --- 2. Forgot Password Flow ---

    // Step A: Send OTP
    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<?> sendForgotPasswordOtp(@RequestParam String email) {
        authService.initiateForgotPassword(email);
        return ResponseEntity.ok(Map.of("message", "OTP sent to email"));
    }

    // Step B: Verify OTP
    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<?> verifyForgotPasswordOtp(@RequestParam String email, @RequestParam String otp) {
        authService.verifyForgotPasswordOtp(email, otp);
        return ResponseEntity.ok(Map.of("message", "OTP verified. You may now reset your password."));
    }

    // Step C: Update Password
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    // --- 3. Email Verification Flow ---

    // Step A: Send OTP (NEW ENDPOINT)
    @PostMapping("/verify-email/send-otp")
    public ResponseEntity<?> sendEmailVerificationOtp(@RequestParam Long userId) {
        authService.initiateEmailVerification(userId);
        return ResponseEntity.ok(Map.of("message", "Verification OTP sent to email"));
    }

    // Step B: Verify OTP (FIXED PARAMETERS)
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam Long userId, @RequestParam String otp) {
        authService.verifyEmail(userId, otp); // Now passes both arguments
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    // --- 4. Phone Verification Flow ---

    // Step A: Send OTP (NEW ENDPOINT)
    @PostMapping("/verify-phone/send-otp")
    public ResponseEntity<?> sendPhoneVerificationOtp(@RequestParam Long userId) {
        authService.initiatePhoneVerification(userId);
        return ResponseEntity.ok(Map.of("message", "Verification OTP sent to phone"));
    }

    // Step B: Verify OTP (FIXED PARAMETERS)
    @PostMapping("/verify-phone")
    public ResponseEntity<?> verifyPhone(@RequestParam Long userId, @RequestParam String otp) {
        authService.verifyPhone(userId, otp); // Now passes both arguments
        return ResponseEntity.ok(Map.of("message", "Phone verified successfully"));
    }
}