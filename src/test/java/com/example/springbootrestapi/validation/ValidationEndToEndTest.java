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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end validation test through REST API endpoints
 * Verifies that validation errors are properly returned with field-specific messages
 */
@SpringBootTest
@AutoConfigureWebMvc
class ValidationEndToEndTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @Test
    @WithMockUser
    void testValidationErrorsReturnedThroughAPI() throws Exception {
        // Setup MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Given - invalid request with multiple validation errors
        CreateTaskRequest invalidRequest = new CreateTaskRequest();
        invalidRequest.setTitle(""); // Blank title
        invalidRequest.setDescription("A".repeat(501)); // Too long description
        invalidRequest.setStatus(null); // Null status
        invalidRequest.setAssigneeEmail("invalid@forbidden.com"); // Invalid domain
        invalidRequest.setPriority(10); // Invalid priority

        String requestJson = objectMapper.writeValueAsString(invalidRequest);

        // When & Then
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
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
    void testValidRequestSucceeds() throws Exception {
        // Setup MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Given - valid request
        CreateTaskRequest validRequest = new CreateTaskRequest();
        validRequest.setTitle("Valid Task Title");
        validRequest.setDescription("Valid description");
        validRequest.setStatus(Task.TaskStatus.PENDING);
        validRequest.setAssigneeEmail("user@company.com");
        validRequest.setPriority(3);

        String requestJson = objectMapper.writeValueAsString(validRequest);

        // When & Then
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Valid Task Title"))
                .andExpect(jsonPath("$.description").value("Valid description"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.assigneeEmail").value("user@company.com"))
                .andExpect(jsonPath("$.priority").value(3));
    }

    @Test
    @WithMockUser
    void testMalformedJsonReturnsError() throws Exception {
        // Setup MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Given - malformed JSON
        String malformedJson = "{ \"title\": \"Test\", \"status\": }"; // Missing value

        // When & Then
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("MALFORMED_JSON"))
                .andExpect(jsonPath("$.message").value("Malformed JSON in request body"));
    }

    @Test
    @WithMockUser
    void testUnsupportedMediaTypeReturnsError() throws Exception {
        // Setup MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Given - XML content type instead of JSON
        String xmlContent = "<task><title>Test</title></task>";

        // When & Then
        mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_XML)
                .content(xmlContent))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.message").value("Content type 'application/xml' not supported. Expected 'application/json'"));
    }
}