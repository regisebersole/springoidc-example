package com.example.springbootrestapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for business email addresses
 * Ensures email follows business rules (e.g., allowed domains)
 */
@Documented
@Constraint(validatedBy = BusinessEmailValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBusinessEmail {
    String message() default "Email must be from an allowed business domain";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Allow null values (for optional fields)
     */
    boolean allowNull() default true;
    
    /**
     * Allowed domains for business emails
     */
    String[] allowedDomains() default {"company.com", "example.com", "gmail.com", "outlook.com"};
}