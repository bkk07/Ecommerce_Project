package com.ecommerce.paymentservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

/**
 * Validator for payment amount
 */
public class PaymentAmountValidator implements ConstraintValidator<ValidPaymentAmount, BigDecimal> {

    private double minAmount;
    private double maxAmount;

    @Override
    public void initialize(ValidPaymentAmount constraintAnnotation) {
        this.minAmount = constraintAnnotation.min();
        this.maxAmount = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null
        }

        double amount = value.doubleValue();
        
        if (amount < minAmount || amount > maxAmount) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("Amount must be between ₹%.2f and ₹%.2f. Provided: ₹%.2f", 
                            minAmount, maxAmount, amount)
            ).addConstraintViolation();
            return false;
        }

        // Check for too many decimal places (max 2)
        if (value.scale() > 2) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Amount cannot have more than 2 decimal places"
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}
