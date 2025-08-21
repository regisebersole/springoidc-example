package com.example.springbootrestapi.controller;

import com.example.springbootrestapi.model.Task;
import com.example.springbootrestapi.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TaskController
 * Tests all CRUD endpoints with proper HTTP methods and status codes
 */
@WebMvcTest(controllers = TaskController.class, 
           excludeAutoConfiguration = {
               org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
               org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
           })
@TestPropertySource(properties = {
    "app.cors.allowed-origins=http://localhost:3000",
    "app.cors.allowed-methods=GET,POST,PUT,DELETE,PATCH,OPTIONS",
    "app.cors.allowed-headers=*",
    "app.cors.allow-credentials=true"
})
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @Autowired
    private ObjectMapper objectMapper;

    private Task sampleTask;
    private List<Task> sampleTasks;

    @BeforeEach
    void setUp() {
        sampleTask = new Task("Sample Task", "Sample Description", Task.TaskStatus.PENDING);
        sampleTask.setId(1L);
        sampleTask.setCreatedBy("testuser");
        sampleTask.setCreatedAt(LocalDateTime.now());
        sampleTask.setUpdatedAt(LocalDateTime.now());

        Task task2 = new Task("Task 2", "Description 2", Task.TaskStatus.IN_PROGRESS);
        task2.setId(2L);
        task2.setCreatedBy("testuser");

        sampleTasks = Arrays.asList(sampleTask, task2);
    }

    @Test
    @WithMockUser
    void getAllTasks_ShouldReturnAllTasks_WithHttp200() throws Exception {
        // Given
        when(taskService.getAllTasks()).thenReturn(sampleTasks);

        // When & Then
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Sample Task"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].title").value("Task 2"));

        verify(taskService).getAllTasks();
    }

    @Test
    @WithMockUser
    void getTaskById_WhenTaskExists_ShouldReturnTask_WithHttp200() throws Exception {
        // Given
        when(taskService.getTaskById(1L)).thenReturn(Optional.of(sampleTask));

        // When & Then
        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Sample Task"))
                .andExpect(jsonPath("$.description").value("Sample Description"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(taskService).getTaskById(1L);
    }

    @Test
    @WithMockUser
    void getTaskById_WhenTaskNotExists_ShouldReturn404() throws Exception {
        // Given
        when(taskService.getTaskById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/tasks/999"))
                .andExpect(status().isNotFound());

        verify(taskService).getTaskById(999L);
    }

    @Test
    @WithMockUser
    void createTask_WithValidData_ShouldCreateTask_WithHttp201() throws Exception {
        // Given
        Task newTask = new Task("New Task", "New Description", Task.TaskStatus.PENDING);
        Task createdTask = new Task("New Task", "New Description", Task.TaskStatus.PENDING);
        createdTask.setId(3L);
        createdTask.setCreatedBy("user");

        when(taskService.createTask(any(Task.class), anyString())).thenReturn(createdTask);

        // When & Then
        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newTask)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.title").value("New Task"))
                .andExpect(jsonPath("$.description").value("New Description"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(taskService).createTask(any(Task.class), anyString());
    }

    @Test
    @WithMockUser
    void createTask_WithInvalidData_ShouldReturn400() throws Exception {
        // Given - Task with blank title (validation should fail)
        Task invalidTask = new Task("", "Description", Task.TaskStatus.PENDING);

        // When & Then
        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidTask)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());

        verify(taskService, never()).createTask(any(Task.class), anyString());
    }

    @Test
    @WithMockUser
    void createTask_WithMalformedJson_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("MALFORMED_JSON"));

        verify(taskService, never()).createTask(any(Task.class), anyString());
    }

    @Test
    @WithMockUser
    void createTask_WithUnsupportedMediaType_ShouldReturn415() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.TEXT_PLAIN)
                .content("plain text content"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_MEDIA_TYPE"));

        verify(taskService, never()).createTask(any(Task.class), anyString());
    }

    @Test
    @WithMockUser
    void updateTask_WhenTaskExists_ShouldUpdateTask_WithHttp200() throws Exception {
        // Given
        Task updatedTask = new Task("Updated Task", "Updated Description", Task.TaskStatus.IN_PROGRESS);
        updatedTask.setId(1L);

        when(taskService.updateTask(eq(1L), any(Task.class))).thenReturn(Optional.of(updatedTask));

        // When & Then
        mockMvc.perform(put("/api/tasks/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedTask)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Updated Task"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(taskService).updateTask(eq(1L), any(Task.class));
    }

    @Test
    @WithMockUser
    void updateTask_WhenTaskNotExists_ShouldReturn404() throws Exception {
        // Given
        Task updatedTask = new Task("Updated Task", "Updated Description", Task.TaskStatus.IN_PROGRESS);
        when(taskService.updateTask(eq(999L), any(Task.class))).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/api/tasks/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedTask)))
                .andExpect(status().isNotFound());

        verify(taskService).updateTask(eq(999L), any(Task.class));
    }

    @Test
    @WithMockUser
    void deleteTask_WhenTaskExists_ShouldDeleteTask_WithHttp204() throws Exception {
        // Given
        when(taskService.deleteTask(1L)).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/tasks/1")
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(taskService).deleteTask(1L);
    }

    @Test
    @WithMockUser
    void deleteTask_WhenTaskNotExists_ShouldReturn404() throws Exception {
        // Given
        when(taskService.deleteTask(999L)).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/tasks/999")
                .with(csrf()))
                .andExpect(status().isNotFound());

        verify(taskService).deleteTask(999L);
    }

    @Test
    @WithMockUser
    void getTasksByStatus_ShouldReturnFilteredTasks_WithHttp200() throws Exception {
        // Given
        List<Task> pendingTasks = Arrays.asList(sampleTask);
        when(taskService.getTasksByStatus(Task.TaskStatus.PENDING)).thenReturn(pendingTasks);

        // When & Then
        mockMvc.perform(get("/api/tasks/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        verify(taskService).getTasksByStatus(Task.TaskStatus.PENDING);
    }

    @Test
    @WithMockUser
    void getMyTasks_ShouldReturnUserTasks_WithHttp200() throws Exception {
        // Given
        when(taskService.getTasksByCreatedBy(anyString())).thenReturn(sampleTasks);

        // When & Then
        mockMvc.perform(get("/api/tasks/my"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2));

        verify(taskService).getTasksByCreatedBy(anyString());
    }

    @Test
    @WithMockUser
    void updateTaskStatus_WithValidStatus_ShouldUpdateStatus_WithHttp200() throws Exception {
        // Given
        Task updatedTask = new Task("Sample Task", "Sample Description", Task.TaskStatus.COMPLETED);
        updatedTask.setId(1L);
        
        when(taskService.getTaskById(1L)).thenReturn(Optional.of(sampleTask));
        when(taskService.updateTask(eq(1L), any(Task.class))).thenReturn(Optional.of(updatedTask));

        // When & Then
        mockMvc.perform(patch("/api/tasks/1/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(taskService).getTaskById(1L);
        verify(taskService).updateTask(eq(1L), any(Task.class));
    }

    @Test
    @WithMockUser
    void updateTaskStatus_WithInvalidStatus_ShouldReturn400() throws Exception {
        // When & Then
        mockMvc.perform(patch("/api/tasks/1/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"INVALID_STATUS\"}"))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).updateTask(anyLong(), any(Task.class));
    }

    @Test
    @WithMockUser
    void getTaskCount_ShouldReturnCount_WithHttp200() throws Exception {
        // Given
        when(taskService.getAllTasks()).thenReturn(sampleTasks);

        // When & Then
        mockMvc.perform(get("/api/tasks/count"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.count").value(2));

        verify(taskService).getAllTasks();
    }

    @Test
    @WithMockUser
    void methodNotAllowed_ShouldReturn405() throws Exception {
        // When & Then - Try to use PATCH on the main tasks endpoint
        mockMvc.perform(patch("/api/tasks")
                .with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"));
    }
}