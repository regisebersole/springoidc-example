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

/**
 * Comprehensive validation test to verify all requirements 6.1-6.5 are met
 */
@SpringBootTest
class ComprehensiveValidationTest {

    @Autowired
    private Validator validator;

    /**
     * Requirement 6.1: WHEN request data is received THEN the system SHALL validate required fields are present
     */
    @Test
    void testRequiredFieldsValidation() {
        // Given - request with missing required fields
        CreateTaskRequest request = new CreateTaskRequest();
        // title is null (required)
        // status is null (required)

        // When
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty(), "Should have violations for missing required fields");
        
        // Verify title is required
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("title") && 
            v.getMessage().contains("Title is required")), 
            "Should validate title is required");
            
        // Verify status is required
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("status") && 
            v.getMessage().contains("Status is required")), 
            "Should validate status is required");
    }

    /**
     * Requirement 6.2: WHEN data types are incorrect THEN the system SHALL return validation errors with field names
     */
    @Test
    void testDataTypeValidation() {
        // Given - request with invalid data types (handled by custom validators)
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Valid Title");
        request.setStatus(Task.TaskStatus.PENDING);
        request.setPriority(999); // Invalid priority value (should be 1-5)

        // When
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty(), "Should have violations for invalid data types");
        
        // Verify priority validation with field name
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("priority") && 
            v.getMessage().contains("Priority must be between 1 and 5")), 
            "Should validate priority data type with field name");
    }

    /**
     * Requirement 6.3: WHEN string length limits are exceeded THEN the system SHALL return appropriate validation messages
     */
    @Test
    void testStringLengthValidation() {
        // Given - request with strings exceeding length limits
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("AB"); // Too short (min 3)
        request.setDescription("A".repeat(501)); // Too long (max 500)
        request.setStatus(Task.TaskStatus.PENDING);

        // When
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty(), "Should have violations for string length limits");
        
        // Verify title length validation
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("title") && 
            v.getMessage().contains("between 3 and 100 characters")), 
            "Should validate title minimum length");
            
        // Verify description length validation
        assertTrue(violations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("description") && 
            v.getMessage().contains("must not exceed 500 characters")), 
            "Should validate description maximum length");

        // Test maximum title length
        CreateTaskRequest longTitleRequest = new CreateTaskRequest();
        longTitleRequest.setTitle("A".repeat(101)); // Too long (max 100)
        longTitleRequest.setStatus(Task.TaskStatus.PENDING);

        Set<ConstraintViolation<CreateTaskRequest>> longTitleViolations = validator.validate(longTitleRequest);
        assertTrue(longTitleViolations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("title") && 
            v.getMessage().contains("between 3 and 100 characters")), 
            "Should validate title maximum length");
    }

    /**
     * Requirement 6.4: WHEN email format is invalid THEN the system SHALL return format validation errors
     */
    @Test
    void testEmailFormatValidation() {
        // Given - request with invalid email formats
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Valid Title");
        request.setStatus(Task.TaskStatus.PENDING);

        // Test various invalid email formats
        String[] invalidEmails = {
            "invalid-email",           // No @ symbol
            "user@",                   // Missing domain
            "@domain.com",             // Missing local part
            "user@@domain.com",        // Multiple @ symbols
            "user@domain",             // Missing TLD
            "user@.domain.com",        // Domain starts with dot
            "user@domain.com.",        // Domain ends with dot
            "user@forbidden.com"       // Valid format but forbidden domain
        };

        for (String email : invalidEmails) {
            request.setAssigneeEmail(email);
            Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);
            
            assertTrue(violations.stream().anyMatch(v -> 
                v.getPropertyPath().toString().equals("assigneeEmail")), 
                "Should validate email format for: " + email);
        }
    }

    /**
     * Requirement 6.5: IF custom validation rules exist THEN the system SHALL apply them and return specific error messages
     */
    @Test
    void testCustomValidationRules() {
        // Test custom business email validation
        CreateTaskRequest emailRequest = new CreateTaskRequest();
        emailRequest.setTitle("Valid Title");
        emailRequest.setStatus(Task.TaskStatus.PENDING);
        emailRequest.setAssigneeEmail("user@forbidden.com"); // Not in allowed domains

        Set<ConstraintViolation<CreateTaskRequest>> emailViolations = validator.validate(emailRequest);
        assertTrue(emailViolations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("assigneeEmail") && 
            v.getMessage().contains("forbidden.com") && 
            v.getMessage().contains("not allowed")), 
            "Should apply custom email domain validation");

        // Test custom priority validation
        CreateTaskRequest priorityRequest = new CreateTaskRequest();
        priorityRequest.setTitle("Valid Title");
        priorityRequest.setStatus(Task.TaskStatus.PENDING);
        priorityRequest.setPriority(0); // Invalid priority (must be 1-5)

        Set<ConstraintViolation<CreateTaskRequest>> priorityViolations = validator.validate(priorityRequest);
        assertTrue(priorityViolations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("priority") && 
            v.getMessage().contains("Priority must be between 1 and 5")), 
            "Should apply custom priority validation");

        // Test custom title pattern validation
        CreateTaskRequest patternRequest = new CreateTaskRequest();
        patternRequest.setTitle("Invalid<script>alert('xss')</script>"); // Contains invalid characters
        patternRequest.setStatus(Task.TaskStatus.PENDING);

        Set<ConstraintViolation<CreateTaskRequest>> patternViolations = validator.validate(patternRequest);
        assertTrue(patternViolations.stream().anyMatch(v -> 
            v.getPropertyPath().toString().equals("title") && 
            v.getMessage().contains("Title contains invalid characters")), 
            "Should apply custom title pattern validation");
    }

    /**
     * Test that valid data passes all validation rules
     */
    @Test
    void testValidDataPassesAllValidation() {
        // Given - completely valid request
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

    /**
     * Test validation on UpdateTaskRequest as well
     */
    @Test
    void testUpdateRequestValidation() {
        // Given - invalid update request
        UpdateTaskRequest updateRequest = new UpdateTaskRequest();
        updateRequest.setTitle(""); // Blank title
        updateRequest.setDescription("A".repeat(501)); // Too long
        updateRequest.setStatus(null); // Null status
        updateRequest.setAssigneeEmail("invalid@forbidden.com"); // Invalid domain
        updateRequest.setPriority(10); // Invalid priority

        // When
        Set<ConstraintViolation<UpdateTaskRequest>> violations = validator.validate(updateRequest);

        // Then
        assertFalse(violations.isEmpty(), "Invalid update request should have violations");
        
        // Verify all validation rules apply to update requests too
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("status")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("assigneeEmail")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("priority")));
    }

    /**
     * Test validation on Task model itself
     */
    @Test
    void testTaskModelValidation() {
        // Given - invalid task
        Task task = new Task();
        task.setTitle(""); // Blank title
        task.setDescription("A".repeat(501)); // Too long
        task.setStatus(null); // Null status
        task.setAssigneeEmail("invalid@forbidden.com"); // Invalid domain
        task.setPriority(0); // Invalid priority

        // When
        Set<ConstraintViolation<Task>> violations = validator.validate(task);

        // Then
        assertFalse(violations.isEmpty(), "Invalid task should have violations");
        
        // Verify validation applies to Task model
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("status")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("assigneeEmail")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("priority")));
    }
}