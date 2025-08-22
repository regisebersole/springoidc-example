package com.example.springbootrestapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for task priority values
 * Ensures priority is within valid business range and follows business rules
 */
@Documented
@Constraint(validatedBy = PriorityValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPriority {
    String message() default "Priority must be between 1 and 5";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Allow null values (for optional fields)
     */
    boolean allowNull() default true;
}