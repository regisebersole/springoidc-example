package com.example.springbootrestapi.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for custom application metrics
 */
@Configuration
public class MetricsConfig {

    /**
     * Counter for authentication attempts
     */
    @Bean
    public Counter authenticationAttemptCounter(MeterRegistry meterRegistry) {
        return Counter.builder("authentication.attempts")
                .description("Number of authentication attempts")
                .tag("type", "total")
                .register(meterRegistry);
    }

    /**
     * Counter for successful authentications
     */
    @Bean
    public Counter authenticationSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("authentication.success")
                .description("Number of successful authentications")
                .register(meterRegistry);
    }

    /**
     * Counter for failed authentications
     */
    @Bean
    public Counter authenticationFailureCounter(MeterRegistry meterRegistry) {
        return Counter.builder("authentication.failure")
                .description("Number of failed authentications")
                .register(meterRegistry);
    }

    /**
     * Timer for JWT token creation
     */
    @Bean
    public Timer jwtCreationTimer(MeterRegistry meterRegistry) {
        return Timer.builder("jwt.creation.time")
                .description("Time taken to create JWT tokens")
                .register(meterRegistry);
    }

    /**
     * Timer for JWT token validation
     */
    @Bean
    public Timer jwtValidationTimer(MeterRegistry meterRegistry) {
        return Timer.builder("jwt.validation.time")
                .description("Time taken to validate JWT tokens")
                .register(meterRegistry);
    }

    /**
     * Counter for API requests
     */
    @Bean
    public Counter apiRequestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("api.requests")
                .description("Number of API requests")
                .register(meterRegistry);
    }
}