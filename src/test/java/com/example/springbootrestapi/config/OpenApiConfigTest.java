package com.example.springbootrestapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for OpenAPI configuration
 */
@SpringBootTest
@TestPropertySource(properties = {
    "server.port=8080"
})
class OpenApiConfigTest {

    @Autowired
    private OpenApiConfig openApiConfig;

    @Test
    void testOpenApiConfigurationExists() {
        assertThat(openApiConfig).isNotNull();
    }

    @Test
    void testCustomOpenApiBean() {
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        
        assertThat(openAPI).isNotNull();
        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("Spring Boot REST API");
        assertThat(openAPI.getInfo().getDescription()).contains("REST API built with Spring Boot");
        assertThat(openAPI.getInfo().getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void testOpenApiInfoConfiguration() {
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        
        assertThat(openAPI.getInfo().getContact()).isNotNull();
        assertThat(openAPI.getInfo().getContact().getName()).isEqualTo("API Support");
        assertThat(openAPI.getInfo().getContact().getEmail()).isEqualTo("support@example.com");
        
        assertThat(openAPI.getInfo().getLicense()).isNotNull();
        assertThat(openAPI.getInfo().getLicense().getName()).isEqualTo("MIT License");
    }

    @Test
    void testOpenApiServersConfiguration() {
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        
        assertThat(openAPI.getServers()).isNotNull();
        assertThat(openAPI.getServers()).hasSize(2);
        assertThat(openAPI.getServers().get(0).getUrl()).isEqualTo("http://localhost:8080");
        assertThat(openAPI.getServers().get(0).getDescription()).isEqualTo("Development server");
        assertThat(openAPI.getServers().get(1).getUrl()).isEqualTo("https://api.example.com");
        assertThat(openAPI.getServers().get(1).getDescription()).isEqualTo("Production server");
    }

    @Test
    void testOpenApiSecurityConfiguration() {
        OpenAPI openAPI = openApiConfig.customOpenAPI();
        
        assertThat(openAPI.getComponents()).isNotNull();
        assertThat(openAPI.getComponents().getSecuritySchemes()).isNotNull();
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openAPI.getComponents().getSecuritySchemes()).containsKey("oidcAuth");
        
        assertThat(openAPI.getSecurity()).isNotNull();
        assertThat(openAPI.getSecurity()).hasSize(1);
        assertThat(openAPI.getSecurity().get(0).keySet()).contains("bearerAuth");
    }
}