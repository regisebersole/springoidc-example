package com.example.springbootrestapi.exception;

/**
 * Exception thrown when authentication fails
 * Results in HTTP 401 Unauthorized response
 */
public class AuthenticationException extends RuntimeException {
    
    private final String reason;

    public AuthenticationException(String message) {
        super(message);
        this.reason = null;
    }

    public AuthenticationException(String message, String reason) {
        super(message);
        this.reason = reason;
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
        this.reason = null;
    }

    public AuthenticationException(String message, String reason, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}