package com.example.springbootrestapi.health;

import com.example.springbootrestapi.service.SessionJwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceHealthIndicatorTest {

    @Mock
    private SessionJwtService sessionJwtService;

    private JwtServiceHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new JwtServiceHealthIndicator(sessionJwtService);
    }

    @Test
    void health_ShouldReturnUpStatus_WhenJwtServiceIsWorking() {
        // Given
        String testJwt = "test.jwt.token";
        when(sessionJwtService.createSessionJwt(anyString(), anyString(), anyString(), any()))
            .thenReturn(testJwt);
        when(sessionJwtService.validateSessionJwt(testJwt)).thenReturn(new com.example.springbootrestapi.model.SessionClaims("test", "test@example.com", "Test User", null, 0L, 0L, 0L, 0L));
        
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("jwt-service");
        assertThat(health.getDetails()).containsKey("can-create-tokens");
        assertThat(health.getDetails()).containsKey("can-validate-tokens");
        assertThat(health.getDetails()).containsKey("checked-at");
        assertThat(health.getDetails().get("jwt-service")).isEqualTo("Available");
        assertThat(health.getDetails().get("can-create-tokens")).isEqualTo(true);
        assertThat(health.getDetails().get("can-validate-tokens")).isEqualTo(true);
    }

    @Test
    void health_ShouldReturnDownStatus_WhenJwtValidationFails() {
        // Given
        String testJwt = "test.jwt.token";
        when(sessionJwtService.createSessionJwt(anyString(), anyString(), anyString(), any()))
            .thenReturn(testJwt);
        when(sessionJwtService.validateSessionJwt(testJwt)).thenThrow(new RuntimeException("Token validation failed"));
        
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("jwt-service");
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails()).containsKey("checked-at");
        assertThat(health.getDetails().get("jwt-service")).isEqualTo("Error");
        assertThat(health.getDetails().get("error")).isEqualTo("Token validation failed");
    }

    @Test
    void health_ShouldReturnDownStatus_WhenJwtCreationFails() {
        // Given
        when(sessionJwtService.createSessionJwt(anyString(), anyString(), anyString(), any()))
            .thenThrow(new RuntimeException("JWT creation failed"));
        
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("jwt-service");
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails()).containsKey("checked-at");
        assertThat(health.getDetails().get("jwt-service")).isEqualTo("Error");
        assertThat(health.getDetails().get("error")).isEqualTo("JWT creation failed");
    }
}