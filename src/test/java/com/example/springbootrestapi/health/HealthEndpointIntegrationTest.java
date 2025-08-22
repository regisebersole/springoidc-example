package com.example.springbootrestapi.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
class HealthEndpointIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthEndpoint_ShouldReturnHealthStatus() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/health", Map.class);
        
        // Then
        // Health endpoint might return 503 if any health indicator is DOWN
        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("status");
        
        String status = (String) response.getBody().get("status");
        assertThat(status).isIn("UP", "DOWN");
    }

    @Test
    void infoEndpoint_ShouldReturnApplicationInfo() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/info", Map.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("app");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> appInfo = (Map<String, Object>) response.getBody().get("app");
        assertThat(appInfo).containsKey("name");
        assertThat(appInfo).containsKey("description");
        assertThat(appInfo).containsKey("version");
        assertThat(appInfo.get("name")).isEqualTo("Spring Boot REST API Test");
    }

    @Test
    void metricsEndpoint_ShouldReturnMetrics() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/metrics", Map.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("names");
        
        @SuppressWarnings("unchecked")
        java.util.List<String> metricNames = (java.util.List<String>) response.getBody().get("names");
        assertThat(metricNames).isNotEmpty();
        assertThat(metricNames).contains("jvm.memory.used");
        assertThat(metricNames).contains("system.cpu.usage");
    }

    @Test
    void specificMetric_ShouldReturnMetricDetails() {
        // When
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator/metrics/jvm.memory.used", Map.class);
        
        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("name");
        assertThat(response.getBody()).containsKey("measurements");
        assertThat(response.getBody().get("name")).isEqualTo("jvm.memory.used");
    }

    @Test
    void actuatorEndpoints_ShouldBeAccessible() {
        // Test that actuator base path is accessible
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/actuator", Map.class);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("_links");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> links = (Map<String, Object>) response.getBody().get("_links");
        assertThat(links).containsKey("health");
        assertThat(links).containsKey("info");
        assertThat(links).containsKey("metrics");
    }
}