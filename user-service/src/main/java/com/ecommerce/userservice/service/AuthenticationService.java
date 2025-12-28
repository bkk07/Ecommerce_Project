package com.ecommerce.userservice.service;

import com.ecommerce.userservice.api.dto.LoginRequest;
import com.ecommerce.userservice.api.dto.RegisterRequest;
import com.ecommerce.userservice.api.dto.UserAuthResponse;
import com.ecommerce.userservice.domain.model.User;
import com.ecommerce.userservice.domain.port.UserRepositoryPort;
import com.ecommerce.userservice.exception.CustomException; // Assuming this exists based on your code
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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
        // Assuming default role is "USER" as per your previous code
        String token = jwtService.generateToken(savedUser.getId().toString(), "USER");

        // 4. Return Response
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

        // Generate Token
        String token = jwtService.generateToken(user.getId().toString(), "USER");

        return UserAuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .role("USER") // Or user.getRole() if you add that field to domain model
                .build();
    }

    public void forgotPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        if (!user.canResetPassword()) {
            throw new CustomException("Cannot reset password: User must verify Email or Phone first.", HttpStatus.FORBIDDEN);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void verifyEmail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        user.setEmailVerified(true);
        userRepository.save(user);
    }

    public void verifyPhone(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        user.setPhoneVerified(true);
        userRepository.save(user);
    }
}