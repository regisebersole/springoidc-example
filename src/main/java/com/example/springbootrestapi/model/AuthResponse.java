package com.example.springbootrestapi.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response model for authentication endpoint
 */
@Schema(description = "Response object containing session JWT and user information")
public class AuthResponse {
    
    @Schema(description = "JWT session token for subsequent API calls", 
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String sessionToken;
    
    @Schema(description = "User information extracted from OIDC token")
    private UserInfo user;
    
    @Schema(description = "Session timeout in seconds", example = "1200")
    private Long expiresIn;

    public AuthResponse() {}

    public AuthResponse(String sessionToken, UserInfo user, Long expiresIn) {
        this.sessionToken = sessionToken;
        this.user = user;
        this.expiresIn = expiresIn;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * Nested UserInfo class for response
     */
    @Schema(description = "User information from OIDC token")
    public static class UserInfo {
        @Schema(description = "Unique user identifier", example = "user123")
        private String userId;
        
        @Schema(description = "User email address", example = "user@example.com")
        private String email;
        
        @Schema(description = "User display name", example = "John Doe")
        private String name;
        
        @Schema(description = "User roles and permissions", example = "[\"user\", \"admin\"]")
        private java.util.List<String> roles;

        public UserInfo() {}

        public UserInfo(String userId, String email, String name, java.util.List<String> roles) {
            this.userId = userId;
            this.email = email;
            this.name = name;
            this.roles = roles;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public java.util.List<String> getRoles() {
            return roles;
        }

        public void setRoles(java.util.List<String> roles) {
            this.roles = roles;
        }
    }
}