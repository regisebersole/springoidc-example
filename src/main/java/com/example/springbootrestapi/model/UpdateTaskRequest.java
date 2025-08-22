package com.example.springbootrestapi.model;

import com.example.springbootrestapi.validation.ValidBusinessEmail;
import com.example.springbootrestapi.validation.ValidPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Request model for updating tasks with comprehensive validation
 */
@Schema(description = "Request object for updating an existing task")
public class UpdateTaskRequest {
    
    @Schema(description = "Updated task title", example = "Complete project documentation", required = true)
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_.,!?()]+$", 
             message = "Title contains invalid characters. Only letters, numbers, spaces, and basic punctuation are allowed")
    private String title;
    
    @Schema(description = "Updated task description", example = "Write comprehensive documentation for the REST API including all endpoints and examples")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    @Schema(description = "Updated task status", example = "IN_PROGRESS", required = true)
    @NotNull(message = "Status is required")
    private Task.TaskStatus status;
    
    @Schema(description = "Updated assignee email", example = "assignee@company.com")
    @ValidBusinessEmail(message = "Invalid business email address")
    private String assigneeEmail;
    
    @Schema(description = "Updated task priority level (1-5, where 1 is highest)", example = "2", minimum = "1", maximum = "5")
    @ValidPriority
    private Integer priority;

    public UpdateTaskRequest() {}

    public UpdateTaskRequest(String title, String description, Task.TaskStatus status) {
        this.title = title;
        this.description = description;
        this.status = status;
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
     * Apply updates to existing task
     */
    public void applyTo(Task task) {
        task.setTitle(title);
        task.setDescription(description);
        task.setStatus(status);
        task.setAssigneeEmail(assigneeEmail);
        if (priority != null) {
            task.setPriority(priority);
        }
        task.updateTimestamp();
    }
}