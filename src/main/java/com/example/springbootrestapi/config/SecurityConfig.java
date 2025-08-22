package com.example.springbootrestapi.config;

import com.example.springbootrestapi.security.OidcTokenFilter;
import com.example.springbootrestapi.security.SessionJwtFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the Spring Boot REST API.
 * Configures OIDC authentication, JWT validation, CORS settings, and the security filter chain.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OidcTokenFilter oidcTokenFilter;
    private final SessionJwtFilter sessionJwtFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${app.cors.allow-credentials}")
    private boolean allowCredentials;

    @Value("${spring.security.oauth2.resourceserver.opaque-token.introspection-uri:}")
    private String introspectionUri;

    @Value("${spring.security.oauth2.resourceserver.opaque-token.client-id:}")
    private String clientId;

    @Value("${spring.security.oauth2.resourceserver.opaque-token.client-secret:}")
    private String clientSecret;

    public SecurityConfig(OidcTokenFilter oidcTokenFilter, SessionJwtFilter sessionJwtFilter) {
        this.oidcTokenFilter = oidcTokenFilter;
        this.sessionJwtFilter = sessionJwtFilter;
    }

    /**
     * Configures the main security filter chain with OIDC and JWT support.
     * Sets up authentication requirements and session management.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for REST API (stateless)
            .csrf(csrf -> csrf.disable())
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure session management (stateless for JWT)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/actuator/metrics",
                    "/actuator/metrics/**",
                    "/actuator",
                    "/api-docs/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()
                
                // Authentication endpoint - accessible with OIDC token
                .requestMatchers("/api/auth/**").permitAll()
                
                // All other API endpoints require authentication
                .requestMatchers("/api/**").authenticated()
                
                // Default - require authentication
                .anyRequest().authenticated()
            )
            
            // Add custom security filters
            // SessionJwtFilter should run first to handle session JWTs
            .addFilterBefore(sessionJwtFilter, UsernamePasswordAuthenticationFilter.class)
            // OidcTokenFilter should run after SessionJwtFilter to handle OIDC tokens
            .addFilterAfter(oidcTokenFilter, SessionJwtFilter.class)
            
            // Configure OAuth2 Resource Server for opaque token introspection
            .oauth2ResourceServer(oauth2 -> oauth2
                .opaqueToken(opaque -> {
                    // Only configure if introspection URI is provided
                    if (introspectionUri != null && !introspectionUri.isEmpty()) {
                        opaque.introspectionUri(introspectionUri)
                              .introspectionClientCredentials(clientId, clientSecret);
                    }
                })
            )
            
            // Configure exception handling
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"error\":\"Forbidden\",\"message\":\"Access denied\"}"
                    );
                })
            );

        return http.build();
    }

    /**
     * Configures CORS settings for frontend integration.
     * Allows the React frontend to make authenticated requests to the API.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Set allowed origins from configuration
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);
        
        // Set allowed methods from configuration
        List<String> methods = Arrays.asList(allowedMethods.split(","));
        configuration.setAllowedMethods(methods);
        
        // Set allowed headers
        if ("*".equals(allowedHeaders)) {
            configuration.addAllowedHeader("*");
        } else {
            List<String> headers = Arrays.asList(allowedHeaders.split(","));
            configuration.setAllowedHeaders(headers);
        }
        
        // Configure credentials
        configuration.setAllowCredentials(allowCredentials);
        
        // Expose common headers that frontend might need
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-Session-Token"
        ));
        
        // Set max age for preflight requests
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}