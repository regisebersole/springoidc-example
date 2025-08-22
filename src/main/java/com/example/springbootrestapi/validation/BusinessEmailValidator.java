package com.example.springbootrestapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator for business email addresses
 * Implements business rules for allowed email domains
 */
public class BusinessEmailValidator implements ConstraintValidator<ValidBusinessEmail, String> {

    private boolean allowNull;
    private Set<String> allowedDomains;

    @Override
    public void initialize(ValidBusinessEmail constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
        this.allowedDomains = Arrays.stream(constraintAnnotation.allowedDomains())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        // Handle null values based on configuration
        if (email == null || email.trim().isEmpty()) {
            return allowNull;
        }

        // Basic email format validation
        if (!email.contains("@") || !email.contains(".")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Email format is invalid"
            ).addConstraintViolation();
            return false;
        }

        // Extract domain from email
        String[] parts = email.toLowerCase().split("@");
        if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Email format is invalid"
            ).addConstraintViolation();
            return false;
        }

        String domain = parts[1];

        // Check if domain contains a dot and has valid format
        if (!domain.contains(".") || domain.startsWith(".") || domain.endsWith(".")) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Email format is invalid"
            ).addConstraintViolation();
            return false;
        }

        // Check if domain is in allowed list
        if (!allowedDomains.contains(domain)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Email domain '" + domain + "' is not allowed. Allowed domains: " + 
                String.join(", ", allowedDomains)
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}