package com.example.springbootrestapi.security;

import com.example.springbootrestapi.model.SessionClaims;
import com.example.springbootrestapi.service.SessionJwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionJwtFilterTest {

    @Mock
    private SessionJwtService sessionJwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Authentication existingAuthentication;

    private SessionJwtFilter sessionJwtFilter;

    @BeforeEach
    void setUp() {
        sessionJwtFilter = new SessionJwtFilter(sessionJwtService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSkipProcessingWhenAlreadyAuthenticated() throws ServletException, IOException {
        // Given
        SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

        // When
        sessionJwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(sessionJwtService, never()).validateSessionJwt(any());
        verify(sessionJwtService, never()).updateSessionActivity(any());
    }

    @Test
    void shouldSkipProcessingWhenNoSessionToken() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-Session-Token")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        sessionJwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(sessionJwtService, never()).validateSessionJwt(any());
        verify(sessionJwtService, never()).updateSessionActivity(any());
    }

    @Test
    void shouldSkipProcessingWhenTokenIsNotJwt() throws ServletException, IOException {
        // Given
        when(request.getHeader("X-Session-Token")).thenReturn("opaque-token");

        // When
        sessionJwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(sessionJwtService, never()).validateSessionJwt(any());
        verify(sessionJwtService, never()).updateSessionActivity(any());
    }

    @Test
    void shouldProcessSessionJwtFromSessionTokenHeader() throws ServletException, IOException {
        // Given
        String sessionJwt = "session.jwt.token";
        String updatedJwt = "updated.jwt.token";
        List<String> roles = Arrays.asList("USER", "ADMIN");
        
        SessionClaims sessionClaims = new SessionClaims(
            "user123", "user@example.com", "John Doe", roles,
            1234567890L, 1234571490L, 1234567890L, 1234567890L
        );

        when(request.getHeader("X-Session-Token")).thenReturn(sessionJwt);
        when(sessionJwtService.validateSessionJwt(sessionJwt)).thenReturn(sessionClaims);
        when(sessionJwtService.updateSessionActivity(sessionJwt)).thenReturn(updatedJwt);

        // When
        sessionJwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(sessionJwtService).validateSessionJwt(sessionJwt);
        verify(sessionJwtService).updateSessionActivity(sessionJwt);
        verify(response).setHeader("X-Session-Token", updatedJwt);
        verify(filterChain).doFilter(request, response);
        
        // Verify authentication was set in security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        assert auth.getPrincipal() instanceof SessionJwtFilter.SessionPrincipal;
        assert auth.getAuthorities().size() == 2;
        
        SessionJwtFilter.SessionPrincipal principal = (SessionJwtFilter.SessionPrincipal) auth.getPrincipal();
        assert "user123".equals(principal.getUserId());
        assert "user@example.com".equals(principal.getEmail());
        assert "John Doe".equals(principal.getName());
    }

    @Test
    void shouldProcessSessionJwtFromAuthorizationHeader() throws ServletException, IOException {
        // Given
        String sessionJwt = "session.jwt.token";
        String updatedJwt = "updated.jwt.token";
        List<String> roles = Arrays.asList("USER");
        
        SessionClaims sessionClaims = new SessionClaims(
            "user456", "user2@example.com", "Jane Doe", roles,
            1234567890L, 1234571490L, 1234567890L, 1234567890L
        );

        when(request.getHeader("X-Session-Token")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + sessionJwt);
        when(sessionJwtService.validateSessionJwt(sessionJwt)).thenReturn(sessionClaims);
        when(sessionJwtService.updateSessionActivity(sessionJwt)).thenReturn(updatedJwt);

        // When
        sessionJwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(sessionJwtService).validateSessionJwt(sessionJwt);
        verify(sessionJwtService).updateSessionActivity(sessionJwt);
        verify(response).setHeader("X-Session-Token", updatedJwt);
        verify(filterChain).doFilter(request, response);
        
        // Verify authentication was set
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        assert auth.getAuthorities().size() == 1;
    }

    @Test
    void shouldHandleJwtValidationFailure() throws ServletException, IOException {
        // Given
        String invalidJwt = "invalid.jwt.token";
        when(request.getHeader("X-Session-Token")).thenReturn(invalidJwt);
        when(sessionJwtService.validateSessionJwt(invalidJwt))
            .thenThrow(new JwtException("JWT expired"));

        // When
        sessionJwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(sessionJwtService).validateSessionJwt(invalidJwt);
        verify(sessionJwtService, never()).updateSessionActivity(any());
        verify(response, never()).setHeader(eq("X-Session-Token"), any());
        verify(filterChain).doFilter(request, response);
        
        // Verify no authentication was set
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth == null;
    }

    @Test
    void shouldHandleUnexpectedErrors() throws ServletException, IOException {
        // Given
        String sessionJwt = "session.jwt.token";
        when(request.getHeader("X-Session-Token")).thenReturn(sessionJwt);
        when(sessionJwtService.validateSessionJwt(sessionJwt))
            .thenThrow(new RuntimeException("Unexpected error"));

        // When
        sessionJwtFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(sessionJwtService).validateSessionJwt(sessionJwt);
        verify(sessionJwtService, never()).updateSessionActivity(any());
        verify(response, never()).setHeader(eq("X-Session-Token"), any());
        verify(filterChain).doFilter(request, response);
        
        // Verify no authentication was set
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth == null;
    }

    @Test
    void shouldNotFilterPublicEndpoints() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // When
        boolean shouldNotFilter = sessionJwtFilter.shouldNotFilter(request);

        // Then
        assert shouldNotFilter;
    }

    @Test
    void shouldNotFilterSwaggerEndpoints() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/v3/api-docs/swagger-config");

        // When
        boolean shouldNotFilter = sessionJwtFilter.shouldNotFilter(request);

        // Then
        assert shouldNotFilter;
    }

    @Test
    void shouldFilterProtectedEndpoints() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/protected");

        // When
        boolean shouldNotFilter = sessionJwtFilter.shouldNotFilter(request);

        // Then
        assert !shouldNotFilter;
    }

    @Test
    void testSessionPrincipalMethods() {
        // Given
        List<String> roles = Arrays.asList("USER", "ADMIN");
        SessionClaims sessionClaims = new SessionClaims(
            "user123", "user@example.com", "John Doe", roles,
            1234567890L, 1234571490L, 1234567890L, 1234567890L
        );

        // When
        SessionJwtFilter.SessionPrincipal principal = new SessionJwtFilter.SessionPrincipal(sessionClaims);

        // Then
        assert "user123".equals(principal.getUserId());
        assert "user@example.com".equals(principal.getEmail());
        assert "John Doe".equals(principal.getName());
        assert roles.equals(principal.getRoles());
        assert sessionClaims == principal.getSessionClaims();
        assert principal.toString().contains("user123");
        assert principal.toString().contains("user@example.com");
    }
}