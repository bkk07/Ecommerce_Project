package com.ecommerce.paymentservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

/**
 * Validator for Razorpay Payment ID format
 */
public class RazorpayPaymentIdValidator implements ConstraintValidator<ValidRazorpayPaymentId, String> {

    private static final String RAZORPAY_PAYMENT_PREFIX = "pay_";
    private static final int MIN_LENGTH = 18; // pay_ (4) + at least 14 chars
    private static final int MAX_LENGTH = 50;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(value)) {
            return true; // Let @NotBlank handle null/empty
        }

        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            return false;
        }

        if (!value.startsWith(RAZORPAY_PAYMENT_PREFIX)) {
            return false;
        }

        // Validate the ID part after prefix
        String idPart = value.substring(RAZORPAY_PAYMENT_PREFIX.length());
        return idPart.matches("^[a-zA-Z0-9]+$");
    }
}
