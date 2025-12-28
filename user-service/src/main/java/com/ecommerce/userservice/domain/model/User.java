package com.ecommerce.userservice.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
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
    public boolean canResetPassword() {
        return isEmailVerified || isPhoneVerified;
    }
}