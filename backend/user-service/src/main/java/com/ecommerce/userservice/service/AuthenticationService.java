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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationProducerPort notificationProducer;
    private final OtpRateLimiterService otpRateLimiterService;
    
    private static final String OTP_TYPE_EMAIL = "EMAIL";
    private static final String OTP_TYPE_PHONE = "PHONE";
    private static final String OTP_TYPE_FORGOT_PASSWORD = "FORGOT_PASSWORD";

    // --- Register ---
    @Transactional
    public UserAuthResponse register(RegisterRequest request) {
        // Normalize email to lowercase for case-insensitive matching
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        
        Optional<User> existingUserOpt = userRepository.findByEmail(normalizedEmail);
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            if (existingUser.isEmailVerified()) {
                // Industry standard: Don't reveal email exists - send notification email instead
                sendAccountExistsNotification(existingUser);
                log.info("Registration attempted for existing verified email: {}", normalizedEmail);
                // Return same response as successful registration to prevent user enumeration
                return UserAuthResponse.builder()
                        .userId(0L) // Dummy ID - user won't use this since account already exists
                        .role(Role.USER.name())
                        .message("If this email is not already registered, you will receive a verification email shortly.")
                        .build();
            }
            // Resend verification if user exists but not verified
            otpRateLimiterService.checkRequestAllowed(normalizedEmail, OTP_TYPE_EMAIL);
            sendVerificationEmail(existingUser);
            // Return consistent response
            return UserAuthResponse.builder()
                    .userId(existingUser.getId())
                    .role(existingUser.getRole().name())
                    .message("If this email is not already registered, you will receive a verification email shortly.")
                    .build();
        }

        // 1. Create User with normalized email
        User newUser = User.builder()
                .name(request.getName())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.USER)
                .isEmailVerified(false)
                .isPhoneVerified(false)
                .build();

        // 2. Save User
        User savedUser = userRepository.save(newUser);

        // 3. Send Verification Email
        sendVerificationEmail(savedUser);

        // 4. Return response (No Token)
        return UserAuthResponse.builder()
                .userId(savedUser.getId())
                .role(savedUser.getRole().name())
                .message("If this email is not already registered, you will receive a verification email shortly.")
                .build();
    }

    @Transactional
    public UserAuthResponse registerAdmin(RegisterRequest request) {
        // Normalize email to lowercase
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new CustomException("Email already taken", HttpStatus.CONFLICT);
        }
        // 1. Create User with normalized email
        User newUser = User.builder()
                .name(request.getName())
                .email(normalizedEmail)
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
        
        log.info("Admin user created: {}", normalizedEmail);
        
        // 4. Publish User Created Event for Notification Service to store user details
        return getUserAuthResponse(savedUser, token);
    }

    private UserAuthResponse getUserAuthResponse(User savedUser, String token) {
        UserEvent userEvent = new UserEvent();
        userEvent.setUserId(savedUser.getId());
        userEvent.setEmail(savedUser.getEmail());
        userEvent.setName(savedUser.getName());
        userEvent.setPhone(savedUser.getPhone());
        userEvent.setEventType("USER_CREATED");
        notificationProducer.sendUserEvent(userEvent);
        log.info("Published USER_CREATED event for admin userId: {}", savedUser.getId());

        return UserAuthResponse.builder()
                .token(token)
                .userId(savedUser.getId())
                .role(savedUser.getRole().name())
                .build();
    }

    @Transactional
    public UserAuthResponse login(LoginRequest request) {
        // Normalize email for case-insensitive lookup
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new CustomException("Invalid Credentials", HttpStatus.UNAUTHORIZED));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException("Invalid Credentials", HttpStatus.UNAUTHORIZED);
        }

        if (!user.isEmailVerified()) {
            otpRateLimiterService.checkRequestAllowed(normalizedEmail, OTP_TYPE_EMAIL);
            sendVerificationEmail(user);
            // Return response with userId but no token - indicates verification needed
            return UserAuthResponse.builder()
                    .userId(user.getId())
                    .role(user.getRole().name())
                    .requiresVerification(true)
                    .message("Please verify your email. Verification code sent.")
                    .build();
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
        // Normalize email
        String normalizedEmail = email.toLowerCase().trim();
        
        // Check rate limit before processing
        otpRateLimiterService.checkRequestAllowed(normalizedEmail, OTP_TYPE_FORGOT_PASSWORD);
        
        User user = userRepository.findByEmail(normalizedEmail)
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
        
        log.debug("Forgot password OTP sent to: {}", normalizedEmail);
    }

    // --- STEP 2: Verify OTP ---
    @Transactional
    public void verifyForgotPasswordOtp(String email, String otp) {
        String normalizedEmail = email.toLowerCase().trim();
        
        // Check if locked out
        if (otpRateLimiterService.isLockedOut(normalizedEmail, OTP_TYPE_FORGOT_PASSWORD)) {
            throw new CustomException("Too many failed attempts. Please try again later.", HttpStatus.TOO_MANY_REQUESTS);
        }
        
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        try {
            user.validateForgotPasswordOtp(otp);
            userRepository.save(user);
            otpRateLimiterService.recordSuccess(normalizedEmail, OTP_TYPE_FORGOT_PASSWORD);
        } catch (CustomException e) {
            otpRateLimiterService.recordFailure(normalizedEmail, OTP_TYPE_FORGOT_PASSWORD);
            int remaining = otpRateLimiterService.getRemainingAttempts(normalizedEmail, OTP_TYPE_FORGOT_PASSWORD);
            throw new CustomException(e.getMessage() + " Remaining attempts: " + remaining, e.getStatus());
        }
    }

    // --- STEP 3: Change Password ---
    @Transactional
    public void forgotPassword(String email, String newPassword) {
        String normalizedEmail = email.toLowerCase().trim();
        
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        if (!user.isPasswordResetVerified()) {
            throw new CustomException("Verification required. Please verify OTP first.", HttpStatus.FORBIDDEN);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetVerified(false);
        userRepository.save(user);
        
        log.info("Password reset successful for: {}", normalizedEmail);
    }

    // --- Initiate Email Verification ---
    @Transactional
    public void initiateEmailVerification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        if (user.isEmailVerified()) {
            throw new CustomException("Email already verified", HttpStatus.BAD_REQUEST);
        }

        // Check rate limit
        otpRateLimiterService.checkRequestAllowed(user.getEmail(), OTP_TYPE_EMAIL);
        
        sendVerificationEmail(user);
    }

    // --- Complete Email Verification ---
    @Transactional
    public UserAuthResponse verifyEmail(Long userId, String otp) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
        
        String userEmail = user.getEmail();
        
        // Check if locked out
        if (otpRateLimiterService.isLockedOut(userEmail, OTP_TYPE_EMAIL)) {
            throw new CustomException("Too many failed attempts. Please try again later.", HttpStatus.TOO_MANY_REQUESTS);
        }
        
        try {
            user.validateEmailOtp(otp);
            userRepository.save(user);
            otpRateLimiterService.recordSuccess(userEmail, OTP_TYPE_EMAIL);
        } catch (CustomException e) {
            otpRateLimiterService.recordFailure(userEmail, OTP_TYPE_EMAIL);
            int remaining = otpRateLimiterService.getRemainingAttempts(userEmail, OTP_TYPE_EMAIL);
            throw new CustomException(e.getMessage() + " Remaining attempts: " + remaining, e.getStatus());
        }
        
        // Publish UserEvent to notification service for storing user profile
        UserEvent userEvent = new UserEvent();
        userEvent.setUserId(user.getId());
        userEvent.setEmail(user.getEmail());
        userEvent.setName(user.getName());
        userEvent.setPhone(user.getPhone());
        userEvent.setEventType("USER_CREATED");
        notificationProducer.sendUserEvent(userEvent);
        log.info("Published USER_CREATED event for userId: {}", user.getId());
        
        // Send Welcome Notification
        Map<String, String> params = new HashMap<>();
        params.put("name", user.getName());

        String token = jwtService.generateToken(user.getId().toString(), user.getRole().name());
        NotificationEvent welcome = new NotificationEvent();
        welcome.setEventId(UUID.randomUUID().toString());
        welcome.setEventType("USER_WELCOME");
        welcome.setChannel(ChannelType.EMAIL);
        welcome.setRecipient(user.getEmail());
        welcome.setPayload(params);
        welcome.setOccurredAt(LocalDateTime.now());
        notificationProducer.sendNotification(welcome);

        return UserAuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .role(user.getRole().name())
                .build();
    }

    // --- Initiate Phone Verification ---
    @Transactional
    public void initiatePhoneVerification(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        if (user.isPhoneVerified()) {
            throw new CustomException("Phone already verified", HttpStatus.BAD_REQUEST);
        }

        // Check rate limit for phone
        otpRateLimiterService.checkRequestAllowed(user.getPhone(), OTP_TYPE_PHONE);

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

        String userPhone = user.getPhone();
        
        // Check if locked out
        if (otpRateLimiterService.isLockedOut(userPhone, OTP_TYPE_PHONE)) {
            throw new CustomException("Too many failed attempts. Please try again later.", HttpStatus.TOO_MANY_REQUESTS);
        }
        
        try {
            user.validatePhoneOtp(otp);
            userRepository.save(user);
            otpRateLimiterService.recordSuccess(userPhone, OTP_TYPE_PHONE);
        } catch (CustomException e) {
            otpRateLimiterService.recordFailure(userPhone, OTP_TYPE_PHONE);
            int remaining = otpRateLimiterService.getRemainingAttempts(userPhone, OTP_TYPE_PHONE);
            throw new CustomException(e.getMessage() + " Remaining attempts: " + remaining, e.getStatus());
        }
    }

    private void sendVerificationEmail(User user) {
        user.generateEmailOtp();
        userRepository.save(user);

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

    /**
     * Send notification to existing user that someone attempted to register with their email.
     * This prevents user enumeration attacks while still informing the actual account owner.
     */
    private void sendAccountExistsNotification(User user) {
        Map<String, String> params = new HashMap<>();
        params.put("name", user.getName());

        NotificationEvent notification = new NotificationEvent();
        notification.setEventId(UUID.randomUUID().toString());
        notification.setEventType("ACCOUNT_EXISTS_NOTIFICATION");
        notification.setChannel(ChannelType.EMAIL);
        notification.setRecipient(user.getEmail());
        notification.setPayload(params);
        notification.setOccurredAt(LocalDateTime.now());
        notificationProducer.sendNotification(notification);
    }
}