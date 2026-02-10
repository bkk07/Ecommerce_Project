package com.ecommerce.paymentservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for Razorpay signature format
 */
@Documented
@Constraint(validatedBy = RazorpaySignatureValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRazorpaySignature {
    String message() default "Invalid Razorpay signature format. Must be 64 hexadecimal characters (SHA256)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
