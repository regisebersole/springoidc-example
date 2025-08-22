package com.example.springbootrestapi.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;

/**
 * Custom health indicator for OIDC provider connectivity
 */
@Component
public class OidcProviderHealthIndicator implements HealthIndicator {

    private final String introspectionUri;
    private final RestTemplate restTemplate;

    public OidcProviderHealthIndicator(
            @Value("${spring.security.oauth2.resourceserver.opaque-token.introspection-uri}") String introspectionUri) {
        this.introspectionUri = introspectionUri;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public Health health() {
        try {
            Instant start = Instant.now();
            
            // Try to reach the OIDC provider endpoint
            // We don't need to make a full request, just check connectivity
            String baseUrl = extractBaseUrl(introspectionUri);
            restTemplate.getForEntity(baseUrl, String.class);
            
            Duration responseTime = Duration.between(start, Instant.now());
            
            return Health.up()
                    .withDetail("oidc-provider", "Available")
                    .withDetail("introspection-uri", introspectionUri)
                    .withDetail("response-time-ms", responseTime.toMillis())
                    .withDetail("checked-at", Instant.now().toString())
                    .build();
                    
        } catch (RestClientException e) {
            return Health.down()
                    .withDetail("oidc-provider", "Unavailable")
                    .withDetail("introspection-uri", introspectionUri)
                    .withDetail("error", e.getMessage())
                    .withDetail("checked-at", Instant.now().toString())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("oidc-provider", "Error")
                    .withDetail("introspection-uri", introspectionUri)
                    .withDetail("error", "Unexpected error: " + e.getMessage())
                    .withDetail("checked-at", Instant.now().toString())
                    .build();
        }
    }

    private String extractBaseUrl(String fullUrl) {
        try {
            // Extract base URL for connectivity check
            if (fullUrl.contains("googleapis.com")) {
                return "https://www.googleapis.com";
            }
            // For other providers, extract the base domain
            String[] parts = fullUrl.split("/");
            if (parts.length >= 3) {
                return parts[0] + "//" + parts[2];
            }
            return fullUrl;
        } catch (Exception e) {
            return fullUrl;
        }
    }
}