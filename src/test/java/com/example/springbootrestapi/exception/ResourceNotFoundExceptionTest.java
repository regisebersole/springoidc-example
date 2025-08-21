package com.example.springbootrestapi.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceNotFoundExceptionTest {

    @Test
    void shouldCreateExceptionWithMessage() {
        // Given
        String message = "Resource not found";

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(message);

        // Then
        assertEquals(message, exception.getMessage());
        assertNull(exception.getResourceType());
        assertNull(exception.getResourceId());
    }

    @Test
    void shouldCreateExceptionWithResourceTypeAndId() {
        // Given
        String resourceType = "Task";
        String resourceId = "123";

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(resourceType, resourceId);

        // Then
        assertEquals("Task with id '123' not found", exception.getMessage());
        assertEquals(resourceType, exception.getResourceType());
        assertEquals(resourceId, exception.getResourceId());
    }

    @Test
    void shouldCreateExceptionWithResourceTypeIdAndCustomMessage() {
        // Given
        String resourceType = "User";
        String resourceId = "456";
        String customMessage = "User account has been deleted";

        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(resourceType, resourceId, customMessage);

        // Then
        assertEquals(customMessage, exception.getMessage());
        assertEquals(resourceType, exception.getResourceType());
        assertEquals(resourceId, exception.getResourceId());
    }
}