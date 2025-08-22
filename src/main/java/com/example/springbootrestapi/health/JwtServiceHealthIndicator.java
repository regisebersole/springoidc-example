package com.example.springbootrestapi.health;

import com.example.springbootrestapi.service.SessionJwtService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Custom health indicator for JWT service functionality
 */
@Component
public class JwtServiceHealthIndicator implements HealthIndicator {

    private final SessionJwtService sessionJwtService;

    public JwtServiceHealthIndicator(SessionJwtService sessionJwtService) {
        this.sessionJwtService = sessionJwtService;
    }

    @Override
    public Health health() {
        try {
            // Test JWT service by creating and validating a test token
            String testUserId = "health-check-user";
            String testEmail = "healthcheck@example.com";
            String testName = "Health Check User";
            
            // Create a test JWT
            String testJwt = sessionJwtService.createSessionJwt(testUserId, testEmail, testName, null);
            
            // Validate the test JWT
            sessionJwtService.validateSessionJwt(testJwt);
            
            // If we get here without exception, validation succeeded
            return Health.up()
                    .withDetail("jwt-service", "Available")
                    .withDetail("can-create-tokens", true)
                    .withDetail("can-validate-tokens", true)
                    .withDetail("checked-at", Instant.now().toString())
                    .build();
            
        } catch (Exception e) {
            return Health.down()
                    .withDetail("jwt-service", "Error")
                    .withDetail("error", e.getMessage())
                    .withDetail("checked-at", Instant.now().toString())
                    .build();
        }
    }
}