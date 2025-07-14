package com.example.springbootrestapi.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT configuration for session token management.
 * Provides beans for JWT signing and validation.
 */
@Configuration
public class JwtConfig {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    @Value("${app.jwt.max-session-duration}")
    private long maxSessionDuration;

    /**
     * Creates the secret key for JWT signing and validation.
     * Uses HMAC-SHA algorithm with the configured secret.
     */
    @Bean
    public SecretKey jwtSecretKey() {
        // Ensure the secret is long enough for HS256
        String secret = jwtSecret.length() >= 32 ? jwtSecret : 
            jwtSecret + "0".repeat(32 - jwtSecret.length());
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns the JWT expiration time in seconds (20 minutes for inactivity timeout).
     */
    public long getJwtExpiration() {
        return jwtExpiration;
    }

    /**
     * Returns the maximum session duration in seconds (24 hours).
     */
    public long getMaxSessionDuration() {
        return maxSessionDuration;
    }
}