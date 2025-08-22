package com.example.springbootrestapi.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class PriorityValidatorTest {

    @Mock
    private ValidPriority validPriority;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    private PriorityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PriorityValidator();
        lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        lenient().when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    void testValidPriority() {
        // Given
        when(validPriority.allowNull()).thenReturn(true);
        validator.initialize(validPriority);

        // When & Then
        assertTrue(validator.isValid(1, context));
        assertTrue(validator.isValid(3, context));
        assertTrue(validator.isValid(5, context));
        verifyNoInteractions(context);
    }

    @Test
    void testInvalidPriorityTooLow() {
        // Given
        when(validPriority.allowNull()).thenReturn(true);
        validator.initialize(validPriority);

        // When
        boolean result = validator.isValid(0, context);

        // Then
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Priority must be between 1 and 5, but was 0");
    }

    @Test
    void testInvalidPriorityTooHigh() {
        // Given
        when(validPriority.allowNull()).thenReturn(true);
        validator.initialize(validPriority);

        // When
        boolean result = validator.isValid(6, context);

        // Then
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Priority must be between 1 and 5, but was 6");
    }

    @Test
    void testNullPriorityWhenAllowed() {
        // Given
        when(validPriority.allowNull()).thenReturn(true);
        validator.initialize(validPriority);

        // When & Then
        assertTrue(validator.isValid(null, context));
        verifyNoInteractions(context);
    }

    @Test
    void testNullPriorityWhenNotAllowed() {
        // Given
        when(validPriority.allowNull()).thenReturn(false);
        validator.initialize(validPriority);

        // When & Then
        assertFalse(validator.isValid(null, context));
        verifyNoInteractions(context);
    }

    @Test
    void testBoundaryValues() {
        // Given
        when(validPriority.allowNull()).thenReturn(true);
        validator.initialize(validPriority);

        // When & Then
        assertTrue(validator.isValid(1, context)); // Lower boundary
        assertTrue(validator.isValid(5, context)); // Upper boundary
        
        assertFalse(validator.isValid(0, context)); // Below lower boundary
        assertFalse(validator.isValid(6, context)); // Above upper boundary
    }
}