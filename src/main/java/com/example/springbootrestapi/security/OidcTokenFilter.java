package com.example.springbootrestapi.security;

import com.example.springbootrestapi.service.OidcTokenValidator;
import com.example.springbootrestapi.service.SessionJwtService;
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
 * Filter for processing initial OIDC access tokens.
 * This filter validates opaque access tokens with the OIDC provider
 * and creates session JWTs for subsequent requests.
 */
@Component
public class OidcTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(OidcTokenFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final OidcTokenValidator oidcTokenValidator;
    private final SessionJwtService sessionJwtService;

    public OidcTokenFilter(OidcTokenValidator oidcTokenValidator, SessionJwtService sessionJwtService) {
        this.oidcTokenValidator = oidcTokenValidator;
        this.sessionJwtService = sessionJwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        // Skip processing if no authorization header or already authenticated
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX) || 
            SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        
        // Skip if this looks like a session JWT (contains dots indicating JWT structure)
        // OIDC access tokens are typically opaque (no dots)
        if (isSessionJwt(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Validate OIDC access token and extract user info
            OidcTokenValidator.UserInfo userInfo = oidcTokenValidator.validateTokenAndExtractUserInfo(token);
            
            // Create session JWT
            String sessionJwt = sessionJwtService.createSessionJwt(
                userInfo.getUserId(),
                userInfo.getEmail(),
                userInfo.getName(),
                userInfo.getRoles()
            );
            
            // Set up authentication in security context
            List<SimpleGrantedAuthority> authorities = userInfo.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
            
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(userInfo, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Add session JWT to response header for client to use in subsequent requests
            response.setHeader("X-Session-Token", sessionJwt);
            
            logger.debug("Successfully processed OIDC token for user: {}", userInfo.getUserId());
            
        } catch (Exception e) {
            logger.error("OIDC token validation failed: {}", e.getMessage());
            // Don't set authentication - let the request proceed unauthenticated
            // The security configuration will handle the 401 response
        }
        
        filterChain.doFilter(request, response);
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
}