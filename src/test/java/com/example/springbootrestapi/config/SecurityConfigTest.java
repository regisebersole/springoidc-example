package com.example.springbootrestapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for SecurityConfig to verify CORS, authentication, and authorization rules.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.security.oauth2.resourceserver.opaque-token.introspection-uri=https://oauth2.googleapis.com/tokeninfo",
    "spring.security.oauth2.resourceserver.opaque-token.client-id=test-client",
    "spring.security.oauth2.resourceserver.opaque-token.client-secret=test-secret",
    "app.cors.allowed-origins=http://localhost:3000",
    "app.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS",
    "app.cors.allowed-headers=*",
    "app.cors.allow-credentials=true"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowAccessToPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowAccessToSwaggerEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRequireAuthenticationForProtectedApiEndpoints() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAccessToAuthEndpoints() throws Exception {
        // Auth endpoints should be accessible (they handle their own authentication)
        mockMvc.perform(post("/api/auth/token"))
                .andExpect(status().isNotFound()); // 404 because endpoint doesn't exist yet, not 401
    }

    @Test
    void shouldHandleCorsPreflightRequests() throws Exception {
        mockMvc.perform(options("/api/test")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void shouldRejectCorsFromUnauthorizedOrigin() throws Exception {
        mockMvc.perform(options("/api/test")
                .header("Origin", "http://malicious-site.com")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}