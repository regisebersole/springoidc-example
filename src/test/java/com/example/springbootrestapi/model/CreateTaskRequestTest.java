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
 * Unit tests for CreateTaskRequest validation
 */
class CreateTaskRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldPassValidationWithValidData() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Valid Title");
        request.setDescription("Valid description");
        request.setStatus(Task.TaskStatus.PENDING);
        request.setAssigneeEmail("user@example.com");
        request.setPriority(3);

        // When
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldFailValidationWithBlankTitle() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle(""); // Blank title
        request.setDescription("Valid description");
        request.setStatus(Task.TaskStatus.PENDING);

        // When
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    void shouldFailValidationWithTitleTooShort() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("AB"); // Too short
        request.setDescription("Valid description");
        request.setStatus(Task.TaskStatus.PENDING);

        // When
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("title") &&
                              v.getMessage().contains("between 3 and 100 characters")));
    }

    @Test
    void shouldFailValidationWithTitleTooLong() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("A".repeat(101)); // Too long
        request.setDescription("Valid description");
        request.setStatus(Task.TaskStatus.PENDING);

        // When
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("title") &&
                              v.getMessage().contains("between 3 and 100 characters")));
    }

    @Test
    void shouldFailValidationWithDescriptionTooLong() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Valid Title");
        request.setDescription("A".repeat(501)); // Too long
        request.setStatus(Task.TaskStatus.PENDING);

        // When
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("description") &&
                              v.getMessage().contains("must not exceed 500 characters")));
    }

    @Test
    void shouldFailValidationWithNullStatus() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Valid Title");
        request.setDescription("Valid description");
        request.setStatus(null); // Null status

        // When
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("status") &&
                              v.getMessage().contains("Status is required")));
    }

    @Test
    void shouldFailValidationWithInvalidEmail() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Valid Title");
        request.setDescription("Valid description");
        request.setStatus(Task.TaskStatus.PENDING);
        request.setAssigneeEmail("invalid-email"); // Invalid email

        // When
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("assigneeEmail") &&
                              v.getMessage().contains("Invalid email format")));
    }

    @Test
    void shouldFailValidationWithPriorityTooLow() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Valid Title");
        request.setDescription("Valid description");
        request.setStatus(Task.TaskStatus.PENDING);
        request.setPriority(0); // Too low

        // When
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("priority") &&
                              v.getMessage().contains("Priority must be at least 1")));
    }

    @Test
    void shouldFailValidationWithPriorityTooHigh() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Valid Title");
        request.setDescription("Valid description");
        request.setStatus(Task.TaskStatus.PENDING);
        request.setPriority(6); // Too high

        // When
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("priority") &&
                              v.getMessage().contains("Priority must not exceed 5")));
    }

    @Test
    void shouldConvertToTaskCorrectly() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Test Task");
        request.setDescription("Test Description");
        request.setStatus(Task.TaskStatus.IN_PROGRESS);
        request.setAssigneeEmail("test@example.com");
        request.setPriority(4);

        // When
        Task task = request.toTask();

        // Then
        assertEquals("Test Task", task.getTitle());
        assertEquals("Test Description", task.getDescription());
        assertEquals(Task.TaskStatus.IN_PROGRESS, task.getStatus());
        assertEquals("test@example.com", task.getAssigneeEmail());
        assertEquals(4, task.getPriority());
    }

    @Test
    void shouldSetDefaultPriorityWhenNull() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Test Task");
        request.setDescription("Test Description");
        request.setStatus(Task.TaskStatus.PENDING);
        request.setPriority(null);

        // When
        Task task = request.toTask();

        // Then
        assertEquals(3, task.getPriority()); // Default priority
    }
}