package com.example.springbootrestapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for Task status transitions
 */
@Documented
@Constraint(validatedBy = TaskStatusValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTaskStatus {
    String message() default "Invalid task status transition";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}