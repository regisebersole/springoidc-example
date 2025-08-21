package com.example.springbootrestapi.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        // Given
        String message = "Access denied";

        // When
        AuthorizationException exception = new AuthorizationException(message);

        // Then
        assertEquals(message, exception.getMessage());
        assertNull(exception.getRequiredPermission());
        assertNull(exception.getUserId());
    }

    @Test
    void shouldCreateExceptionWithMessageAndRequiredPermission() {
        // Given
        String message = "Insufficient permissions";
        String requiredPermission = "ADMIN_ROLE";

        // When
        AuthorizationException exception = new AuthorizationException(message, requiredPermission);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(requiredPermission, exception.getRequiredPermission());
        assertNull(exception.getUserId());
    }

    @Test
    void shouldCreateExceptionWithMessagePermissionAndUserId() {
        // Given
        String message = "User lacks required permission";
        String requiredPermission = "WRITE_ACCESS";
        String userId = "user123";

        // When
        AuthorizationException exception = new AuthorizationException(message, requiredPermission, userId);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(requiredPermission, exception.getRequiredPermission());
        assertEquals(userId, exception.getUserId());
    }

    @Test
    void shouldCreateExceptionWithMessageAndCause() {
        // Given
        String message = "Authorization check failed";
        Throwable cause = new RuntimeException("Database connection error");

        // When
        AuthorizationException exception = new AuthorizationException(message, cause);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertNull(exception.getRequiredPermission());
        assertNull(exception.getUserId());
    }
}