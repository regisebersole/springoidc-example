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
import static org.mockito.Mockito.atLeast;

@ExtendWith(MockitoExtension.class)
class BusinessEmailValidatorTest {

    @Mock
    private ValidBusinessEmail validBusinessEmail;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    private BusinessEmailValidator validator;

    @BeforeEach
    void setUp() {
        validator = new BusinessEmailValidator();
        lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
        lenient().when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    void testValidBusinessEmails() {
        // Given
        when(validBusinessEmail.allowNull()).thenReturn(true);
        when(validBusinessEmail.allowedDomains()).thenReturn(new String[]{"company.com", "example.com", "gmail.com"});
        validator.initialize(validBusinessEmail);

        // When & Then
        assertTrue(validator.isValid("user@company.com", context));
        assertTrue(validator.isValid("test@example.com", context));
        assertTrue(validator.isValid("admin@gmail.com", context));
        assertTrue(validator.isValid("USER@COMPANY.COM", context)); // Case insensitive
        verifyNoInteractions(context);
    }

    @Test
    void testInvalidEmailDomain() {
        // Given
        when(validBusinessEmail.allowNull()).thenReturn(true);
        when(validBusinessEmail.allowedDomains()).thenReturn(new String[]{"company.com", "example.com"});
        validator.initialize(validBusinessEmail);

        // When
        boolean result = validator.isValid("user@invalid.com", context);

        // Then
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(
            "Email domain 'invalid.com' is not allowed. Allowed domains: company.com, example.com"
        );
    }

    @Test
    void testInvalidEmailFormat() {
        // Given
        when(validBusinessEmail.allowNull()).thenReturn(true);
        when(validBusinessEmail.allowedDomains()).thenReturn(new String[]{"company.com"});
        validator.initialize(validBusinessEmail);

        // When & Then
        assertFalse(validator.isValid("invalid-email", context));
        assertFalse(validator.isValid("user@", context));
        assertFalse(validator.isValid("@company.com", context));
        assertFalse(validator.isValid("user@@company.com", context));
        
        verify(context, atLeast(4)).disableDefaultConstraintViolation();
        verify(context, atLeast(4)).buildConstraintViolationWithTemplate("Email format is invalid");
    }

    @Test
    void testNullEmailWhenAllowed() {
        // Given
        when(validBusinessEmail.allowNull()).thenReturn(true);
        when(validBusinessEmail.allowedDomains()).thenReturn(new String[]{"company.com"});
        validator.initialize(validBusinessEmail);

        // When & Then
        assertTrue(validator.isValid(null, context));
        assertTrue(validator.isValid("", context));
        assertTrue(validator.isValid("   ", context));
        verifyNoInteractions(context);
    }

    @Test
    void testNullEmailWhenNotAllowed() {
        // Given
        when(validBusinessEmail.allowNull()).thenReturn(false);
        when(validBusinessEmail.allowedDomains()).thenReturn(new String[]{"company.com"});
        validator.initialize(validBusinessEmail);

        // When & Then
        assertFalse(validator.isValid(null, context));
        assertFalse(validator.isValid("", context));
        assertFalse(validator.isValid("   ", context));
        verifyNoInteractions(context);
    }

    @Test
    void testCaseInsensitiveDomainMatching() {
        // Given
        when(validBusinessEmail.allowNull()).thenReturn(true);
        when(validBusinessEmail.allowedDomains()).thenReturn(new String[]{"Company.Com", "EXAMPLE.COM"});
        validator.initialize(validBusinessEmail);

        // When & Then
        assertTrue(validator.isValid("user@company.com", context));
        assertTrue(validator.isValid("user@COMPANY.COM", context));
        assertTrue(validator.isValid("user@example.com", context));
        assertTrue(validator.isValid("user@EXAMPLE.COM", context));
        verifyNoInteractions(context);
    }

    @Test
    void testMultipleAtSymbols() {
        // Given
        when(validBusinessEmail.allowNull()).thenReturn(true);
        when(validBusinessEmail.allowedDomains()).thenReturn(new String[]{"company.com"});
        validator.initialize(validBusinessEmail);

        // When
        boolean result = validator.isValid("user@test@company.com", context);

        // Then
        assertFalse(result);
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("Email format is invalid");
    }
}