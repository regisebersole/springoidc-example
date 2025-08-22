package com.example.springbootrestapi.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationHealthIndicatorTest {

    private ApplicationHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new ApplicationHealthIndicator();
    }

    @Test
    void health_ShouldReturnUpStatus_WhenApplicationIsHealthy() {
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("application");
        assertThat(health.getDetails()).containsKey("memory-usage-percent");
        assertThat(health.getDetails()).containsKey("used-memory-mb");
        assertThat(health.getDetails()).containsKey("max-memory-mb");
        assertThat(health.getDetails()).containsKey("uptime-ms");
        assertThat(health.getDetails()).containsKey("checked-at");
        assertThat(health.getDetails().get("application")).isEqualTo("Running normally");
    }

    @Test
    void health_ShouldIncludeMemoryInformation() {
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getDetails().get("memory-usage-percent")).isNotNull();
        assertThat(health.getDetails().get("used-memory-mb")).isNotNull();
        assertThat(health.getDetails().get("max-memory-mb")).isNotNull();
        
        // Memory usage should be a reasonable percentage
        String memoryUsageStr = (String) health.getDetails().get("memory-usage-percent");
        double memoryUsage = Double.parseDouble(memoryUsageStr);
        assertThat(memoryUsage).isBetween(0.0, 100.0);
    }

    @Test
    void health_ShouldIncludeUptimeInformation() {
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getDetails().get("uptime-ms")).isNotNull();
        
        // Uptime should be positive
        Long uptime = (Long) health.getDetails().get("uptime-ms");
        assertThat(uptime).isPositive();
    }

    @Test
    void health_ShouldIncludeTimestamp() {
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getDetails().get("checked-at")).isNotNull();
        String timestamp = (String) health.getDetails().get("checked-at");
        assertThat(timestamp).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*");
    }
}