package com.example.springbootrestapi.repository;

import com.example.springbootrestapi.model.Task;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory repository for Task entities
 * This is a simple implementation for demonstration purposes
 */
@Repository
public class TaskRepository {
    
    private final Map<Long, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * Find all tasks
     * @return List of all tasks
     */
    public List<Task> findAll() {
        return new ArrayList<>(tasks.values());
    }

    /**
     * Find task by ID
     * @param id Task ID
     * @return Optional containing the task if found
     */
    public Optional<Task> findById(Long id) {
        return Optional.ofNullable(tasks.get(id));
    }

    /**
     * Save a new task or update existing one
     * @param task Task to save
     * @return Saved task with generated ID
     */
    public Task save(Task task) {
        if (task.getId() == null) {
            // New task - generate ID
            task.setId(idGenerator.getAndIncrement());
        } else {
            // Update existing task
            task.updateTimestamp();
        }
        tasks.put(task.getId(), task);
        return task;
    }

    /**
     * Delete task by ID
     * @param id Task ID to delete
     * @return true if task was deleted, false if not found
     */
    public boolean deleteById(Long id) {
        return tasks.remove(id) != null;
    }

    /**
     * Check if task exists by ID
     * @param id Task ID
     * @return true if task exists
     */
    public boolean existsById(Long id) {
        return tasks.containsKey(id);
    }

    /**
     * Count total number of tasks
     * @return Total count
     */
    public long count() {
        return tasks.size();
    }

    /**
     * Find tasks by status
     * @param status Task status
     * @return List of tasks with the specified status
     */
    public List<Task> findByStatus(Task.TaskStatus status) {
        return tasks.values().stream()
                .filter(task -> task.getStatus() == status)
                .toList();
    }

    /**
     * Find tasks by created by user
     * @param createdBy User who created the tasks
     * @return List of tasks created by the user
     */
    public List<Task> findByCreatedBy(String createdBy) {
        return tasks.values().stream()
                .filter(task -> Objects.equals(task.getCreatedBy(), createdBy))
                .toList();
    }
}