package com.ecommerce.paymentservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for Razorpay Payment ID format
 */
@Documented
@Constraint(validatedBy = RazorpayPaymentIdValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRazorpayPaymentId {
    String message() default "Invalid Razorpay Payment ID format. Must start with 'pay_' followed by alphanumeric characters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
