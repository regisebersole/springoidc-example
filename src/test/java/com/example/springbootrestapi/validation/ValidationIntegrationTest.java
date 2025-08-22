package com.example.springbootrestapi.validation;

import com.example.springbootrestapi.model.CreateTaskRequest;
import com.example.springbootrestapi.model.Task;
import com.example.springbootrestapi.model.UpdateTaskRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ValidationIntegrationTest {

    @Autowired
    private Validator validator;

    @Test
    void testCreateTaskRequestValidation() {
        // Given - valid request
        CreateTaskRequest validRequest = new CreateTaskRequest();
        validRequest.setTitle("Valid Task Title");
        validRequest.setDescription("Valid description");
        validRequest.setStatus(Task.TaskStatus.PENDING);
        validRequest.setAssigneeEmail("user@company.com");
        validRequest.setPriority(3);

        // When
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(validRequest);

        // Then
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    void testCreateTaskRequestValidationErrors() {
        // Given - invalid request
        CreateTaskRequest invalidRequest = new CreateTaskRequest();
        invalidRequest.setTitle(""); // Blank title
        invalidRequest.setDescription("A".repeat(501)); // Too long description
        invalidRequest.setStatus(null); // Null status
        invalidRequest.setAssigneeEmail("invalid@forbidden.com"); // Invalid domain
        invalidRequest.setPriority(10); // Invalid priority

        // When
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(invalidRequest);

        // Then
        assertFalse(violations.isEmpty(), "Invalid request should have violations");
        assertEquals(7, violations.size(), "Should have 7 validation errors");
        
        // Verify specific error messages exist
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Title is required")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Title contains invalid characters")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Title must be between 3 and 100 characters")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Description must not exceed 500 characters")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Status is required")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("forbidden.com")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Priority must be between 1 and 5")));
    }

    @Test
    void testUpdateTaskRequestValidation() {
        // Given - valid request
        UpdateTaskRequest validRequest = new UpdateTaskRequest();
        validRequest.setTitle("Updated Task Title");
        validRequest.setDescription("Updated description");
        validRequest.setStatus(Task.TaskStatus.IN_PROGRESS);
        validRequest.setAssigneeEmail("admin@example.com");
        validRequest.setPriority(4);

        // When
        Set<ConstraintViolation<UpdateTaskRequest>> violations = validator.validate(validRequest);

        // Then
        assertTrue(violations.isEmpty(), "Valid request should have no violations");
    }

    @Test
    void testTaskModelValidation() {
        // Given - valid task
        Task validTask = new Task();
        validTask.setTitle("Valid Task");
        validTask.setDescription("Valid description");
        validTask.setStatus(Task.TaskStatus.PENDING);
        validTask.setAssigneeEmail("user@gmail.com");
        validTask.setPriority(2);

        // When
        Set<ConstraintViolation<Task>> violations = validator.validate(validTask);

        // Then
        assertTrue(violations.isEmpty(), "Valid task should have no violations");
    }

    @Test
    void testTitlePatternValidation() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Invalid<script>alert('xss')</script>"); // Contains invalid characters
        request.setStatus(Task.TaskStatus.PENDING);

        // When
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> 
            v.getMessage().contains("Title contains invalid characters")));
    }

    @Test
    void testValidTitlePatterns() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setStatus(Task.TaskStatus.PENDING);

        // Test valid title patterns
        String[] validTitles = {
            "Simple Task",
            "Task-123",
            "Task_with_underscores",
            "Task with numbers 123",
            "Task, with punctuation!",
            "Task (with parentheses)",
            "Task with question?"
        };

        for (String title : validTitles) {
            request.setTitle(title);
            Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);
            
            boolean hasTitleError = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("title"));
            assertFalse(hasTitleError, "Title '" + title + "' should be valid");
        }
    }

    @Test
    void testEmailDomainValidation() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Test Task");
        request.setStatus(Task.TaskStatus.PENDING);

        // Test valid email domains
        String[] validEmails = {
            "user@company.com",
            "admin@example.com",
            "test@gmail.com",
            "support@outlook.com"
        };

        for (String email : validEmails) {
            request.setAssigneeEmail(email);
            Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);
            
            boolean hasEmailError = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("assigneeEmail"));
            assertFalse(hasEmailError, "Email '" + email + "' should be valid");
        }
    }

    @Test
    void testPriorityBoundaryValidation() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Test Task");
        request.setStatus(Task.TaskStatus.PENDING);

        // Test valid priorities
        Integer[] validPriorities = {1, 2, 3, 4, 5};
        for (Integer priority : validPriorities) {
            request.setPriority(priority);
            Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);
            
            boolean hasPriorityError = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("priority"));
            assertFalse(hasPriorityError, "Priority " + priority + " should be valid");
        }

        // Test invalid priorities
        Integer[] invalidPriorities = {0, 6, -1, 100};
        for (Integer priority : invalidPriorities) {
            request.setPriority(priority);
            Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);
            
            boolean hasPriorityError = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("priority"));
            assertTrue(hasPriorityError, "Priority " + priority + " should be invalid");
        }
    }

    @Test
    void testNullOptionalFields() {
        // Given - request with only required fields
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Minimal Task");
        request.setStatus(Task.TaskStatus.PENDING);
        // description, assigneeEmail, and priority are null

        // When
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        // Then
        assertTrue(violations.isEmpty(), "Request with null optional fields should be valid");
    }

    @Test
    void testEmptyStringValidation() {
        // Given
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("   "); // Whitespace only
        request.setDescription("");
        request.setStatus(Task.TaskStatus.PENDING);
        request.setAssigneeEmail(""); // Empty email

        // When
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("title") && 
            v.getMessage().contains("Title is required")));
    }

    @Test
    void testComplexValidationScenario() {
        // Given - request with multiple validation errors
        CreateTaskRequest complexRequest = new CreateTaskRequest();
        complexRequest.setTitle(""); // Blank title
        complexRequest.setDescription("A".repeat(501)); // Too long description
        complexRequest.setStatus(null); // Null status
        complexRequest.setAssigneeEmail("invalid@forbidden.com"); // Invalid domain
        complexRequest.setPriority(10); // Invalid priority

        // When
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(complexRequest);

        // Then
        assertFalse(violations.isEmpty(), "Complex invalid request should have multiple violations");
        
        // Verify we have violations for each field
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("status")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("assigneeEmail")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("priority")));
    }
}