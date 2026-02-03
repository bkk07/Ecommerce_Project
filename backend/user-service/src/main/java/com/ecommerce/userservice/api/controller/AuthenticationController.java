package com.ecommerce.userservice.api.controller;

import com.ecommerce.Common;
import com.ecommerce.userservice.api.dto.*;
import com.ecommerce.userservice.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthenticationController {

    private final AuthenticationService authService;
    
    public String hello() {
        Common common = new Common("Kiran", "RED");
        return common.toString();
    }

    // --- 1. Register & Login ---

    @Operation(summary = "Register a new user", description = "Creates a new user account and sends email verification OTP")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = UserAuthResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already taken"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @PostMapping("/register")
    public ResponseEntity<UserAuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(summary = "Register a new admin user", description = "Creates a new admin account (auto-verified)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Admin registered successfully"),
            @ApiResponse(responseCode = "409", description = "Email already taken")
    })
    @PostMapping("/register/admin")
    public ResponseEntity<UserAuthResponse> registerAdmin(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.registerAdmin(request));
    }

    @Operation(summary = "User login", description = "Authenticates user and returns JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Email not verified")
    })
    @PostMapping("/login")
    public ResponseEntity<UserAuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // --- 2. Forgot Password Flow ---

    @Operation(summary = "Send forgot password OTP", description = "Sends OTP to user's email for password reset")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP sent successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Email not verified")
    })
    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<?> sendForgotPasswordOtp(
            @Parameter(description = "User's registered email") 
            @RequestParam @Email(message = "Invalid email format") @NotBlank String email) {
        authService.initiateForgotPassword(email);
        return ResponseEntity.ok(Map.of("message", "OTP sent to email"));
    }

    @Operation(summary = "Verify forgot password OTP", description = "Verifies the OTP sent to user's email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<?> verifyForgotPasswordOtp(
            @Parameter(description = "User's email") @RequestParam @Email @NotBlank String email,
            @Parameter(description = "6-digit OTP") @RequestParam @NotBlank String otp) {
        authService.verifyForgotPasswordOtp(email, otp);
        return ResponseEntity.ok(Map.of("message", "OTP verified. You may now reset your password."));
    }

    @Operation(summary = "Reset password", description = "Updates user's password after OTP verification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password updated successfully"),
            @ApiResponse(responseCode = "403", description = "OTP verification required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    // --- 3. Email Verification Flow ---

    @Operation(summary = "Send email verification OTP", description = "Sends verification OTP to user's email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP sent successfully"),
            @ApiResponse(responseCode = "400", description = "Email already verified"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/verify-email/send-otp")
    public ResponseEntity<?> sendEmailVerificationOtp(
            @Parameter(description = "User ID") @RequestParam Long userId) {
        authService.initiateEmailVerification(userId);
        return ResponseEntity.ok(Map.of("message", "Verification OTP sent to email"));
    }

    @Operation(summary = "Verify email with OTP", description = "Verifies user's email and returns JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/verify-email")
    public ResponseEntity<UserAuthResponse> verifyEmail(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "6-digit OTP") @RequestParam @NotBlank String otp) {
        return ResponseEntity.ok(authService.verifyEmail(userId, otp));
    }

    // --- 4. Phone Verification Flow ---

    @Operation(summary = "Send phone verification OTP", description = "Sends verification OTP to user's phone via SMS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP sent successfully"),
            @ApiResponse(responseCode = "400", description = "Phone already verified"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/verify-phone/send-otp")
    public ResponseEntity<?> sendPhoneVerificationOtp(
            @Parameter(description = "User ID") @RequestParam Long userId) {
        authService.initiatePhoneVerification(userId);
        return ResponseEntity.ok(Map.of("message", "Verification OTP sent to phone"));
    }

    @Operation(summary = "Verify phone with OTP", description = "Verifies user's phone number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Phone verified successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/verify-phone")
    public ResponseEntity<?> verifyPhone(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "6-digit OTP") @RequestParam @NotBlank String otp) {
        authService.verifyPhone(userId, otp);
        return ResponseEntity.ok(Map.of("message", "Phone verified successfully"));
    }
}