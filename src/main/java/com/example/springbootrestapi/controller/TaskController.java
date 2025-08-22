package com.example.springbootrestapi.controller;

import com.example.springbootrestapi.model.Task;
import com.example.springbootrestapi.model.CreateTaskRequest;
import com.example.springbootrestapi.model.UpdateTaskRequest;
import com.example.springbootrestapi.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Tasks", description = "Task management operations")
@SecurityRequirement(name = "bearerAuth")
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
    @Operation(
        summary = "Get all tasks",
        description = "Retrieves a list of all tasks in the system"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved all tasks",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Task.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing authentication token"
        )
    })
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
    @Operation(
        summary = "Get task by ID",
        description = "Retrieves a specific task by its unique identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Task found and returned successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Task.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Task not found with the specified ID"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing authentication token"
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(
        @Parameter(description = "Unique identifier of the task", required = true, example = "1")
        @PathVariable Long id) {
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
    @Operation(
        summary = "Create a new task",
        description = "Creates a new task with the provided information. All validation rules will be applied."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Task created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Task.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data - validation errors"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing authentication token"
        )
    })
    @PostMapping
    public ResponseEntity<Task> createTask(
        @Parameter(description = "Task creation request", required = true)
        @Valid @RequestBody CreateTaskRequest createRequest) {
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
    @Operation(
        summary = "Update an existing task",
        description = "Updates all fields of an existing task. All validation rules will be applied."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Task updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Task.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data - validation errors"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Task not found with the specified ID"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing authentication token"
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
        @Parameter(description = "Unique identifier of the task to update", required = true, example = "1")
        @PathVariable Long id, 
        @Parameter(description = "Updated task data", required = true)
        @Valid @RequestBody UpdateTaskRequest updateRequest) {
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
    @Operation(
        summary = "Delete a task",
        description = "Permanently deletes a task from the system"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Task deleted successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Task not found with the specified ID"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing authentication token"
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
        @Parameter(description = "Unique identifier of the task to delete", required = true, example = "1")
        @PathVariable Long id) {
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
    @Operation(
        summary = "Get tasks by status",
        description = "Retrieves all tasks that have the specified status"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved tasks with the specified status",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Task.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid status value"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing authentication token"
        )
    })
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Task>> getTasksByStatus(
        @Parameter(description = "Task status to filter by", required = true, example = "PENDING")
        @PathVariable Task.TaskStatus status) {
        logger.debug("Getting tasks with status: {}", status);
        List<Task> tasks = taskService.getTasksByStatus(status);
        return ResponseEntity.ok(tasks);
    }

    /**
     * GET /api/tasks/my - Get tasks created by current user
     * @return List of tasks created by current user
     */
    @Operation(
        summary = "Get current user's tasks",
        description = "Retrieves all tasks created by the currently authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved user's tasks",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Task.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing authentication token"
        )
    })
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
    @Operation(
        summary = "Update task status",
        description = "Updates only the status field of an existing task"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Task status updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Task.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid status value or missing status field"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Task not found with the specified ID"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing authentication token"
        )
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<Task> updateTaskStatus(
        @Parameter(description = "Unique identifier of the task", required = true, example = "1")
        @PathVariable Long id, 
        @Parameter(description = "Status update object", required = true, 
                  schema = @Schema(example = "{\"status\": \"COMPLETED\"}"))
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
    @Operation(
        summary = "Get task count",
        description = "Returns the total number of tasks in the system"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved task count",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = "{\"count\": 42}")
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing authentication token"
        )
    })
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