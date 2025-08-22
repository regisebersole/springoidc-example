package com.example.springbootrestapi.model;

import com.example.springbootrestapi.validation.ValidBusinessEmail;
import com.example.springbootrestapi.validation.ValidPriority;
import com.example.springbootrestapi.validation.ValidTaskStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

/**
 * Task model for demonstrating CRUD operations
 */
@ValidTaskStatus
public class Task {
    
    private Long id;
    
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_.,!?()]+$", 
             message = "Title contains invalid characters. Only letters, numbers, spaces, and basic punctuation are allowed")
    private String title;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    @NotNull(message = "Status is required")
    private TaskStatus status;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    
    @ValidBusinessEmail(message = "Invalid business email address")
    private String assigneeEmail;
    
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
    public enum TaskStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
}