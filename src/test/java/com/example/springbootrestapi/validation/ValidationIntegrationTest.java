package com.example.springbootrestapi.validation;

import com.example.springbootrestapi.model.CreateTaskRequest;
import com.example.springbootrestapi.model.Task;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for validation functionality
 */
@SpringBootTest
@AutoConfigureWebMvc
class ValidationIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @Test
    @WithMockUser
    void createTask_WithValidationErrors_ShouldReturnFieldSpecificErrors() throws Exception {
        // Setup MockMvc with security
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Given - Request with multiple validation errors
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle(""); // Blank title
        request.setDescription("A".repeat(501)); // Too long description
        request.setStatus(null); // Null status
        request.setAssigneeEmail("invalid-email"); // Invalid email
        request.setPriority(10); // Invalid priority

        // When & Then
        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.fieldErrors").exists())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.description").exists())
                .andExpect(jsonPath("$.fieldErrors.status").exists())
                .andExpect(jsonPath("$.fieldErrors.assigneeEmail").exists())
                .andExpect(jsonPath("$.fieldErrors.priority").exists());
    }

    @Test
    @WithMockUser
    void createTask_WithValidData_ShouldSucceed() throws Exception {
        // Setup MockMvc with security
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Given - Valid request
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Valid Task Title");
        request.setDescription("Valid task description");
        request.setStatus(Task.TaskStatus.PENDING);
        request.setAssigneeEmail("user@example.com");
        request.setPriority(3);

        // When & Then
        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Valid Task Title"))
                .andExpect(jsonPath("$.description").value("Valid task description"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.assigneeEmail").value("user@example.com"))
                .andExpect(jsonPath("$.priority").value(3));
    }

    @Test
    @WithMockUser
    void createTask_WithEmailValidation_ShouldReturnSpecificError() throws Exception {
        // Setup MockMvc with security
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Given - Request with invalid email
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Valid Title");
        request.setDescription("Valid description");
        request.setStatus(Task.TaskStatus.PENDING);
        request.setAssigneeEmail("not-an-email"); // Invalid email format
        request.setPriority(3);

        // When & Then
        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.assigneeEmail").value("Invalid email format"));
    }
}