package com.ecommerce.userservice.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAuthResponse {
    private String token;
    private String refreshToken;
    private Long expiresIn; // Access token expiry in milliseconds
    private Long userId;
    private String role;
    private Boolean requiresVerification;
    private String message;
}