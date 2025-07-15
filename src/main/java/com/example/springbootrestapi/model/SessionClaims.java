package com.example.springbootrestapi.model;

import java.time.Instant;
import java.util.List;

/**
 * Model representing JWT session claims with user information and session metadata
 */
public class SessionClaims {
    private String sub; // User ID (subject)
    private String email;
    private String name;
    private List<String> roles;
    private Long iat; // Issued at timestamp
    private Long exp; // Expires at timestamp
    private Long lastActivity; // Last activity timestamp
    private Long sessionStart; // Session start timestamp

    public SessionClaims() {}

    public SessionClaims(String sub, String email, String name, List<String> roles, 
                        Long iat, Long exp, Long lastActivity, Long sessionStart) {
        this.sub = sub;
        this.email = email;
        this.name = name;
        this.roles = roles;
        this.iat = iat;
        this.exp = exp;
        this.lastActivity = lastActivity;
        this.sessionStart = sessionStart;
    }

    // Getters and setters
    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
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

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Long getIat() {
        return iat;
    }

    public void setIat(Long iat) {
        this.iat = iat;
    }

    public Long getExp() {
        return exp;
    }

    public void setExp(Long exp) {
        this.exp = exp;
    }

    public Long getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(Long lastActivity) {
        this.lastActivity = lastActivity;
    }

    public Long getSessionStart() {
        return sessionStart;
    }

    public void setSessionStart(Long sessionStart) {
        this.sessionStart = sessionStart;
    }

    /**
     * Check if the session has expired due to inactivity (20 minutes)
     */
    public boolean isInactivityExpired(int inactivityTimeoutSeconds) {
        if (lastActivity == null) {
            return true;
        }
        long currentTime = Instant.now().getEpochSecond();
        return (currentTime - lastActivity) > inactivityTimeoutSeconds;
    }

    /**
     * Check if the session has exceeded maximum duration (24 hours)
     */
    public boolean isMaxDurationExpired(int maxSessionDurationSeconds) {
        if (sessionStart == null) {
            return true;
        }
        long currentTime = Instant.now().getEpochSecond();
        return (currentTime - sessionStart) > maxSessionDurationSeconds;
    }

    /**
     * Update the last activity timestamp to current time
     */
    public void updateLastActivity() {
        this.lastActivity = Instant.now().getEpochSecond();
    }
}