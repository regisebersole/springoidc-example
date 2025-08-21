package com.example.springbootrestapi.service;

import com.example.springbootrestapi.model.Task;
import com.example.springbootrestapi.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for Task operations
 */
@Service
public class TaskService {

    private final TaskRepository taskRepository;

    @Autowired
    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * Get all tasks
     * @return List of all tasks
     */
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    /**
     * Get task by ID
     * @param id Task ID
     * @return Optional containing the task if found
     */
    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    /**
     * Create a new task
     * @param task Task to create
     * @param createdBy User creating the task
     * @return Created task
     */
    public Task createTask(Task task, String createdBy) {
        task.setId(null); // Ensure it's treated as new
        task.setCreatedBy(createdBy);
        return taskRepository.save(task);
    }

    /**
     * Update an existing task
     * @param id Task ID to update
     * @param updatedTask Updated task data
     * @return Updated task if found, empty otherwise
     */
    public Optional<Task> updateTask(Long id, Task updatedTask) {
        return taskRepository.findById(id)
                .map(existingTask -> {
                    existingTask.setTitle(updatedTask.getTitle());
                    existingTask.setDescription(updatedTask.getDescription());
                    existingTask.setStatus(updatedTask.getStatus());
                    existingTask.updateTimestamp();
                    return taskRepository.save(existingTask);
                });
    }

    /**
     * Delete a task by ID
     * @param id Task ID to delete
     * @return true if task was deleted, false if not found
     */
    public boolean deleteTask(Long id) {
        return taskRepository.deleteById(id);
    }

    /**
     * Get tasks by status
     * @param status Task status
     * @return List of tasks with the specified status
     */
    public List<Task> getTasksByStatus(Task.TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    /**
     * Get tasks created by a specific user
     * @param createdBy User who created the tasks
     * @return List of tasks created by the user
     */
    public List<Task> getTasksByCreatedBy(String createdBy) {
        return taskRepository.findByCreatedBy(createdBy);
    }

    /**
     * Check if task exists
     * @param id Task ID
     * @return true if task exists
     */
    public boolean taskExists(Long id) {
        return taskRepository.existsById(id);
    }
}