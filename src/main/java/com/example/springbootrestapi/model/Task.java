package com.example.springbootrestapi.model;

import com.example.springbootrestapi.validation.ValidBusinessEmail;
import com.example.springbootrestapi.validation.ValidPriority;
import com.example.springbootrestapi.validation.ValidTaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/**
 * Task model for demonstrating CRUD operations
 */
@Schema(description = "Task entity representing a work item")
@ValidTaskStatus
public class Task {
    
    @Schema(description = "Unique task identifier", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;
    
    @Schema(description = "Task title", example = "Complete project documentation", required = true)
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_.,!?()]+$", 
             message = "Title contains invalid characters. Only letters, numbers, spaces, and basic punctuation are allowed")
    private String title;
    
    @Schema(description = "Detailed task description", example = "Write comprehensive documentation for the REST API including all endpoints and examples")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    @Schema(description = "Current task status", example = "PENDING", required = true)
    @NotNull(message = "Status is required")
    private TaskStatus status;
    
    @Schema(description = "Task creation timestamp", example = "2024-01-15T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;
    
    @Schema(description = "Last update timestamp", example = "2024-01-15T14:45:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;
    
    @Schema(description = "User who created the task", example = "user123", accessMode = Schema.AccessMode.READ_ONLY)
    private String createdBy;
    
    @Schema(description = "Email of the assigned user", example = "assignee@company.com")
    @ValidBusinessEmail(message = "Invalid business email address")
    private String assigneeEmail;
    
    @Schema(description = "Task priority level (1-5, where 1 is highest)", example = "3", minimum = "1", maximum = "5")
    @ValidPriority
    private Integer priority;

    public Task() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = TaskStatus.PENDING;
    }

    public Task(String title, String description, TaskStatus status) {
        this();
        this.title = title;
        this.description = description;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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
     * Update the updatedAt timestamp
     */
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Task status enumeration
     */
    @Schema(description = "Available task statuses")
    public enum TaskStatus {
        @Schema(description = "Task is pending and not yet started")
        PENDING,
        @Schema(description = "Task is currently being worked on")
        IN_PROGRESS,
        @Schema(description = "Task has been completed successfully")
        COMPLETED,
        @Schema(description = "Task has been cancelled and will not be completed")
        CANCELLED
    }
}