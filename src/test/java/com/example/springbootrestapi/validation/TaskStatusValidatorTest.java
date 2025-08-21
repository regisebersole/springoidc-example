package com.example.springbootrestapi.validation;

import com.example.springbootrestapi.model.Task;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TaskStatusValidator
 */
class TaskStatusValidatorTest {

    private TaskStatusValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new TaskStatusValidator();
        validator.initialize(null);
    }

    @Test
    void shouldReturnTrueForNullTask() {
        // When & Then
        assertTrue(validator.isValid(null, context));
    }

    @Test
    void shouldReturnTrueForTaskWithNullStatus() {
        // Given
        Task task = new Task();
        task.setStatus(null);

        // When & Then
        assertTrue(validator.isValid(task, context));
    }

    @Test
    void shouldReturnTrueForNewTaskWithAnyStatus() {
        // Given - new task (no ID)
        Task task = new Task("Test Task", "Description", Task.TaskStatus.PENDING);

        // When & Then
        assertTrue(validator.isValid(task, context));
    }

    @Test
    void shouldReturnTrueForValidPendingStatus() {
        // Given
        Task task = new Task("Test Task", "Description", Task.TaskStatus.PENDING);
        task.setId(1L);

        // When & Then
        assertTrue(validator.isValid(task, context));
    }

    @Test
    void shouldReturnTrueForValidInProgressStatus() {
        // Given
        Task task = new Task("Test Task", "Description", Task.TaskStatus.IN_PROGRESS);
        task.setId(1L);

        // When & Then
        assertTrue(validator.isValid(task, context));
    }

    @Test
    void shouldReturnTrueForValidCompletedStatus() {
        // Given
        Task task = new Task("Test Task", "Description", Task.TaskStatus.COMPLETED);
        task.setId(1L);

        // When & Then
        assertTrue(validator.isValid(task, context));
    }

    @Test
    void shouldReturnTrueForValidCancelledStatus() {
        // Given
        Task task = new Task("Test Task", "Description", Task.TaskStatus.CANCELLED);
        task.setId(1L);

        // When & Then
        assertTrue(validator.isValid(task, context));
    }
}