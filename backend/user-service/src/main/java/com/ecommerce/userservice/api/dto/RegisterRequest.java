package com.ecommerce.userservice.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for user registration")
public class RegisterRequest {
    
    @NotBlank(message = "Name cannot be empty")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Schema(description = "User's full name", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    @Schema(description = "User's email address", example = "john.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
             message = "Password must contain at least one uppercase letter, one lowercase letter, and one number")
    @Schema(description = "User's password (min 8 chars, must include uppercase, lowercase, and number)", 
            example = "SecurePass123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "Phone cannot be empty")
    @Pattern(regexp = "^\\+?[1-9]\\d{9,14}$", message = "Invalid phone number format")
    @Schema(description = "User's phone number", example = "+1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;
}