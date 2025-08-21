package com.example.springbootrestapi.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UpdateTaskRequest validation
 */
class UpdateTaskRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldPassValidationWithValidData() {
        // Given
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Valid Title");
        request.setDescription("Valid description");
        request.setStatus(Task.TaskStatus.IN_PROGRESS);
        request.setAssigneeEmail("user@example.com");
        request.setPriority(4);

        // When
        Set<ConstraintViolation<UpdateTaskRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldFailValidationWithBlankTitle() {
        // Given
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle(""); // Blank title
        request.setDescription("Valid description");
        request.setStatus(Task.TaskStatus.IN_PROGRESS);

        // When
        Set<ConstraintViolation<UpdateTaskRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    void shouldFailValidationWithInvalidEmail() {
        // Given
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Valid Title");
        request.setDescription("Valid description");
        request.setStatus(Task.TaskStatus.IN_PROGRESS);
        request.setAssigneeEmail("invalid-email"); // Invalid email

        // When
        Set<ConstraintViolation<UpdateTaskRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("assigneeEmail") &&
                              v.getMessage().contains("Invalid email format")));
    }

    @Test
    void shouldApplyToTaskCorrectly() {
        // Given
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Updated Title");
        request.setDescription("Updated Description");
        request.setStatus(Task.TaskStatus.COMPLETED);
        request.setAssigneeEmail("updated@example.com");
        request.setPriority(5);

        Task existingTask = new Task("Original Title", "Original Description", Task.TaskStatus.PENDING);
        existingTask.setId(1L);
        existingTask.setAssigneeEmail("original@example.com");
        existingTask.setPriority(2);

        // When
        request.applyTo(existingTask);

        // Then
        assertEquals("Updated Title", existingTask.getTitle());
        assertEquals("Updated Description", existingTask.getDescription());
        assertEquals(Task.TaskStatus.COMPLETED, existingTask.getStatus());
        assertEquals("updated@example.com", existingTask.getAssigneeEmail());
        assertEquals(5, existingTask.getPriority());
    }

    @Test
    void shouldNotUpdatePriorityWhenNull() {
        // Given
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Updated Title");
        request.setDescription("Updated Description");
        request.setStatus(Task.TaskStatus.COMPLETED);
        request.setPriority(null); // Null priority

        Task existingTask = new Task("Original Title", "Original Description", Task.TaskStatus.PENDING);
        existingTask.setId(1L);
        existingTask.setPriority(2); // Original priority

        // When
        request.applyTo(existingTask);

        // Then
        assertEquals("Updated Title", existingTask.getTitle());
        assertEquals("Updated Description", existingTask.getDescription());
        assertEquals(Task.TaskStatus.COMPLETED, existingTask.getStatus());
        assertEquals(2, existingTask.getPriority()); // Should remain unchanged
    }
}