package com.example.springbootrestapi.model;

/**
 * Response model for authentication endpoint
 */
public class AuthResponse {
    
    private String sessionToken;
    private UserInfo user;
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
    public static class UserInfo {
        private String userId;
        private String email;
        private String name;
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