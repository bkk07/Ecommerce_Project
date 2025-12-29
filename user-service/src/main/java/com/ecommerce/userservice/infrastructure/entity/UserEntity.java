package com.ecommerce.userservice.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false) private String password;
    private String phone;

    private boolean isEmailVerified;
    private boolean isPhoneVerified;

    @Column(name = "email_otp")
    private String emailVerificationOtp;

    @Column(name = "email_otp_expiry")
    private LocalDateTime emailOtpExpiry;

    @Column(name = "phone_otp")
    private String phoneVerificationOtp;

    @Column(name = "phone_otp_expiry")
    private LocalDateTime phoneOtpExpiry;

    // --- Forgot Password Fields ---
    @Column(name = "forgot_pass_otp")
    private String forgotPasswordOtp;

    @Column(name = "forgot_pass_otp_expiry")
    private LocalDateTime forgotPasswordOtpExpiry;

    @Column(name = "is_pass_reset_verified")
    private boolean isPasswordResetVerified;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AddressEntity> addresses = new ArrayList<>();
}