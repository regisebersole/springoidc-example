package com.example.springbootrestapi.validation;

import com.example.springbootrestapi.model.Task;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskStatusValidatorTest {

    @Mock
    private ValidTaskStatus validTaskStatus;

    @Mock
    private ConstraintValidatorContext context;

    private TaskStatusValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TaskStatusValidator();
        validator.initialize(validTaskStatus);
    }

    @Test
    void testNullTaskIsValid() {
        // When & Then
        assertTrue(validator.isValid(null, context));
        verifyNoInteractions(context);
    }

    @Test
    void testTaskWithNullStatusIsValid() {
        // Given
        Task task = new Task();
        task.setStatus(null);

        // When & Then
        assertTrue(validator.isValid(task, context));
        verifyNoInteractions(context);
    }

    @Test
    void testNewTaskWithAnyStatusIsValid() {
        // Given - new task (no ID)
        Task task = new Task();
        task.setStatus(Task.TaskStatus.PENDING);

        // When & Then
        assertTrue(validator.isValid(task, context));

        // Test all statuses for new tasks
        task.setStatus(Task.TaskStatus.IN_PROGRESS);
        assertTrue(validator.isValid(task, context));

        task.setStatus(Task.TaskStatus.COMPLETED);
        assertTrue(validator.isValid(task, context));

        task.setStatus(Task.TaskStatus.CANCELLED);
        assertTrue(validator.isValid(task, context));

        verifyNoInteractions(context);
    }

    @Test
    void testExistingTaskWithValidStatusTransitions() {
        // Given - existing task (has ID)
        Task task = new Task();
        task.setId(1L);

        // When & Then - all status transitions should be valid in current implementation
        task.setStatus(Task.TaskStatus.PENDING);
        assertTrue(validator.isValid(task, context));

        task.setStatus(Task.TaskStatus.IN_PROGRESS);
        assertTrue(validator.isValid(task, context));

        task.setStatus(Task.TaskStatus.COMPLETED);
        assertTrue(validator.isValid(task, context));

        task.setStatus(Task.TaskStatus.CANCELLED);
        assertTrue(validator.isValid(task, context));

        verifyNoInteractions(context);
    }

    @Test
    void testValidStatusTransitionLogic() {
        // Given
        Task task = new Task();
        task.setId(1L);

        // When & Then - test the private method logic through public interface
        // All statuses should be valid according to current business rules
        for (Task.TaskStatus status : Task.TaskStatus.values()) {
            task.setStatus(status);
            assertTrue(validator.isValid(task, context), 
                "Status " + status + " should be valid");
        }

        verifyNoInteractions(context);
    }

    @Test
    void testTaskStatusEnumValues() {
        // Verify all enum values exist and are handled
        Task.TaskStatus[] statuses = Task.TaskStatus.values();
        assertEquals(4, statuses.length);
        
        // Verify specific enum values
        assertTrue(java.util.Arrays.asList(statuses).contains(Task.TaskStatus.PENDING));
        assertTrue(java.util.Arrays.asList(statuses).contains(Task.TaskStatus.IN_PROGRESS));
        assertTrue(java.util.Arrays.asList(statuses).contains(Task.TaskStatus.COMPLETED));
        assertTrue(java.util.Arrays.asList(statuses).contains(Task.TaskStatus.CANCELLED));
    }
}