package com.example.springbootrestapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request model for authentication endpoint
 */
@Schema(description = "Request object for OIDC token exchange")
public class AuthRequest {
    
    @Schema(description = "OIDC access token obtained from identity provider", 
            example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...", 
            required = true)
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