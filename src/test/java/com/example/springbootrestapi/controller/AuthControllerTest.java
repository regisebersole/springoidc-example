package com.example.springbootrestapi.controller;

import com.example.springbootrestapi.model.AuthRequest;
import com.example.springbootrestapi.model.AuthResponse;
import com.example.springbootrestapi.service.OidcTokenValidator;
import com.example.springbootrestapi.service.SessionJwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthController
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private OidcTokenValidator oidcTokenValidator;

    @Mock
    private SessionJwtService sessionJwtService;

    private AuthController authController;

    private OidcTokenValidator.UserInfo mockUserInfo;
    private String validAccessToken;
    private String validSessionJwt;
    private int inactivityTimeoutSeconds = 1200;

    @BeforeEach
    void setUp() {
        validAccessToken = "valid-oidc-access-token";
        validSessionJwt = "valid-session-jwt-token";
        
        List<String> roles = Arrays.asList("USER", "ADMIN");
        mockUserInfo = new OidcTokenValidator.UserInfo(
                "user123",
                "user@example.com",
                "John Doe",
                roles
        );
        
        // Manually create the controller with mocked dependencies
        authController = new AuthController(oidcTokenValidator, sessionJwtService, inactivityTimeoutSeconds);
    }

    @Test
    void tokenExchange_WithValidToken_ShouldReturnSessionJwt() {
        // Arrange
        AuthRequest request = new AuthRequest(validAccessToken);
        
        when(oidcTokenValidator.validateTokenAndExtractUserInfo(validAccessToken))
                .thenReturn(mockUserInfo);
        when(sessionJwtService.createSessionJwt(
                eq("user123"),
                eq("user@example.com"),
                eq("John Doe"),
                eq(Arrays.asList("USER", "ADMIN"))
        )).thenReturn(validSessionJwt);

        // Act
        ResponseEntity<AuthResponse> response = authController.exchangeToken(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(validSessionJwt, response.getBody().getSessionToken());
        assertEquals("user123", response.getBody().getUser().getUserId());
        assertEquals("user@example.com", response.getBody().getUser().getEmail());
        assertEquals("John Doe", response.getBody().getUser().getName());
        assertEquals(Arrays.asList("USER", "ADMIN"), response.getBody().getUser().getRoles());
        assertEquals(Long.valueOf(inactivityTimeoutSeconds), response.getBody().getExpiresIn());

        verify(oidcTokenValidator).validateTokenAndExtractUserInfo(validAccessToken);
        verify(sessionJwtService).createSessionJwt(
                "user123", "user@example.com", "John Doe", Arrays.asList("USER", "ADMIN"));
    }

    @Test
    void tokenExchange_WithInvalidToken_ShouldReturnUnauthorized() {
        // Arrange
        AuthRequest request = new AuthRequest("invalid-token");
        
        when(oidcTokenValidator.validateTokenAndExtractUserInfo("invalid-token"))
                .thenThrow(new OidcTokenValidator.TokenValidationException("Invalid token"));

        // Act
        ResponseEntity<AuthResponse> response = authController.exchangeToken(request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        verify(oidcTokenValidator).validateTokenAndExtractUserInfo("invalid-token");
        verify(sessionJwtService, never()).createSessionJwt(anyString(), anyString(), anyString(), anyList());
    }

    @Test
    void tokenExchange_WithServiceException_ShouldReturnInternalServerError() {
        // Arrange
        AuthRequest request = new AuthRequest(validAccessToken);
        
        when(oidcTokenValidator.validateTokenAndExtractUserInfo(validAccessToken))
                .thenReturn(mockUserInfo);
        when(sessionJwtService.createSessionJwt(anyString(), anyString(), anyString(), anyList()))
                .thenThrow(new RuntimeException("Unexpected service error"));

        // Act
        ResponseEntity<AuthResponse> response = authController.exchangeToken(request);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        verify(oidcTokenValidator).validateTokenAndExtractUserInfo(validAccessToken);
        verify(sessionJwtService).createSessionJwt(anyString(), anyString(), anyString(), anyList());
    }

    @Test
    void refreshSession_WithValidToken_ShouldReturnUpdatedJwt() {
        // Arrange
        String updatedJwt = "updated-session-jwt";
        String authorizationHeader = "Bearer " + validSessionJwt;
        
        when(sessionJwtService.updateSessionActivity(validSessionJwt))
                .thenReturn(updatedJwt);
        when(sessionJwtService.extractUserId(updatedJwt))
                .thenReturn("user123");

        // Act
        ResponseEntity<AuthResponse> response = authController.refreshSession(authorizationHeader);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(updatedJwt, response.getBody().getSessionToken());
        assertEquals(Long.valueOf(inactivityTimeoutSeconds), response.getBody().getExpiresIn());

        verify(sessionJwtService).updateSessionActivity(validSessionJwt);
        verify(sessionJwtService).extractUserId(updatedJwt);
    }

    @Test
    void refreshSession_WithInvalidToken_ShouldReturnUnauthorized() {
        // Arrange
        String authorizationHeader = "Bearer invalid-token";
        when(sessionJwtService.updateSessionActivity(anyString()))
                .thenThrow(new RuntimeException("Invalid session token"));

        // Act
        ResponseEntity<AuthResponse> response = authController.refreshSession(authorizationHeader);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        verify(sessionJwtService).updateSessionActivity("invalid-token");
    }

    @Test
    void refreshSession_WithoutAuthorizationHeader_ShouldReturnUnauthorized() {
        // Act
        ResponseEntity<AuthResponse> response = authController.refreshSession(null);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        verify(sessionJwtService, never()).updateSessionActivity(anyString());
    }

    @Test
    void refreshSession_WithMalformedAuthorizationHeader_ShouldReturnUnauthorized() {
        // Act
        ResponseEntity<AuthResponse> response = authController.refreshSession("InvalidFormat token");

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        verify(sessionJwtService, never()).updateSessionActivity(anyString());
    }

    @Test
    void health_ShouldReturnOk() {
        // Act
        ResponseEntity<String> response = authController.health();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Authentication service is healthy", response.getBody());
    }

    @Test
    void tokenExchange_WithEmptyToken_ShouldHandleValidation() {
        // This test would require validation to be enabled in the controller
        // For now, we'll test that empty tokens are handled by the service layer
        AuthRequest request = new AuthRequest("");
        
        when(oidcTokenValidator.validateTokenAndExtractUserInfo(""))
                .thenThrow(new OidcTokenValidator.TokenValidationException("Empty token"));

        // Act
        ResponseEntity<AuthResponse> response = authController.exchangeToken(request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(oidcTokenValidator).validateTokenAndExtractUserInfo("");
    }

    @Test
    void tokenExchange_WithNullToken_ShouldHandleValidation() {
        // This test would require validation to be enabled in the controller
        // For now, we'll test that null tokens are handled by the service layer
        AuthRequest request = new AuthRequest(null);
        
        when(oidcTokenValidator.validateTokenAndExtractUserInfo(null))
                .thenThrow(new OidcTokenValidator.TokenValidationException("Null token"));

        // Act
        ResponseEntity<AuthResponse> response = authController.exchangeToken(request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(oidcTokenValidator).validateTokenAndExtractUserInfo(null);
    }
}