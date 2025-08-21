package com.example.springbootrestapi.controller;

import com.example.springbootrestapi.model.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for TaskController using full Spring Boot context
 * Tests REST API endpoints with proper HTTP methods and status codes
 */
@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "app.cors.allowed-origins=http://localhost:3000",
    "app.cors.allowed-methods=GET,POST,PUT,DELETE,PATCH,OPTIONS",
    "app.cors.allowed-headers=*",
    "app.cors.allow-credentials=true",
    "spring.security.oauth2.resourceserver.opaque-token.introspection-uri=",
    "spring.security.oauth2.resourceserver.opaque-token.client-id=",
    "spring.security.oauth2.resourceserver.opaque-token.client-secret="
})
class TaskControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser
    void getAllTasks_ShouldReturnTaskList_WithHttp200() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser
    void createTask_WithValidData_ShouldCreateTask_WithHttp201() throws Exception {
        Task newTask = new Task("Test Task", "Test Description", Task.TaskStatus.PENDING);

        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newTask)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Test Task"))
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @WithMockUser
    void createAndGetTask_ShouldWork() throws Exception {
        // Create a task
        Task newTask = new Task("Integration Test Task", "Integration Description", Task.TaskStatus.PENDING);

        String response = mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newTask)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Task createdTask = objectMapper.readValue(response, Task.class);

        // Get the created task
        mockMvc.perform(get("/api/tasks/" + createdTask.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(createdTask.getId()))
                .andExpect(jsonPath("$.title").value("Integration Test Task"))
                .andExpect(jsonPath("$.description").value("Integration Description"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser
    void createUpdateAndDeleteTask_ShouldWork() throws Exception {
        // Create a task
        Task newTask = new Task("CRUD Test Task", "CRUD Description", Task.TaskStatus.PENDING);

        String createResponse = mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newTask)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Task createdTask = objectMapper.readValue(createResponse, Task.class);

        // Update the task
        createdTask.setTitle("Updated CRUD Task");
        createdTask.setStatus(Task.TaskStatus.IN_PROGRESS);

        mockMvc.perform(put("/api/tasks/" + createdTask.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createdTask)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated CRUD Task"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        // Delete the task
        mockMvc.perform(delete("/api/tasks/" + createdTask.getId())
                .with(csrf()))
                .andExpect(status().isNoContent());

        // Verify task is deleted
        mockMvc.perform(get("/api/tasks/" + createdTask.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void createTask_WithInvalidData_ShouldReturn400() throws Exception {
        // Task with blank title (validation should fail)
        Task invalidTask = new Task("", "Description", Task.TaskStatus.PENDING);

        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidTask)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    @WithMockUser
    void createTask_WithMalformedJson_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("MALFORMED_JSON"));
    }

    @Test
    @WithMockUser
    void createTask_WithUnsupportedMediaType_ShouldReturn415() throws Exception {
        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.TEXT_PLAIN)
                .content("plain text content"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    @WithMockUser
    void getTaskById_WhenTaskNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/tasks/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void updateTask_WhenTaskNotExists_ShouldReturn404() throws Exception {
        Task updatedTask = new Task("Updated Task", "Updated Description", Task.TaskStatus.IN_PROGRESS);
        
        mockMvc.perform(put("/api/tasks/999")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedTask)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void deleteTask_WhenTaskNotExists_ShouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/tasks/999")
                .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getTasksByStatus_ShouldReturnFilteredTasks() throws Exception {
        // Create tasks with different statuses
        Task pendingTask = new Task("Pending Task", "Pending Description", Task.TaskStatus.PENDING);
        Task inProgressTask = new Task("In Progress Task", "In Progress Description", Task.TaskStatus.IN_PROGRESS);

        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pendingTask)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inProgressTask)))
                .andExpect(status().isCreated());

        // Get tasks by status
        mockMvc.perform(get("/api/tasks/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[?(@.status == 'PENDING')]").exists());
    }

    @Test
    @WithMockUser
    void updateTaskStatus_WithValidStatus_ShouldUpdateStatus() throws Exception {
        // Create a task
        Task newTask = new Task("Status Test Task", "Status Description", Task.TaskStatus.PENDING);

        String createResponse = mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newTask)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Task createdTask = objectMapper.readValue(createResponse, Task.class);

        // Update status
        mockMvc.perform(patch("/api/tasks/" + createdTask.getId() + "/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser
    void updateTaskStatus_WithInvalidStatus_ShouldReturn400() throws Exception {
        // Create a task first
        Task newTask = new Task("Status Test Task", "Status Description", Task.TaskStatus.PENDING);

        String createResponse = mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newTask)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Task createdTask = objectMapper.readValue(createResponse, Task.class);

        // Try to update with invalid status
        mockMvc.perform(patch("/api/tasks/" + createdTask.getId() + "/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"INVALID_STATUS\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void getTaskCount_ShouldReturnCount() throws Exception {
        // Create a few tasks
        Task task1 = new Task("Count Test Task 1", "Description 1", Task.TaskStatus.PENDING);
        Task task2 = new Task("Count Test Task 2", "Description 2", Task.TaskStatus.IN_PROGRESS);

        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(task1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(task2)))
                .andExpect(status().isCreated());

        // Get count (should be at least 2, but might be more from other tests)
        mockMvc.perform(get("/api/tasks/count"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.count").isNumber());
    }

    @Test
    @WithMockUser
    void methodNotAllowed_ShouldReturn405() throws Exception {
        // Try to use PATCH on the main tasks endpoint (not supported)
        mockMvc.perform(patch("/api/tasks")
                .with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("METHOD_NOT_ALLOWED"));
    }
}