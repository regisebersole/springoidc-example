package com.example.springbootrestapi.security;

import com.example.springbootrestapi.service.OidcTokenValidator;
import com.example.springbootrestapi.service.SessionJwtService;
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
class OidcTokenFilterTest {

    @Mock
    private OidcTokenValidator oidcTokenValidator;

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

    private OidcTokenFilter oidcTokenFilter;

    @BeforeEach
    void setUp() {
        oidcTokenFilter = new OidcTokenFilter(oidcTokenValidator, sessionJwtService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSkipProcessingWhenNoAuthorizationHeader() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn(null);

        // When
        oidcTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(oidcTokenValidator, never()).validateTokenAndExtractUserInfo(any());
        verify(sessionJwtService, never()).createSessionJwt(any(), any(), any(), any());
    }

    @Test
    void shouldSkipProcessingWhenNotBearerToken() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

        // When
        oidcTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(oidcTokenValidator, never()).validateTokenAndExtractUserInfo(any());
        verify(sessionJwtService, never()).createSessionJwt(any(), any(), any(), any());
    }

    @Test
    void shouldSkipProcessingWhenAlreadyAuthenticated() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer opaque-token");
        SecurityContextHolder.getContext().setAuthentication(existingAuthentication);

        // When
        oidcTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(oidcTokenValidator, never()).validateTokenAndExtractUserInfo(any());
        verify(sessionJwtService, never()).createSessionJwt(any(), any(), any(), any());
    }

    @Test
    void shouldSkipProcessingWhenTokenLooksLikeJwt() throws ServletException, IOException {
        // Given
        when(request.getHeader("Authorization")).thenReturn("Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ");

        // When
        oidcTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(oidcTokenValidator, never()).validateTokenAndExtractUserInfo(any());
        verify(sessionJwtService, never()).createSessionJwt(any(), any(), any(), any());
    }

    @Test
    void shouldProcessOidcTokenSuccessfully() throws ServletException, IOException {
        // Given
        String opaqueToken = "opaque-access-token";
        String sessionJwt = "session.jwt.token";
        List<String> roles = Arrays.asList("USER", "ADMIN");
        
        OidcTokenValidator.UserInfo userInfo = new OidcTokenValidator.UserInfo(
            "user123", "user@example.com", "John Doe", roles
        );

        when(request.getHeader("Authorization")).thenReturn("Bearer " + opaqueToken);
        when(oidcTokenValidator.validateTokenAndExtractUserInfo(opaqueToken)).thenReturn(userInfo);
        when(sessionJwtService.createSessionJwt("user123", "user@example.com", "John Doe", roles))
            .thenReturn(sessionJwt);

        // When
        oidcTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(oidcTokenValidator).validateTokenAndExtractUserInfo(opaqueToken);
        verify(sessionJwtService).createSessionJwt("user123", "user@example.com", "John Doe", roles);
        verify(response).setHeader("X-Session-Token", sessionJwt);
        verify(filterChain).doFilter(request, response);
        
        // Verify authentication was set in security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        assert auth.getPrincipal() == userInfo;
        assert auth.getAuthorities().size() == 2;
    }

    @Test
    void shouldHandleTokenValidationFailure() throws ServletException, IOException {
        // Given
        String opaqueToken = "invalid-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + opaqueToken);
        when(oidcTokenValidator.validateTokenAndExtractUserInfo(opaqueToken))
            .thenThrow(new OidcTokenValidator.TokenValidationException("Invalid token"));

        // When
        oidcTokenFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(oidcTokenValidator).validateTokenAndExtractUserInfo(opaqueToken);
        verify(sessionJwtService, never()).createSessionJwt(any(), any(), any(), any());
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
        boolean shouldNotFilter = oidcTokenFilter.shouldNotFilter(request);

        // Then
        assert shouldNotFilter;
    }

    @Test
    void shouldNotFilterSwaggerEndpoints() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/swagger-ui/index.html");

        // When
        boolean shouldNotFilter = oidcTokenFilter.shouldNotFilter(request);

        // Then
        assert shouldNotFilter;
    }

    @Test
    void shouldFilterProtectedEndpoints() throws ServletException {
        // Given
        when(request.getRequestURI()).thenReturn("/api/protected");

        // When
        boolean shouldNotFilter = oidcTokenFilter.shouldNotFilter(request);

        // Then
        assert !shouldNotFilter;
    }
}