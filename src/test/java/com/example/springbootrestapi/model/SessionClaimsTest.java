package com.example.springbootrestapi.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SessionClaimsTest {

    @Test
    void constructor_ShouldCreateSessionClaimsWithAllFields() {
        // Given
        String sub = "user123";
        String email = "test@example.com";
        String name = "Test User";
        List<String> roles = Arrays.asList("USER", "ADMIN");
        Long iat = Instant.now().getEpochSecond();
        Long exp = iat + 3600;
        Long lastActivity = iat;
        Long sessionStart = iat;

        // When
        SessionClaims claims = new SessionClaims(sub, email, name, roles, iat, exp, lastActivity, sessionStart);

        // Then
        assertEquals(sub, claims.getSub());
        assertEquals(email, claims.getEmail());
        assertEquals(name, claims.getName());
        assertEquals(roles, claims.getRoles());
        assertEquals(iat, claims.getIat());
        assertEquals(exp, claims.getExp());
        assertEquals(lastActivity, claims.getLastActivity());
        assertEquals(sessionStart, claims.getSessionStart());
    }

    @Test
    void isInactivityExpired_WithRecentActivity_ShouldReturnFalse() {
        // Given
        SessionClaims claims = new SessionClaims();
        claims.setLastActivity(Instant.now().getEpochSecond() - 600); // 10 minutes ago
        int inactivityTimeout = 1200; // 20 minutes

        // When
        boolean isExpired = claims.isInactivityExpired(inactivityTimeout);

        // Then
        assertFalse(isExpired);
    }

    @Test
    void isInactivityExpired_WithOldActivity_ShouldReturnTrue() {
        // Given
        SessionClaims claims = new SessionClaims();
        claims.setLastActivity(Instant.now().getEpochSecond() - 1800); // 30 minutes ago
        int inactivityTimeout = 1200; // 20 minutes

        // When
        boolean isExpired = claims.isInactivityExpired(inactivityTimeout);

        // Then
        assertTrue(isExpired);
    }

    @Test
    void isInactivityExpired_WithNullLastActivity_ShouldReturnTrue() {
        // Given
        SessionClaims claims = new SessionClaims();
        claims.setLastActivity(null);
        int inactivityTimeout = 1200;

        // When
        boolean isExpired = claims.isInactivityExpired(inactivityTimeout);

        // Then
        assertTrue(isExpired);
    }

    @Test
    void isMaxDurationExpired_WithRecentSession_ShouldReturnFalse() {
        // Given
        SessionClaims claims = new SessionClaims();
        claims.setSessionStart(Instant.now().getEpochSecond() - 3600); // 1 hour ago
        int maxSessionDuration = 86400; // 24 hours

        // When
        boolean isExpired = claims.isMaxDurationExpired(maxSessionDuration);

        // Then
        assertFalse(isExpired);
    }

    @Test
    void isMaxDurationExpired_WithOldSession_ShouldReturnTrue() {
        // Given
        SessionClaims claims = new SessionClaims();
        claims.setSessionStart(Instant.now().getEpochSecond() - 90000); // 25 hours ago
        int maxSessionDuration = 86400; // 24 hours

        // When
        boolean isExpired = claims.isMaxDurationExpired(maxSessionDuration);

        // Then
        assertTrue(isExpired);
    }

    @Test
    void isMaxDurationExpired_WithNullSessionStart_ShouldReturnTrue() {
        // Given
        SessionClaims claims = new SessionClaims();
        claims.setSessionStart(null);
        int maxSessionDuration = 86400;

        // When
        boolean isExpired = claims.isMaxDurationExpired(maxSessionDuration);

        // Then
        assertTrue(isExpired);
    }

    @Test
    void updateLastActivity_ShouldSetCurrentTimestamp() {
        // Given
        SessionClaims claims = new SessionClaims();
        Long oldActivity = Instant.now().getEpochSecond() - 600; // 10 minutes ago
        claims.setLastActivity(oldActivity);

        // When
        claims.updateLastActivity();

        // Then
        assertNotNull(claims.getLastActivity());
        assertTrue(claims.getLastActivity() > oldActivity);
        // Should be within the last few seconds
        assertTrue(claims.getLastActivity() >= Instant.now().getEpochSecond() - 5);
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        // Given
        SessionClaims claims = new SessionClaims();
        String sub = "user456";
        String email = "user@test.com";
        String name = "Another User";
        List<String> roles = Arrays.asList("ADMIN");
        Long iat = 1234567890L;
        Long exp = 1234571490L;
        Long lastActivity = 1234567890L;
        Long sessionStart = 1234567890L;

        // When
        claims.setSub(sub);
        claims.setEmail(email);
        claims.setName(name);
        claims.setRoles(roles);
        claims.setIat(iat);
        claims.setExp(exp);
        claims.setLastActivity(lastActivity);
        claims.setSessionStart(sessionStart);

        // Then
        assertEquals(sub, claims.getSub());
        assertEquals(email, claims.getEmail());
        assertEquals(name, claims.getName());
        assertEquals(roles, claims.getRoles());
        assertEquals(iat, claims.getIat());
        assertEquals(exp, claims.getExp());
        assertEquals(lastActivity, claims.getLastActivity());
        assertEquals(sessionStart, claims.getSessionStart());
    }
}