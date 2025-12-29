package com.ecommerce.userservice.domain.model;

import com.ecommerce.userservice.exception.CustomException; // Ensure you have this
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class User {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String password;
    private boolean isEmailVerified;
    private boolean isPhoneVerified;
    private List<Address> addresses;

    private String emailVerificationOtp;
    private LocalDateTime emailOtpExpiry;

    private String phoneVerificationOtp;
    private LocalDateTime phoneOtpExpiry;

    private String forgotPasswordOtp;
    private LocalDateTime forgotPasswordOtpExpiry;
    private boolean isPasswordResetVerified;


    // Opt is valid for 15 mins
    public void generateEmailOtp() {
        this.emailVerificationOtp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        this.emailOtpExpiry = LocalDateTime.now().plusMinutes(15);
    }

    // 2. Validate Email OTP
    public void validateEmailOtp(String otp) {
        if (this.emailVerificationOtp == null || !this.emailVerificationOtp.equals(otp)) {
            throw new CustomException("Invalid Email OTP", HttpStatus.BAD_REQUEST);
        }
        if (LocalDateTime.now().isAfter(this.emailOtpExpiry)) {
            throw new CustomException("Email OTP has expired", HttpStatus.BAD_REQUEST);
        }
        this.isEmailVerified = true;
        this.emailVerificationOtp = null;
        this.emailOtpExpiry = null;
    }

    // 3. Generate Phone OTP (6 digits, 5 mins validity)
    public void generatePhoneOtp() {
        this.phoneVerificationOtp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        this.phoneOtpExpiry = LocalDateTime.now().plusMinutes(5);
    }

    // 4. Validate Phone OTP
    public void validatePhoneOtp(String otp) {
        if (this.phoneVerificationOtp == null || !this.phoneVerificationOtp.equals(otp)) {
            throw new CustomException("Invalid Phone OTP", HttpStatus.BAD_REQUEST);
        }
        if (LocalDateTime.now().isAfter(this.phoneOtpExpiry)) {
            throw new CustomException("Phone OTP has expired", HttpStatus.BAD_REQUEST);
        }
        this.isPhoneVerified = true;
        this.phoneVerificationOtp = null;
        this.phoneOtpExpiry = null;
    }

    public boolean canResetPassword() {
        return isEmailVerified || isPhoneVerified;
    }

    // 1. Generate Forgot Password OTP
    public void generateForgotPasswordOtp() {
        this.forgotPasswordOtp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        this.forgotPasswordOtpExpiry = LocalDateTime.now().plusMinutes(5); // 5 mins expiry
        this.isPasswordResetVerified = false; // Reset verification status
    }

    // 2. Validate Forgot Password OTP
    public void validateForgotPasswordOtp(String otp) {
        if (this.forgotPasswordOtp == null || !this.forgotPasswordOtp.equals(otp)) {
            throw new CustomException("Invalid OTP", HttpStatus.BAD_REQUEST);
        }
        if (LocalDateTime.now().isAfter(this.forgotPasswordOtpExpiry)) {
            throw new CustomException("OTP expired", HttpStatus.BAD_REQUEST);
        }
        // OTP is correct, allow password reset
        this.isPasswordResetVerified = true;
        this.forgotPasswordOtp = null; // Clear OTP
        this.forgotPasswordOtpExpiry = null;
    }
}