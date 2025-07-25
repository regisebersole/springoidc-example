package com.example.springbootrestapi.model;

import jakarta.validation.constraints.NotBlank;

/**
 * Request model for authentication endpoint
 */
public class AuthRequest {
    
    @NotBlank(message = "Access token is required")
    private String accessToken;

    public AuthRequest() {}

    public AuthRequest(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}