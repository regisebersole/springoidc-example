package com.example.springbootrestapi.model;

import jakarta.validation.constraints.*;

/**
 * Request model for creating tasks with comprehensive validation
 */
public class CreateTaskRequest {
    
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    @NotNull(message = "Status is required")
    private Task.TaskStatus status;
    
    @Email(message = "Invalid email format")
    private String assigneeEmail;
    
    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 5, message = "Priority must not exceed 5")
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