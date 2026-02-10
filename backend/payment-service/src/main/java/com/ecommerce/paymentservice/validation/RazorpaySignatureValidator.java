package com.ecommerce.paymentservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

/**
 * Validator for Razorpay Signature format (HMAC SHA256)
 */
public class RazorpaySignatureValidator implements ConstraintValidator<ValidRazorpaySignature, String> {

    private static final int SHA256_HEX_LENGTH = 64;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(value)) {
            return true; // Let @NotBlank handle null/empty
        }

        if (value.length() != SHA256_HEX_LENGTH) {
            return false;
        }

        // Check if it's a valid hex string (lowercase)
        return value.matches("^[a-f0-9]{64}$");
    }
}
