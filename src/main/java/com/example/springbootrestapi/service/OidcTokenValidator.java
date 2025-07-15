package com.example.springbootrestapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;

/**
 * Service for validating OIDC opaque access tokens and extracting user information.
 * This service uses Spring Security's OpaqueTokenIntrospector to validate tokens
 * with the OIDC provider and extract user information.
 */
@Service
public class OidcTokenValidator {

    private static final Logger logger = LoggerFactory.getLogger(OidcTokenValidator.class);

    private final OpaqueTokenIntrospector tokenIntrospector;

    @Autowired
    public OidcTokenValidator(OpaqueTokenIntrospector tokenIntrospector) {
        this.tokenIntrospector = tokenIntrospector;
    }

    /**
     * Validates an opaque access token with the OIDC provider and extracts user information.
     * 
     * @param accessToken The opaque access token to validate
     * @return UserInfo containing extracted user information
     * @throws TokenValidationException if token validation fails
     */
    public UserInfo validateTokenAndExtractUserInfo(String accessToken) {
        try {
            logger.debug("Validating OIDC access token");
            
            // Introspect the token with the OIDC provider
            OAuth2AuthenticatedPrincipal principal = tokenIntrospector.introspect(accessToken);
            
            if (principal == null) {
                logger.warn("Token introspection returned null principal");
                throw new TokenValidationException("Token validation failed: invalid token");
            }

            // Extract user information from the principal
            UserInfo userInfo = extractUserInfo(principal);
            
            logger.info("Successfully validated token for user: {}", userInfo.getUserId());
            return userInfo;
            
        } catch (Exception e) {
            logger.error("Token validation failed: {}", e.getMessage());
            throw new TokenValidationException("Token validation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts user information from the OAuth2AuthenticatedPrincipal.
     * 
     * @param principal The authenticated principal from token introspection
     * @return UserInfo containing extracted user information
     */
    private UserInfo extractUserInfo(OAuth2AuthenticatedPrincipal principal) {
        // Extract user ID (subject)
        String userId = principal.getAttribute("sub");
        if (userId == null || userId.isEmpty()) {
            userId = principal.getName(); // Fallback to principal name
        }

        // Extract email
        String email = principal.getAttribute("email");
        
        // Extract name (try different claim names)
        String name = principal.getAttribute("name");
        if (name == null || name.isEmpty()) {
            name = principal.getAttribute("given_name");
            String familyName = principal.getAttribute("family_name");
            if (name != null && familyName != null) {
                name = name + " " + familyName;
            } else if (familyName != null) {
                name = familyName;
            }
        }
        
        // Extract roles/authorities
        List<String> roles = extractRoles(principal);
        
        return new UserInfo(userId, email, name, roles);
    }

    /**
     * Extracts roles from the principal attributes.
     * Looks for roles in various standard claim names.
     * 
     * @param principal The authenticated principal
     * @return List of roles/authorities
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRoles(OAuth2AuthenticatedPrincipal principal) {
        List<String> roles = new ArrayList<>();
        
        // Try different standard claim names for roles
        String[] roleClaims = {"roles", "authorities", "groups", "realm_access.roles"};
        
        for (String claim : roleClaims) {
            Object roleValue = principal.getAttribute(claim);
            if (roleValue instanceof List) {
                List<?> roleList = (List<?>) roleValue;
                for (Object role : roleList) {
                    if (role instanceof String) {
                        roles.add((String) role);
                    }
                }
            } else if (roleValue instanceof String) {
                // Handle comma-separated roles
                String roleString = (String) roleValue;
                String[] roleArray = roleString.split(",");
                for (String role : roleArray) {
                    roles.add(role.trim());
                }
            }
        }
        
        // If no roles found, add a default user role
        if (roles.isEmpty()) {
            roles.add("USER");
        }
        
        return roles;
    }

    /**
     * Model class representing user information extracted from OIDC token.
     */
    public static class UserInfo {
        private final String userId;
        private final String email;
        private final String name;
        private final List<String> roles;

        public UserInfo(String userId, String email, String name, List<String> roles) {
            this.userId = userId;
            this.email = email;
            this.name = name;
            this.roles = roles != null ? new ArrayList<>(roles) : new ArrayList<>();
        }

        public String getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public String getName() {
            return name;
        }

        public List<String> getRoles() {
            return new ArrayList<>(roles);
        }

        @Override
        public String toString() {
            return "UserInfo{" +
                    "userId='" + userId + '\'' +
                    ", email='" + email + '\'' +
                    ", name='" + name + '\'' +
                    ", roles=" + roles +
                    '}';
        }
    }

    /**
     * Exception thrown when token validation fails.
     */
    public static class TokenValidationException extends RuntimeException {
        public TokenValidationException(String message) {
            super(message);
        }

        public TokenValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}