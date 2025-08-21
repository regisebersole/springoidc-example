package com.example.springbootrestapi.security;

import com.example.springbootrestapi.model.SessionClaims;
import com.example.springbootrestapi.service.SessionJwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter for processing session JWT tokens.
 * This filter validates session JWTs issued by the backend,
 * checks for timeout policies, and updates activity timestamps.
 */
@Component
public class SessionJwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SessionJwtFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String SESSION_TOKEN_HEADER = "X-Session-Token";

    private final SessionJwtService sessionJwtService;

    public SessionJwtFilter(SessionJwtService sessionJwtService) {
        this.sessionJwtService = sessionJwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // Skip processing if already authenticated
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractSessionToken(request);
        
        // Skip if no session token found or token doesn't look like JWT
        if (token == null || !isSessionJwt(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Validate session JWT and check timeout policies
            SessionClaims sessionClaims = sessionJwtService.validateSessionJwt(token);
            
            // Update session activity and create new token with updated timestamp
            String updatedToken = sessionJwtService.updateSessionActivity(token);
            
            // Set up authentication in security context
            List<SimpleGrantedAuthority> authorities = sessionClaims.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
            
            // Create a custom principal object with session claims
            SessionPrincipal principal = new SessionPrincipal(sessionClaims);
            
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Add updated session JWT to response header
            response.setHeader(SESSION_TOKEN_HEADER, updatedToken);
            
            logger.debug("Successfully processed session JWT for user: {}", sessionClaims.getSub());
            
        } catch (JwtException e) {
            logger.warn("Session JWT validation failed: {}", e.getMessage());
            // Don't set authentication - let the request proceed unauthenticated
            // The security configuration will handle the 401 response
        } catch (Exception e) {
            logger.error("Unexpected error processing session JWT: {}", e.getMessage());
            // Don't set authentication for unexpected errors
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Extract session token from request headers.
     * Checks both Authorization header and X-Session-Token header.
     */
    private String extractSessionToken(HttpServletRequest request) {
        // First check X-Session-Token header (preferred for session JWTs)
        String sessionToken = request.getHeader(SESSION_TOKEN_HEADER);
        if (sessionToken != null && !sessionToken.isEmpty()) {
            return sessionToken;
        }
        
        // Fallback to Authorization header
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    /**
     * Check if the token appears to be a session JWT (contains dots)
     * vs an opaque OIDC access token (no dots)
     */
    private boolean isSessionJwt(String token) {
        return token.contains(".");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Skip filter for public endpoints
        return path.startsWith("/actuator/health") ||
               path.startsWith("/actuator/info") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-ui") ||
               path.equals("/swagger-ui.html");
    }

    /**
     * Custom principal class that wraps SessionClaims for authenticated users.
     */
    public static class SessionPrincipal {
        private final SessionClaims sessionClaims;

        public SessionPrincipal(SessionClaims sessionClaims) {
            this.sessionClaims = sessionClaims;
        }

        public SessionClaims getSessionClaims() {
            return sessionClaims;
        }

        public String getUserId() {
            return sessionClaims.getSub();
        }

        public String getEmail() {
            return sessionClaims.getEmail();
        }

        public String getName() {
            return sessionClaims.getName();
        }

        public List<String> getRoles() {
            return sessionClaims.getRoles();
        }

        @Override
        public String toString() {
            return "SessionPrincipal{" +
                    "userId='" + sessionClaims.getSub() + '\'' +
                    ", email='" + sessionClaims.getEmail() + '\'' +
                    ", name='" + sessionClaims.getName() + '\'' +
                    ", roles=" + sessionClaims.getRoles() +
                    '}';
        }
    }
}