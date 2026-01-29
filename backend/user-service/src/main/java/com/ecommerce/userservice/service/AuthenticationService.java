package com.ecommerce.userservice.service;

import com.ecommerce.event.UserEvent;
import com.ecommerce.notification.ChannelType;
import com.ecommerce.notification.NotificationEvent;
import com.ecommerce.userservice.api.dto.LoginRequest;
import com.ecommerce.userservice.api.dto.RegisterRequest;
import com.ecommerce.userservice.api.dto.UserAuthResponse;
import com.ecommerce.userservice.domain.model.User;
import com.ecommerce.userservice.domain.model.enums.Role;
import com.ecommerce.userservice.domain.port.NotificationProducerPort;
import com.ecommerce.userservice.domain.port.UserRepositoryPort;
import com.ecommerce.userservice.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final NotificationProducerPort notificationProducer;

    // --- Register ---
    @Transactional
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
                .role(Role.USER)
                .isEmailVerified(false)
                .isPhoneVerified(false)
                .build();
        // 2. Save User
        User savedUser = userRepository.save(newUser);
        // 3. Generate Token
        String token = jwtService.generateToken(savedUser.getId().toString(), savedUser.getRole().name());
        // 4. Send Welcome Notification
        Map<String, String> params = new HashMap<>();
        params.put("name", savedUser.getName());


        NotificationEvent welcome = new NotificationEvent();
        welcome.setEventId(UUID.randomUUID().toString());
        welcome.setEventType("USER_WELCOME");
        welcome.setChannel(ChannelType.EMAIL);
        welcome.setRecipient(savedUser.getEmail());
        welcome.setPayload(params);
        welcome.setOccurredAt(LocalDateTime.now());
        notificationProducer.sendNotification(welcome);


        return getUserAuthResponse(savedUser, token);
    }

    @Transactional
    public UserAuthResponse registerAdmin(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException("Email already taken", HttpStatus.CONFLICT);
        }
        // 1. Create User
        User newUser = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.ADMIN)
                .isEmailVerified(true) // Admins might be auto-verified or follow a different flow
                .isPhoneVerified(true)
                .build();
        // 2. Save User
        User savedUser = userRepository.save(newUser);
        // 3. Generate Token
        String token = jwtService.generateToken(savedUser.getId().toString(), savedUser.getRole().name());
        
        // 4. Publish User Created Event for Notification Service to store user details
        return getUserAuthResponse(savedUser, token);
    }

    private UserAuthResponse getUserAuthResponse(User savedUser, String token) {
        UserEvent userEvent = new UserEvent();
        userEvent.setUserId(savedUser.getId());
        userEvent.setEmail(savedUser.getEmail());
        userEvent.setName(savedUser.getName());
        userEvent.setPhone(savedUser.getPhone());
        userEvent.setEventType("USER_WELCOME");
        notificationProducer.sendUserEvent(userEvent);

        return UserAuthResponse.builder()
                .token(token)
                .userId(savedUser.getId())
                .role(savedUser.getRole().name())
                .build();
    }

    @Transactional(readOnly = true)
    public UserAuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException("Invalid Credentials", HttpStatus.UNAUTHORIZED));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException("Invalid Credentials", HttpStatus.UNAUTHORIZED);
        }
        
        Role role = user.getRole() != null ? user.getRole() : Role.USER;
        String token = jwtService.generateToken(user.getId().toString(), role.name());
        return UserAuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .role(role.name())
                .build();
    }

    @Transactional
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

        NotificationEvent forgotPwd = new NotificationEvent();
        forgotPwd.setEventId(UUID.randomUUID().toString());
        forgotPwd.setEventType("FORGOT_PASSWORD_OTP");
        forgotPwd.setChannel(ChannelType.EMAIL);
        forgotPwd.setRecipient(user.getEmail());
        forgotPwd.setPayload(params);
        forgotPwd.setOccurredAt(LocalDateTime.now());
        notificationProducer.sendNotification(forgotPwd);
    }

    // --- STEP 2: Verify OTP ---
    @Transactional
    public void verifyForgotPasswordOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        user.validateForgotPasswordOtp(otp);
        userRepository.save(user);
    }

    // --- STEP 3: Change Password ---
    @Transactional
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
    @Transactional
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

        NotificationEvent emailOtp = new NotificationEvent();
        emailOtp.setEventId(UUID.randomUUID().toString());
        emailOtp.setEventType("VERIFY_EMAIL_OTP");
        emailOtp.setChannel(ChannelType.EMAIL);
        emailOtp.setRecipient(user.getEmail());
        emailOtp.setPayload(params);
        emailOtp.setOccurredAt(LocalDateTime.now());
        notificationProducer.sendNotification(emailOtp);
    }

    // --- Complete Email Verification ---
    @Transactional
    public void verifyEmail(Long userId, String otp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        user.validateEmailOtp(otp);
        userRepository.save(user);
    }

    // --- Initiate Phone Verification ---
    @Transactional
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

        NotificationEvent phoneOtp = new NotificationEvent();
        phoneOtp.setEventId(UUID.randomUUID().toString());
        phoneOtp.setEventType("VERIFY_PHONE_OTP");
        phoneOtp.setChannel(ChannelType.SMS);
        phoneOtp.setRecipient(user.getPhone());
        phoneOtp.setPayload(params);
        phoneOtp.setOccurredAt(LocalDateTime.now());
        notificationProducer.sendNotification(phoneOtp);
    }
    // --- Complete Phone Verification ---
    @Transactional
    public void verifyPhone(Long userId, String otp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        user.validatePhoneOtp(otp);
        userRepository.save(user);
    }
}