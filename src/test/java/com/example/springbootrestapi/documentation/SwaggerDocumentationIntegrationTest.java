package com.example.springbootrestapi.documentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Swagger/OpenAPI documentation endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "springdoc.api-docs.enabled=true",
    "springdoc.swagger-ui.enabled=true"
})
class SwaggerDocumentationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testOpenApiDocsEndpointAccessible() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Spring Boot REST API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"));
    }

    @Test
    void testSwaggerUiEndpointAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/swagger-ui/index.html"));
    }

    @Test
    void testSwaggerUiIndexPageAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html"));
    }

    @Test
    void testOpenApiDocsContainsExpectedPaths() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths").exists())
                .andExpect(jsonPath("$.paths['/api/auth/token-exchange']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/refresh-session']").exists())
                .andExpect(jsonPath("$.paths['/api/tasks']").exists())
                .andExpect(jsonPath("$.paths['/api/tasks/{id}']").exists());
    }

    @Test
    void testOpenApiDocsContainsSecuritySchemes() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                .andExpect(jsonPath("$.components.securitySchemes.oidcAuth").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    @Test
    void testOpenApiDocsContainsExpectedTags() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Authentication')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Tasks')]").exists());
    }

    @Test
    void testOpenApiDocsContainsSchemas() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas").exists())
                .andExpect(jsonPath("$.components.schemas.Task").exists())
                .andExpect(jsonPath("$.components.schemas.AuthRequest").exists())
                .andExpect(jsonPath("$.components.schemas.AuthResponse").exists())
                .andExpect(jsonPath("$.components.schemas.CreateTaskRequest").exists())
                .andExpect(jsonPath("$.components.schemas.UpdateTaskRequest").exists());
    }

    @Test
    void testTaskEndpointDocumentation() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/tasks'].get.summary").value("Get all tasks"))
                .andExpect(jsonPath("$.paths['/api/tasks'].post.summary").value("Create a new task"))
                .andExpect(jsonPath("$.paths['/api/tasks/{id}'].get.summary").value("Get task by ID"))
                .andExpect(jsonPath("$.paths['/api/tasks/{id}'].put.summary").value("Update an existing task"))
                .andExpect(jsonPath("$.paths['/api/tasks/{id}'].delete.summary").value("Delete a task"));
    }

    @Test
    void testAuthEndpointDocumentation() throws Exception {
        mockMvc.perform(get("/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/token-exchange'].post.summary").value("Exchange OIDC access token for session JWT"))
                .andExpect(jsonPath("$.paths['/api/auth/refresh-session'].post.summary").value("Refresh session JWT"))
                .andExpect(jsonPath("$.paths['/api/auth/health'].get.summary").value("Authentication service health check"));
    }
}