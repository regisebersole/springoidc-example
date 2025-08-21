package com.example.springbootrestapi.exception;

/**
 * Exception thrown when authorization fails (user is authenticated but lacks permissions)
 * Results in HTTP 403 Forbidden response
 */
public class AuthorizationException extends RuntimeException {
    
    private final String requiredPermission;
    private final String userId;

    public AuthorizationException(String message) {
        super(message);
        this.requiredPermission = null;
        this.userId = null;
    }

    public AuthorizationException(String message, String requiredPermission) {
        super(message);
        this.requiredPermission = requiredPermission;
        this.userId = null;
    }

    public AuthorizationException(String message, String requiredPermission, String userId) {
        super(message);
        this.requiredPermission = requiredPermission;
        this.userId = userId;
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
        this.requiredPermission = null;
        this.userId = null;
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }

    public String getUserId() {
        return userId;
    }
}