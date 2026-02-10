package com.ecommerce.paymentservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for Razorpay Order ID format
 */
@Documented
@Constraint(validatedBy = RazorpayOrderIdValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRazorpayOrderId {
    String message() default "Invalid Razorpay Order ID format. Must start with 'order_' followed by alphanumeric characters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
