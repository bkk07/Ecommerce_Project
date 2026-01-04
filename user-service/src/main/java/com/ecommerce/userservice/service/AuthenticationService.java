package com.ecommerce.userservice.service;

import com.ecommerce.event.UserEvent;
import com.ecommerce.userservice.api.dto.LoginRequest;
import com.ecommerce.userservice.api.dto.RegisterRequest;
import com.ecommerce.userservice.api.dto.UserAuthResponse; // Ensure you have this Enum
import com.ecommerce.userservice.domain.model.User;
import com.ecommerce.userservice.domain.port.UserRepositoryPort;
import com.ecommerce.userservice.exception.CustomException;
import com.ecommerce.userservice.infrastructure.messaging.KafkaNotificationProducer;
import com.ecommerce.userservice.infrastructure.messaging.event.ChannelType;
import com.ecommerce.userservice.infrastructure.messaging.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final KafkaNotificationProducer kafkaProducer;

    // --- Register ---
    public UserAuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException("Email already taken", HttpStatus.CONFLICT);
        }
        // 1. Create User
        User newUser = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .isEmailVerified(false)
                .isPhoneVerified(false)
                .build();
        // 2. Save User
        User savedUser = userRepository.save(newUser);
        // 3. Generate Token
        String token = jwtService.generateToken(savedUser.getId().toString(), "USER");
        // 4. Send Welcome Notification
        Map<String, String> params = new HashMap<>();
        params.put("name", savedUser.getName());


        kafkaProducer.sendNotification(NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString()) // Generate Unique ID
                .eventType("USER_WELCOME")
                .channel(ChannelType.EMAIL) // Enum
                .recipient(savedUser.getEmail())
                .payload(params) // Renamed from params -> payload
                .occurredAt(LocalDateTime.now()) // Timestamp
                .build());

        UserEvent userEvent = new UserEvent();
        userEvent.setUserId(savedUser.getId());
        userEvent.setEmail(savedUser.getEmail());
        userEvent.setName(savedUser.getName());
        userEvent.setPhone(savedUser.getPhone());
        userEvent.setEventType("USER_WELCOME");
        // 5. Publish User Created Event for Notification Service to store user details
        kafkaProducer.sendUserEvent(userEvent);

        return UserAuthResponse.builder()
                .token(token)
                .userId(savedUser.getId())
                .role("USER")
                .build();
    }

    public UserAuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException("Invalid Credentials", HttpStatus.UNAUTHORIZED));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException("Invalid Credentials", HttpStatus.UNAUTHORIZED);
        }
        if(request.getEmail().equals("admin")){
        String tokenTemp = jwtService.generateToken(user.getId().toString(), "ADMIN");
            return UserAuthResponse.builder()
                    .token(tokenTemp)
                    .userId(user.getId())
                    .role("ADMIN")
                    .build();
        }
        String token = jwtService.generateToken(user.getId().toString(), "USER");
        return UserAuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .role("USER")
                .build();
    }

    public void initiateForgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        if (!user.isEmailVerified()) {
            throw new CustomException("Email not verified. Cannot reset password.", HttpStatus.FORBIDDEN);
        }
        user.generateForgotPasswordOtp();
        userRepository.save(user);

        // Send OTP via Kafka (Urgent)
        Map<String, String> params = new HashMap<>();
        params.put("name", user.getName());
        params.put("otp", user.getForgotPasswordOtp());

        kafkaProducer.sendNotification(NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("FORGOT_PASSWORD_OTP")
                .channel(ChannelType.EMAIL)
                .recipient(user.getEmail())
                .payload(params)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    // --- STEP 2: Verify OTP ---
    public void verifyForgotPasswordOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        user.validateForgotPasswordOtp(otp);
        userRepository.save(user);
    }

    // --- STEP 3: Change Password ---
    public void forgotPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        if (!user.isPasswordResetVerified()) {
            throw new CustomException("Verification required. Please verify OTP first.", HttpStatus.FORBIDDEN);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetVerified(false);
        userRepository.save(user);
    }

    // --- Initiate Email Verification ---
    public void initiateEmailVerification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        if (user.isEmailVerified()) {
            throw new CustomException("Email already verified", HttpStatus.BAD_REQUEST);
        }

        user.generateEmailOtp();
        userRepository.save(user);

        // Send OTP via Kafka
        Map<String, String> params = new HashMap<>();
        params.put("name", user.getName());
        params.put("otp", user.getEmailVerificationOtp());

        kafkaProducer.sendNotification(NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("VERIFY_EMAIL_OTP")
                .channel(ChannelType.EMAIL)
                .recipient(user.getEmail())
                .payload(params)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    // --- Complete Email Verification ---
    public void verifyEmail(Long userId, String otp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        user.validateEmailOtp(otp);
        userRepository.save(user);
    }

    // --- Initiate Phone Verification ---
    public void initiatePhoneVerification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        if (user.isPhoneVerified()) {
            throw new CustomException("Phone already verified", HttpStatus.BAD_REQUEST);
        }

        user.generatePhoneOtp();
        userRepository.save(user);

        // Send OTP via Kafka
        Map<String, String> params = new HashMap<>();
        params.put("otp", user.getPhoneVerificationOtp());

        kafkaProducer.sendNotification(NotificationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("VERIFY_PHONE_OTP")
                .channel(ChannelType.SMS)
                .recipient(user.getPhone())
                .payload(params)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    // --- Complete Phone Verification ---
    public void verifyPhone(Long userId, String otp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        user.validatePhoneOtp(otp);
        userRepository.save(user);
    }
}