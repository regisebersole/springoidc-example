package com.example.springbootrestapi.controller;

import com.example.springbootrestapi.model.AuthRequest;
import com.example.springbootrestapi.model.AuthResponse;
import com.example.springbootrestapi.service.OidcTokenValidator;
import com.example.springbootrestapi.service.SessionJwtService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authentication endpoints
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final OidcTokenValidator oidcTokenValidator;
    private final SessionJwtService sessionJwtService;
    private final int inactivityTimeoutSeconds;

    @Autowired
    public AuthController(
            OidcTokenValidator oidcTokenValidator,
            SessionJwtService sessionJwtService,
            @Value("${app.jwt.expiration}") int inactivityTimeoutSeconds) {
        this.oidcTokenValidator = oidcTokenValidator;
        this.sessionJwtService = sessionJwtService;
        this.inactivityTimeoutSeconds = inactivityTimeoutSeconds;
    }

    /**
     * Token exchange endpoint - accepts OIDC access token and returns session JWT
     * 
     * @param authRequest Request containing the OIDC access token
     * @return AuthResponse containing session JWT and user information
     */
    @PostMapping("/token-exchange")
    public ResponseEntity<AuthResponse> exchangeToken(@Valid @RequestBody AuthRequest authRequest) {
        try {
            logger.info("Processing token exchange request");

            // Validate the OIDC access token and extract user information
            OidcTokenValidator.UserInfo userInfo = oidcTokenValidator.validateTokenAndExtractUserInfo(
                    authRequest.getAccessToken());

            // Create session JWT with user information
            String sessionJwt = sessionJwtService.createSessionJwt(
                    userInfo.getUserId(),
                    userInfo.getEmail(),
                    userInfo.getName(),
                    userInfo.getRoles()
            );

            // Create response with session token and user info
            AuthResponse.UserInfo responseUserInfo = new AuthResponse.UserInfo(
                    userInfo.getUserId(),
                    userInfo.getEmail(),
                    userInfo.getName(),
                    userInfo.getRoles()
            );

            AuthResponse response = new AuthResponse(
                    sessionJwt,
                    responseUserInfo,
                    (long) inactivityTimeoutSeconds
            );

            logger.info("Successfully exchanged token for user: {}", userInfo.getUserId());
            return ResponseEntity.ok(response);

        } catch (OidcTokenValidator.TokenValidationException e) {
            logger.warn("Token validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Token validation failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error during token exchange: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error"));
        }
    }

    /**
     * Endpoint to refresh session JWT by updating activity timestamp
     * 
     * @param authorizationHeader Authorization header containing the session JWT
     * @return AuthResponse with updated session JWT
     */
    @PostMapping("/refresh-session")
    public ResponseEntity<AuthResponse> refreshSession(
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            String sessionJwt = extractTokenFromHeader(authorizationHeader);
            if (sessionJwt == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Invalid authorization header"));
            }

            // Update session activity and get new JWT
            String updatedJwt = sessionJwtService.updateSessionActivity(sessionJwt);

            // Extract user info from the updated JWT for response
            String userId = sessionJwtService.extractUserId(updatedJwt);
            
            AuthResponse response = new AuthResponse(
                    updatedJwt,
                    null, // User info not needed for refresh
                    (long) inactivityTimeoutSeconds
            );

            logger.debug("Successfully refreshed session for user: {}", userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.warn("Session refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Session refresh failed: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint for authentication service
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Authentication service is healthy");
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    /**
     * Create error response for authentication failures
     */
    private AuthResponse createErrorResponse(String errorMessage) {
        // For error responses, we'll return null values and let the client handle the error
        // The actual error message will be in the HTTP response body
        return new AuthResponse(null, null, null);
    }
}