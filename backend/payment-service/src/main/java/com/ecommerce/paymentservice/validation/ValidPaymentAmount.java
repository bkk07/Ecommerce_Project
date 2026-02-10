package com.ecommerce.paymentservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation for valid payment amount (positive, within range)
 */
@Documented
@Constraint(validatedBy = PaymentAmountValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPaymentAmount {
    String message() default "Payment amount must be between ₹1.00 and ₹1,00,00,000.00";
    double min() default 1.0;
    double max() default 10000000.0;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
