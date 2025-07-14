package com.example.springbootrestapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;

/**
 * OIDC configuration for opaque token validation.
 * Configures the token introspection endpoint and client credentials.
 */
@Configuration
public class OidcConfig {

    @Value("${spring.security.oauth2.resourceserver.opaque-token.introspection-uri}")
    private String introspectionUri;

    @Value("${spring.security.oauth2.resourceserver.opaque-token.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.resourceserver.opaque-token.client-secret}")
    private String clientSecret;

    /**
     * Creates an opaque token introspector for validating OIDC access tokens.
     * This will be used to validate tokens received from the frontend.
     */
    @Bean
    public OpaqueTokenIntrospector opaqueTokenIntrospector() {
        return new SpringOpaqueTokenIntrospector(introspectionUri, clientId, clientSecret);
    }

    /**
     * Returns the OIDC introspection URI.
     */
    public String getIntrospectionUri() {
        return introspectionUri;
    }

    /**
     * Returns the OIDC client ID.
     */
    public String getClientId() {
        return clientId;
    }
}