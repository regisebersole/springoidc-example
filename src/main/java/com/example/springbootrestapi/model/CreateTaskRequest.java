package com.example.springbootrestapi.model;

import com.example.springbootrestapi.validation.ValidBusinessEmail;
import com.example.springbootrestapi.validation.ValidPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Request model for creating tasks with comprehensive validation
 */
@Schema(description = "Request object for creating a new task")
public class CreateTaskRequest {
    
    @Schema(description = "Task title", example = "Complete project documentation", required = true)
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_.,!?()]+$", 
             message = "Title contains invalid characters. Only letters, numbers, spaces, and basic punctuation are allowed")
    private String title;
    
    @Schema(description = "Detailed task description", example = "Write comprehensive documentation for the REST API including all endpoints and examples")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    @Schema(description = "Initial task status", example = "PENDING", required = true)
    @NotNull(message = "Status is required")
    private Task.TaskStatus status;
    
    @Schema(description = "Email of the assigned user", example = "assignee@company.com")
    @ValidBusinessEmail(message = "Invalid business email address")
    private String assigneeEmail;
    
    @Schema(description = "Task priority level (1-5, where 1 is highest)", example = "3", minimum = "1", maximum = "5")
    @ValidPriority
    private Integer priority;

    public CreateTaskRequest() {}

    public CreateTaskRequest(String title, String description, Task.TaskStatus status) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = 3; // Default priority
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Task.TaskStatus getStatus() {
        return status;
    }

    public void setStatus(Task.TaskStatus status) {
        this.status = status;
    }

    public String getAssigneeEmail() {
        return assigneeEmail;
    }

    public void setAssigneeEmail(String assigneeEmail) {
        this.assigneeEmail = assigneeEmail;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    /**
     * Convert to Task entity
     */
    public Task toTask() {
        Task task = new Task(title, description, status);
        task.setAssigneeEmail(assigneeEmail);
        task.setPriority(priority != null ? priority : 3);
        return task;
    }
}