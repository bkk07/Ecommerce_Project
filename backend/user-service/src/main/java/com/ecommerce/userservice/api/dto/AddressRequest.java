package com.ecommerce.userservice.api.dto;

import com.ecommerce.userservice.domain.model.enums.AddressType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Request object for adding/updating user address")
public class AddressRequest {

    @NotBlank(message = "Street cannot be empty")
    @Size(max = 255, message = "Street cannot exceed 255 characters")
    @Schema(description = "Street address", example = "123 Main Street", requiredMode = Schema.RequiredMode.REQUIRED)
    private String street;

    @NotBlank(message = "City cannot be empty")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    @Schema(description = "City name", example = "New York", requiredMode = Schema.RequiredMode.REQUIRED)
    private String city;

    @NotBlank(message = "State cannot be empty")
    @Size(max = 100, message = "State cannot exceed 100 characters")
    @Schema(description = "State/Province name", example = "NY", requiredMode = Schema.RequiredMode.REQUIRED)
    private String state;

    @NotBlank(message = "ZipCode cannot be empty")
    @Pattern(regexp = "^\\d{5,6}$", message = "ZipCode must be 5 or 6 digits")
    @Schema(description = "Postal/ZIP code", example = "10001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String zipCode;

    @NotBlank(message = "Country cannot be empty")
    @Size(max = 100, message = "Country cannot exceed 100 characters")
    @Schema(description = "Country name", example = "USA", requiredMode = Schema.RequiredMode.REQUIRED)
    private String country;

    @NotNull(message = "Address Type is required (SHIPPING or BILLING)")
    @Schema(description = "Type of address", example = "SHIPPING", requiredMode = Schema.RequiredMode.REQUIRED)
    private AddressType type;
}