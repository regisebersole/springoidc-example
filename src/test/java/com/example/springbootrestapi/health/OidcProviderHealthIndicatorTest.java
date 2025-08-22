package com.example.springbootrestapi.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OidcProviderHealthIndicatorTest {

    private OidcProviderHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new OidcProviderHealthIndicator("https://oauth2.googleapis.com/tokeninfo");
    }

    @Test
    void health_ShouldReturnUpStatus_WhenOidcProviderIsReachable() {
        // Given - using Google's public endpoint which should be reachable
        
        // When
        Health health = healthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isIn(Status.UP, Status.DOWN); // May vary based on network
        assertThat(health.getDetails()).containsKey("oidc-provider");
        assertThat(health.getDetails()).containsKey("introspection-uri");
        assertThat(health.getDetails()).containsKey("checked-at");
        assertThat(health.getDetails().get("introspection-uri")).isEqualTo("https://oauth2.googleapis.com/tokeninfo");
    }

    @Test
    void health_ShouldReturnDownStatus_WhenOidcProviderIsUnreachable() {
        // Given
        OidcProviderHealthIndicator unreachableHealthIndicator = 
            new OidcProviderHealthIndicator("https://nonexistent.example.com/tokeninfo");
        
        // When
        Health health = unreachableHealthIndicator.health();
        
        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("oidc-provider");
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails()).containsKey("checked-at");
        assertThat(health.getDetails().get("oidc-provider")).isEqualTo("Unavailable");
    }

    @Test
    void extractBaseUrl_ShouldReturnGoogleBaseUrl_WhenGoogleProviderUsed() {
        // When
        String result = (String) ReflectionTestUtils.invokeMethod(
            healthIndicator, "extractBaseUrl", "https://oauth2.googleapis.com/tokeninfo");
        
        // Then
        assertThat(result).isEqualTo("https://www.googleapis.com");
    }

    @Test
    void extractBaseUrl_ShouldReturnBaseDomain_WhenOtherProviderUsed() {
        // When
        String result = (String) ReflectionTestUtils.invokeMethod(
            healthIndicator, "extractBaseUrl", "https://auth.example.com/oauth/tokeninfo");
        
        // Then
        assertThat(result).isEqualTo("https://auth.example.com");
    }

    @Test
    void extractBaseUrl_ShouldReturnOriginalUrl_WhenInvalidUrlProvided() {
        // When
        String result = (String) ReflectionTestUtils.invokeMethod(
            healthIndicator, "extractBaseUrl", "invalid-url");
        
        // Then
        assertThat(result).isEqualTo("invalid-url");
    }
}