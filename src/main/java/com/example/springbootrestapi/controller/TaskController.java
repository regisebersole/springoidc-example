package com.example.springbootrestapi.controller;

import com.example.springbootrestapi.model.Task;
import com.example.springbootrestapi.model.CreateTaskRequest;
import com.example.springbootrestapi.model.UpdateTaskRequest;
import com.example.springbootrestapi.service.TaskService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Task CRUD operations
 * Demonstrates proper HTTP methods, JSON serialization, and status codes
 */
@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
public class TaskController {

    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    private final TaskService taskService;

    @Autowired
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * GET /api/tasks - Get all tasks
     * @return List of all tasks with HTTP 200
     */
    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks() {
        logger.debug("Getting all tasks");
        List<Task> tasks = taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * GET /api/tasks/{id} - Get task by ID
     * @param id Task ID
     * @return Task with HTTP 200 if found, HTTP 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        logger.debug("Getting task with ID: {}", id);
        return taskService.getTaskById(id)
                .map(task -> ResponseEntity.ok(task))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/tasks - Create a new task
     * @param createRequest Task creation request with validation
     * @return Created task with HTTP 201
     */
    @PostMapping
    public ResponseEntity<Task> createTask(@Valid @RequestBody CreateTaskRequest createRequest) {
        logger.debug("Creating new task: {}", createRequest.getTitle());
        
        String createdBy = getCurrentUserId();
        Task task = createRequest.toTask();
        Task createdTask = taskService.createTask(task, createdBy);
        
        logger.info("Created task with ID: {}", createdTask.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTask);
    }

    /**
     * PUT /api/tasks/{id} - Update an existing task
     * @param id Task ID to update
     * @param updateRequest Updated task data with validation
     * @return Updated task with HTTP 200 if found, HTTP 404 if not found
     */
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest updateRequest) {
        logger.debug("Updating task with ID: {}", id);
        
        return taskService.getTaskById(id)
                .map(existingTask -> {
                    updateRequest.applyTo(existingTask);
                    Task updatedTask = taskService.updateTask(id, existingTask).orElse(null);
                    if (updatedTask != null) {
                        logger.info("Updated task with ID: {}", id);
                        return ResponseEntity.ok(updatedTask);
                    }
                    return ResponseEntity.notFound().<Task>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/tasks/{id} - Delete a task
     * @param id Task ID to delete
     * @return HTTP 204 if deleted, HTTP 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        logger.debug("Deleting task with ID: {}", id);
        
        if (taskService.deleteTask(id)) {
            logger.info("Deleted task with ID: {}", id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/tasks/status/{status} - Get tasks by status
     * @param status Task status
     * @return List of tasks with the specified status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Task>> getTasksByStatus(@PathVariable Task.TaskStatus status) {
        logger.debug("Getting tasks with status: {}", status);
        List<Task> tasks = taskService.getTasksByStatus(status);
        return ResponseEntity.ok(tasks);
    }

    /**
     * GET /api/tasks/my - Get tasks created by current user
     * @return List of tasks created by current user
     */
    @GetMapping("/my")
    public ResponseEntity<List<Task>> getMyTasks() {
        String currentUserId = getCurrentUserId();
        logger.debug("Getting tasks for user: {}", currentUserId);
        List<Task> tasks = taskService.getTasksByCreatedBy(currentUserId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * PATCH /api/tasks/{id}/status - Update task status only
     * @param id Task ID
     * @param statusUpdate Map containing the new status
     * @return Updated task with HTTP 200 if found, HTTP 404 if not found
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Task> updateTaskStatus(@PathVariable Long id, 
                                                @RequestBody Map<String, String> statusUpdate) {
        logger.debug("Updating status for task ID: {}", id);
        
        String statusStr = statusUpdate.get("status");
        if (statusStr == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Task.TaskStatus newStatus = Task.TaskStatus.valueOf(statusStr.toUpperCase());
            
            return taskService.getTaskById(id)
                    .map(task -> {
                        task.setStatus(newStatus);
                        task.updateTimestamp();
                        Task updatedTask = taskService.updateTask(id, task).orElse(null);
                        if (updatedTask != null) {
                            logger.info("Updated status for task ID: {} to {}", id, newStatus);
                            return ResponseEntity.ok(updatedTask);
                        }
                        return ResponseEntity.notFound().<Task>build();
                    })
                    .orElse(ResponseEntity.notFound().build());
                    
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid status value: {}", statusStr);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/tasks/count - Get total count of tasks
     * @return Count of tasks
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getTaskCount() {
        long count = taskService.getAllTasks().size();
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Extract current user ID from security context
     * @return Current user ID or "anonymous" if not authenticated
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !authentication.getName().equals("anonymousUser")) {
            return authentication.getName();
        }
        return "anonymous";
    }
}