package com.example.springbootrestapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for task priority values
 * Implements business rules for valid priority ranges
 */
public class PriorityValidator implements ConstraintValidator<ValidPriority, Integer> {

    private boolean allowNull;

    @Override
    public void initialize(ValidPriority constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(Integer priority, ConstraintValidatorContext context) {
        // Handle null values based on configuration
        if (priority == null) {
            return allowNull;
        }

        // Business rule: Priority must be between 1 and 5 inclusive
        if (priority < 1 || priority > 5) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Priority must be between 1 and 5, but was " + priority
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}