package com.example.springbootrestapi.service;

import com.example.springbootrestapi.model.SessionClaims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SessionJwtServiceTest {

    private SessionJwtService sessionJwtService;
    private final String testSecret = "testSecretKeyThatIsLongEnoughForHmacSha256Algorithm";
    private final int inactivityTimeout = 1200; // 20 minutes
    private final int maxSessionDuration = 86400; // 24 hours

    @BeforeEach
    void setUp() {
        sessionJwtService = new SessionJwtService(testSecret, inactivityTimeout, maxSessionDuration);
    }

    @Test
    void createSessionJwt_ShouldCreateValidJwt() {
        // Given
        String userId = "user123";
        String email = "test@example.com";
        String name = "Test User";
        List<String> roles = Arrays.asList("USER", "ADMIN");

        // When
        String jwt = sessionJwtService.createSessionJwt(userId, email, name, roles);

        // Then
        assertNotNull(jwt);
        assertFalse(jwt.isEmpty());
        assertTrue(jwt.contains("."));
    }

    @Test
    void validateSessionJwt_WithValidToken_ShouldReturnSessionClaims() {
        // Given
        String userId = "user123";
        String email = "test@example.com";
        String name = "Test User";
        List<String> roles = Arrays.asList("USER", "ADMIN");
        String jwt = sessionJwtService.createSessionJwt(userId, email, name, roles);

        // When
        SessionClaims claims = sessionJwtService.validateSessionJwt(jwt);

        // Then
        assertNotNull(claims);
        assertEquals(userId, claims.getSub());
        assertEquals(email, claims.getEmail());
        assertEquals(name, claims.getName());
        assertEquals(roles, claims.getRoles());
        assertNotNull(claims.getIat());
        assertNotNull(claims.getExp());
        assertNotNull(claims.getLastActivity());
        assertNotNull(claims.getSessionStart());
    }

    @Test
    void validateSessionJwt_WithInvalidToken_ShouldThrowJwtException() {
        // Given
        String invalidJwt = "invalid.jwt.token";

        // When & Then
        assertThrows(JwtException.class, () -> sessionJwtService.validateSessionJwt(invalidJwt));
    }

    @Test
    void validateSessionJwt_WithNullToken_ShouldThrowJwtException() {
        // When & Then
        assertThrows(JwtException.class, () -> sessionJwtService.validateSessionJwt(null));
    }

    @Test
    void validateSessionJwt_WithEmptyToken_ShouldThrowJwtException() {
        // When & Then
        assertThrows(JwtException.class, () -> sessionJwtService.validateSessionJwt(""));
    }

    @Test
    void updateSessionActivity_ShouldUpdateLastActivityTime() throws InterruptedException {
        // Given
        String userId = "user123";
        String email = "test@example.com";
        String name = "Test User";
        List<String> roles = Arrays.asList("USER");
        String originalJwt = sessionJwtService.createSessionJwt(userId, email, name, roles);
        
        // Wait a moment to ensure different timestamps
        Thread.sleep(1000);

        // When
        String updatedJwt = sessionJwtService.updateSessionActivity(originalJwt);

        // Then
        assertNotNull(updatedJwt);
        assertNotEquals(originalJwt, updatedJwt);
        
        SessionClaims originalClaims = sessionJwtService.validateSessionJwt(originalJwt);
        SessionClaims updatedClaims = sessionJwtService.validateSessionJwt(updatedJwt);
        
        assertTrue(updatedClaims.getLastActivity() > originalClaims.getLastActivity());
        assertEquals(originalClaims.getSessionStart(), updatedClaims.getSessionStart());
    }

    @Test
    void isTokenExpired_WithValidToken_ShouldReturnFalse() {
        // Given
        String jwt = sessionJwtService.createSessionJwt("user123", "test@example.com", "Test User", Arrays.asList("USER"));

        // When
        boolean isExpired = sessionJwtService.isTokenExpired(jwt);

        // Then
        assertFalse(isExpired);
    }

    @Test
    void isTokenExpired_WithInvalidToken_ShouldReturnTrue() {
        // Given
        String invalidJwt = "invalid.jwt.token";

        // When
        boolean isExpired = sessionJwtService.isTokenExpired(invalidJwt);

        // Then
        assertTrue(isExpired);
    }

    @Test
    void extractUserId_WithValidToken_ShouldReturnUserId() {
        // Given
        String userId = "user123";
        String jwt = sessionJwtService.createSessionJwt(userId, "test@example.com", "Test User", Arrays.asList("USER"));

        // When
        String extractedUserId = sessionJwtService.extractUserId(jwt);

        // Then
        assertEquals(userId, extractedUserId);
    }

    @Test
    void extractUserId_WithInvalidToken_ShouldReturnNull() {
        // Given
        String invalidJwt = "invalid.jwt.token";

        // When
        String extractedUserId = sessionJwtService.extractUserId(invalidJwt);

        // Then
        assertNull(extractedUserId);
    }

    @Test
    void validateSessionJwt_WithExpiredInactivityTimeout_ShouldThrowJwtException() {
        // Given - Create a service with very short timeout for testing
        SessionJwtService shortTimeoutService = new SessionJwtService(testSecret, 1, maxSessionDuration); // 1 second timeout
        String jwt = shortTimeoutService.createSessionJwt("user123", "test@example.com", "Test User", Arrays.asList("USER"));
        
        // Wait for inactivity timeout
        try {
            Thread.sleep(2000); // 2 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When & Then
        JwtException exception = assertThrows(JwtException.class, () -> shortTimeoutService.validateSessionJwt(jwt));
        // The JWT will expire due to standard expiration, which is expected behavior
        assertTrue(exception.getMessage().contains("expired") || exception.getMessage().contains("inactivity"));
    }

    @Test
    void validateSessionJwt_WithExpiredMaxDuration_ShouldThrowJwtException() {
        // Given - Create a service with very short max duration for testing
        SessionJwtService shortDurationService = new SessionJwtService(testSecret, inactivityTimeout, 1); // 1 second max duration
        String jwt = shortDurationService.createSessionJwt("user123", "test@example.com", "Test User", Arrays.asList("USER"));
        
        // Wait for max duration timeout
        try {
            Thread.sleep(2000); // 2 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When & Then
        JwtException exception = assertThrows(JwtException.class, () -> shortDurationService.validateSessionJwt(jwt));
        assertTrue(exception.getMessage().contains("maximum duration"));
    }

    @Test
    void createSessionJwt_ShouldSetCorrectTimestamps() {
        // Given
        long beforeCreation = Instant.now().getEpochSecond();
        
        // When
        String jwt = sessionJwtService.createSessionJwt("user123", "test@example.com", "Test User", Arrays.asList("USER"));
        
        // Then
        SessionClaims claims = sessionJwtService.validateSessionJwt(jwt);
        long afterCreation = Instant.now().getEpochSecond();
        
        assertTrue(claims.getIat() >= beforeCreation);
        assertTrue(claims.getIat() <= afterCreation);
        assertTrue(claims.getLastActivity() >= beforeCreation);
        assertTrue(claims.getLastActivity() <= afterCreation);
        assertTrue(claims.getSessionStart() >= beforeCreation);
        assertTrue(claims.getSessionStart() <= afterCreation);
        assertEquals(claims.getIat(), claims.getLastActivity());
        assertEquals(claims.getIat(), claims.getSessionStart());
        assertEquals(claims.getExp(), claims.getIat() + inactivityTimeout);
    }
}