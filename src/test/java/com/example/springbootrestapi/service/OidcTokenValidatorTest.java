package com.example.springbootrestapi.service;

import com.example.springbootrestapi.service.OidcTokenValidator.TokenValidationException;
import com.example.springbootrestapi.service.OidcTokenValidator.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OidcTokenValidatorTest {

    @Mock
    private OpaqueTokenIntrospector tokenIntrospector;

    @Mock
    private OAuth2AuthenticatedPrincipal principal;

    private OidcTokenValidator oidcTokenValidator;

    @BeforeEach
    void setUp() {
        oidcTokenValidator = new OidcTokenValidator(tokenIntrospector);
    }

    @Test
    void validateTokenAndExtractUserInfo_WithValidToken_ShouldReturnUserInfo() {
        // Given
        String accessToken = "valid-access-token";
        String expectedUserId = "user123";
        String expectedEmail = "test@example.com";
        String expectedName = "Test User";
        List<String> expectedRoles = Arrays.asList("USER", "ADMIN");

        when(tokenIntrospector.introspect(accessToken)).thenReturn(principal);
        when(principal.getAttribute("sub")).thenReturn(expectedUserId);
        when(principal.getAttribute("email")).thenReturn(expectedEmail);
        when(principal.getAttribute("name")).thenReturn(expectedName);
        when(principal.getAttribute("roles")).thenReturn(expectedRoles);

        // When
        UserInfo userInfo = oidcTokenValidator.validateTokenAndExtractUserInfo(accessToken);

        // Then
        assertNotNull(userInfo);
        assertEquals(expectedUserId, userInfo.getUserId());
        assertEquals(expectedEmail, userInfo.getEmail());
        assertEquals(expectedName, userInfo.getName());
        assertEquals(expectedRoles, userInfo.getRoles());
        
        verify(tokenIntrospector).introspect(accessToken);
    }

    @Test
    void validateTokenAndExtractUserInfo_WithNullPrincipal_ShouldThrowException() {
        // Given
        String accessToken = "invalid-access-token";
        when(tokenIntrospector.introspect(accessToken)).thenReturn(null);

        // When & Then
        TokenValidationException exception = assertThrows(TokenValidationException.class,
                () -> oidcTokenValidator.validateTokenAndExtractUserInfo(accessToken));
        
        assertTrue(exception.getMessage().contains("Token validation failed"));
        verify(tokenIntrospector).introspect(accessToken);
    }

    @Test
    void validateTokenAndExtractUserInfo_WithIntrospectionException_ShouldThrowException() {
        // Given
        String accessToken = "problematic-token";
        when(tokenIntrospector.introspect(accessToken))
                .thenThrow(new RuntimeException("Introspection failed"));

        // When & Then
        TokenValidationException exception = assertThrows(TokenValidationException.class,
                () -> oidcTokenValidator.validateTokenAndExtractUserInfo(accessToken));
        
        assertTrue(exception.getMessage().contains("Token validation failed"));
        assertTrue(exception.getCause() instanceof RuntimeException);
        verify(tokenIntrospector).introspect(accessToken);
    }

    @Test
    void validateTokenAndExtractUserInfo_WithMissingSubject_ShouldUsePrincipalName() {
        // Given
        String accessToken = "valid-token";
        String expectedUserId = "principal-name";
        String expectedEmail = "test@example.com";

        when(tokenIntrospector.introspect(accessToken)).thenReturn(principal);
        when(principal.getAttribute("sub")).thenReturn(null);
        when(principal.getName()).thenReturn(expectedUserId);
        when(principal.getAttribute("email")).thenReturn(expectedEmail);
        when(principal.getAttribute("name")).thenReturn("Test User");
        when(principal.getAttribute("roles")).thenReturn(Arrays.asList("USER"));

        // When
        UserInfo userInfo = oidcTokenValidator.validateTokenAndExtractUserInfo(accessToken);

        // Then
        assertEquals(expectedUserId, userInfo.getUserId());
        assertEquals(expectedEmail, userInfo.getEmail());
    }

    @Test
    void validateTokenAndExtractUserInfo_WithSeparateGivenAndFamilyNames_ShouldCombineNames() {
        // Given
        String accessToken = "valid-token";
        String givenName = "John";
        String familyName = "Doe";
        String expectedFullName = "John Doe";

        when(tokenIntrospector.introspect(accessToken)).thenReturn(principal);
        when(principal.getAttribute("sub")).thenReturn("user123");
        when(principal.getAttribute("email")).thenReturn("john.doe@example.com");
        when(principal.getAttribute("name")).thenReturn(null);
        when(principal.getAttribute("given_name")).thenReturn(givenName);
        when(principal.getAttribute("family_name")).thenReturn(familyName);
        when(principal.getAttribute("roles")).thenReturn(Arrays.asList("USER"));

        // When
        UserInfo userInfo = oidcTokenValidator.validateTokenAndExtractUserInfo(accessToken);

        // Then
        assertEquals(expectedFullName, userInfo.getName());
    }

    @Test
    void validateTokenAndExtractUserInfo_WithOnlyFamilyName_ShouldUseFamilyName() {
        // Given
        String accessToken = "valid-token";
        String familyName = "Doe";

        when(tokenIntrospector.introspect(accessToken)).thenReturn(principal);
        when(principal.getAttribute("sub")).thenReturn("user123");
        when(principal.getAttribute("email")).thenReturn("doe@example.com");
        when(principal.getAttribute("name")).thenReturn(null);
        when(principal.getAttribute("given_name")).thenReturn(null);
        when(principal.getAttribute("family_name")).thenReturn(familyName);
        when(principal.getAttribute("roles")).thenReturn(Arrays.asList("USER"));

        // When
        UserInfo userInfo = oidcTokenValidator.validateTokenAndExtractUserInfo(accessToken);

        // Then
        assertEquals(familyName, userInfo.getName());
    }

    @Test
    void validateTokenAndExtractUserInfo_WithRolesAsString_ShouldParseCommaSeparatedRoles() {
        // Given
        String accessToken = "valid-token";
        String rolesString = "USER,ADMIN,MANAGER";
        List<String> expectedRoles = Arrays.asList("USER", "ADMIN", "MANAGER");

        when(tokenIntrospector.introspect(accessToken)).thenReturn(principal);
        when(principal.getAttribute("sub")).thenReturn("user123");
        when(principal.getAttribute("email")).thenReturn("test@example.com");
        when(principal.getAttribute("name")).thenReturn("Test User");
        when(principal.getAttribute("roles")).thenReturn(rolesString);

        // When
        UserInfo userInfo = oidcTokenValidator.validateTokenAndExtractUserInfo(accessToken);

        // Then
        assertEquals(expectedRoles, userInfo.getRoles());
    }

    @Test
    void validateTokenAndExtractUserInfo_WithAuthoritiesClaim_ShouldExtractRoles() {
        // Given
        String accessToken = "valid-token";
        List<String> authorities = Arrays.asList("ROLE_USER", "ROLE_ADMIN");

        when(tokenIntrospector.introspect(accessToken)).thenReturn(principal);
        when(principal.getAttribute("sub")).thenReturn("user123");
        when(principal.getAttribute("email")).thenReturn("test@example.com");
        when(principal.getAttribute("name")).thenReturn("Test User");
        when(principal.getAttribute("roles")).thenReturn(null);
        when(principal.getAttribute("authorities")).thenReturn(authorities);

        // When
        UserInfo userInfo = oidcTokenValidator.validateTokenAndExtractUserInfo(accessToken);

        // Then
        assertEquals(authorities, userInfo.getRoles());
    }

    @Test
    void validateTokenAndExtractUserInfo_WithGroupsClaim_ShouldExtractRoles() {
        // Given
        String accessToken = "valid-token";
        List<String> groups = Arrays.asList("developers", "admins");

        when(tokenIntrospector.introspect(accessToken)).thenReturn(principal);
        when(principal.getAttribute("sub")).thenReturn("user123");
        when(principal.getAttribute("email")).thenReturn("test@example.com");
        when(principal.getAttribute("name")).thenReturn("Test User");
        when(principal.getAttribute("roles")).thenReturn(null);
        when(principal.getAttribute("authorities")).thenReturn(null);
        when(principal.getAttribute("groups")).thenReturn(groups);

        // When
        UserInfo userInfo = oidcTokenValidator.validateTokenAndExtractUserInfo(accessToken);

        // Then
        assertEquals(groups, userInfo.getRoles());
    }

    @Test
    void validateTokenAndExtractUserInfo_WithNoRoles_ShouldAssignDefaultUserRole() {
        // Given
        String accessToken = "valid-token";

        when(tokenIntrospector.introspect(accessToken)).thenReturn(principal);
        when(principal.getAttribute("sub")).thenReturn("user123");
        when(principal.getAttribute("email")).thenReturn("test@example.com");
        when(principal.getAttribute("name")).thenReturn("Test User");
        when(principal.getAttribute("roles")).thenReturn(null);
        when(principal.getAttribute("authorities")).thenReturn(null);
        when(principal.getAttribute("groups")).thenReturn(null);
        when(principal.getAttribute("realm_access.roles")).thenReturn(null);

        // When
        UserInfo userInfo = oidcTokenValidator.validateTokenAndExtractUserInfo(accessToken);

        // Then
        assertEquals(Arrays.asList("USER"), userInfo.getRoles());
    }

    @Test
    void validateTokenAndExtractUserInfo_WithEmptyRolesList_ShouldAssignDefaultUserRole() {
        // Given
        String accessToken = "valid-token";

        when(tokenIntrospector.introspect(accessToken)).thenReturn(principal);
        when(principal.getAttribute("sub")).thenReturn("user123");
        when(principal.getAttribute("email")).thenReturn("test@example.com");
        when(principal.getAttribute("name")).thenReturn("Test User");
        when(principal.getAttribute("roles")).thenReturn(Arrays.asList());

        // When
        UserInfo userInfo = oidcTokenValidator.validateTokenAndExtractUserInfo(accessToken);

        // Then
        assertEquals(Arrays.asList("USER"), userInfo.getRoles());
    }

    @Test
    void validateTokenAndExtractUserInfo_WithCompleteUserInfo_ShouldExtractAllFields() {
        // Given
        String accessToken = "complete-token";
        String userId = "user123";
        String email = "complete@example.com";
        String name = "Complete User";
        List<String> roles = Arrays.asList("USER", "ADMIN", "MANAGER");

        when(tokenIntrospector.introspect(accessToken)).thenReturn(principal);
        when(principal.getAttribute("sub")).thenReturn(userId);
        when(principal.getAttribute("email")).thenReturn(email);
        when(principal.getAttribute("name")).thenReturn(name);
        when(principal.getAttribute("roles")).thenReturn(roles);

        // When
        UserInfo userInfo = oidcTokenValidator.validateTokenAndExtractUserInfo(accessToken);

        // Then
        assertNotNull(userInfo);
        assertEquals(userId, userInfo.getUserId());
        assertEquals(email, userInfo.getEmail());
        assertEquals(name, userInfo.getName());
        assertEquals(roles, userInfo.getRoles());
        
        // Verify toString method works
        String userInfoString = userInfo.toString();
        assertTrue(userInfoString.contains(userId));
        assertTrue(userInfoString.contains(email));
        assertTrue(userInfoString.contains(name));
    }

    @Test
    void userInfo_ShouldBeImmutable() {
        // Given
        String userId = "user123";
        String email = "test@example.com";
        String name = "Test User";
        List<String> originalRoles = Arrays.asList("USER", "ADMIN");
        
        // When
        UserInfo userInfo = new UserInfo(userId, email, name, originalRoles);
        List<String> retrievedRoles = userInfo.getRoles();
        
        // Try to modify the retrieved roles list
        retrievedRoles.add("HACKER");
        
        // Then
        // The original roles in UserInfo should not be affected
        assertEquals(2, userInfo.getRoles().size());
        assertTrue(userInfo.getRoles().contains("USER"));
        assertTrue(userInfo.getRoles().contains("ADMIN"));
        assertFalse(userInfo.getRoles().contains("HACKER"));
    }

    @Test
    void userInfo_WithNullRoles_ShouldHandleGracefully() {
        // Given
        String userId = "user123";
        String email = "test@example.com";
        String name = "Test User";
        
        // When
        UserInfo userInfo = new UserInfo(userId, email, name, null);
        
        // Then
        assertNotNull(userInfo.getRoles());
        assertTrue(userInfo.getRoles().isEmpty());
    }
}