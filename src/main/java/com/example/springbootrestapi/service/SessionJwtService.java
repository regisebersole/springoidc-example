package com.example.springbootrestapi.service;

import com.example.springbootrestapi.model.SessionClaims;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Service for creating and validating session JWTs with timeout logic
 */
@Service
public class SessionJwtService {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionJwtService.class);
    
    private final SecretKey secretKey;
    private final int inactivityTimeoutSeconds;
    private final int maxSessionDurationSeconds;
    
    public SessionJwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration}") int inactivityTimeoutSeconds,
            @Value("${app.jwt.max-session-duration}") int maxSessionDurationSeconds) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.inactivityTimeoutSeconds = inactivityTimeoutSeconds;
        this.maxSessionDurationSeconds = maxSessionDurationSeconds;
    }
    
    /**
     * Create a new session JWT with user information and session metadata
     */
    public String createSessionJwt(String userId, String email, String name, List<String> roles) {
        long currentTime = Instant.now().getEpochSecond();
        long expirationTime = currentTime + inactivityTimeoutSeconds;
        
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("name", name)
                .claim("roles", roles)
                .claim("lastActivity", currentTime)
                .claim("sessionStart", currentTime)
                .issuedAt(new Date(currentTime * 1000))
                .expiration(new Date(expirationTime * 1000))
                .signWith(secretKey)
                .compact();
    }
    
    /**
     * Validate and parse session JWT, checking for expiration and timeout policies
     */
    public SessionClaims validateSessionJwt(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            SessionClaims sessionClaims = extractSessionClaims(claims);
            
            // Check inactivity timeout
            if (sessionClaims.isInactivityExpired(inactivityTimeoutSeconds)) {
                logger.warn("Session expired due to inactivity for user: {}", sessionClaims.getSub());
                throw new JwtException("Session expired due to inactivity");
            }
            
            // Check maximum session duration
            if (sessionClaims.isMaxDurationExpired(maxSessionDurationSeconds)) {
                logger.warn("Session expired due to maximum duration for user: {}", sessionClaims.getSub());
                throw new JwtException("Session expired due to maximum duration");
            }
            
            logger.debug("Successfully validated session JWT for user: {}", sessionClaims.getSub());
            return sessionClaims;
            
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token has expired: {}", e.getMessage());
            throw new JwtException("JWT token has expired", e);
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
            throw new JwtException("JWT token is unsupported", e);
        } catch (MalformedJwtException e) {
            logger.error("JWT token is malformed: {}", e.getMessage());
            throw new JwtException("JWT token is malformed", e);
        } catch (SecurityException e) {
            logger.error("JWT signature validation failed: {}", e.getMessage());
            throw new JwtException("JWT signature validation failed", e);
        } catch (IllegalArgumentException e) {
            logger.error("JWT token is null or empty: {}", e.getMessage());
            throw new JwtException("JWT token is null or empty", e);
        }
    }
    
    /**
     * Update session JWT with new activity timestamp
     */
    public String updateSessionActivity(String token) {
        SessionClaims claims = validateSessionJwt(token);
        claims.updateLastActivity();
        
        // Create new JWT with updated activity time
        long expirationTime = claims.getLastActivity() + inactivityTimeoutSeconds;
        
        return Jwts.builder()
                .subject(claims.getSub())
                .claim("email", claims.getEmail())
                .claim("name", claims.getName())
                .claim("roles", claims.getRoles())
                .claim("lastActivity", claims.getLastActivity())
                .claim("sessionStart", claims.getSessionStart())
                .issuedAt(new Date(claims.getIat() * 1000))
                .expiration(new Date(expirationTime * 1000))
                .signWith(secretKey)
                .compact();
    }
    
    /**
     * Extract session claims from JWT claims
     */
    private SessionClaims extractSessionClaims(Claims claims) {
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        
        return new SessionClaims(
                claims.getSubject(),
                claims.get("email", String.class),
                claims.get("name", String.class),
                roles,
                claims.getIssuedAt().toInstant().getEpochSecond(),
                claims.getExpiration().toInstant().getEpochSecond(),
                claims.get("lastActivity", Long.class),
                claims.get("sessionStart", Long.class)
        );
    }
    
    /**
     * Check if a JWT token is expired without full validation
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Extract user ID from JWT token without full validation
     */
    public String extractUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getSubject();
        } catch (Exception e) {
            logger.error("Failed to extract user ID from token: {}", e.getMessage());
            return null;
        }
    }
}