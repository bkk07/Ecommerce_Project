package com.ecommerce.userservice.api.dto;
import com.ecommerce.userservice.domain.model.enums.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AddressRequest {

    @NotBlank(message = "Street cannot be empty")
    private String street;

    @NotBlank(message = "City cannot be empty")
    private String city;

    @NotBlank(message = "State cannot be empty")
    private String state;

    @NotBlank(message = "ZipCode cannot be empty")
    @Pattern(regexp = "^\\d{5,6}$", message = "ZipCode must be 5 or 6 digits")
    private String zipCode;

    @NotBlank(message = "Country cannot be empty")
    private String country;

    @NotNull(message = "Address Type is required (SHIPPING or BILLING)")
    private AddressType type;
}