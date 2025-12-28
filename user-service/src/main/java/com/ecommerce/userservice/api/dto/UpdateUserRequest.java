package com.ecommerce.userservice.api.dto;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class UpdateUserRequest {
    private String name;
    private String phone;
}

