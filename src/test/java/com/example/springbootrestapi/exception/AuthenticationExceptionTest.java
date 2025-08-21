package com.example.springbootrestapi.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        // Given
        String message = "Authentication failed";

        // When
        AuthenticationException exception = new AuthenticationException(message);

        // Then
        assertEquals(message, exception.getMessage());
        assertNull(exception.getReason());
    }

    @Test
    void shouldCreateExceptionWithMessageAndReason() {
        // Given
        String message = "Token validation failed";
        String reason = "TOKEN_EXPIRED";

        // When
        AuthenticationException exception = new AuthenticationException(message, reason);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(reason, exception.getReason());
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        // Given
        String message = "Authentication error";
        Throwable cause = new RuntimeException("Network error");

        // When
        AuthenticationException exception = new AuthenticationException(message, cause);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertNull(exception.getReason());
    }

    @Test
    void shouldCreateExceptionWithMessageReasonAndCause() {
        // Given
        String message = "Token validation failed";
        String reason = "INVALID_SIGNATURE";
        Throwable cause = new RuntimeException("JWT signature verification failed");

        // When
        AuthenticationException exception = new AuthenticationException(message, reason, cause);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(reason, exception.getReason());
        assertEquals(cause, exception.getCause());
    }
}