package com.ecommerce.userservice.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "name cannot be empty")
    private String name;

    @NotBlank(message = "email cannot be empty")
    private String email;

    @NotBlank(message = "password Cannot be empty")
    private String password;

    @NotBlank(message = "phone cannot be empty")
    private String phone;
}