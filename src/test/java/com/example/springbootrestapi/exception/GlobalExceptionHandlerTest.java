package com.example.springbootrestapi.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @Mock
    private WebRequest webRequest;

    @Mock
    private MethodArgumentNotValidException methodArgumentNotValidException;

    @Mock
    private BindingResult bindingResult;

    @BeforeEach
    void setUp() {
        when(webRequest.getDescription(false)).thenReturn("uri=/test/endpoint");
    }

    @Test
    void shouldHandleResourceNotFoundException() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Task", "123");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleResourceNotFoundException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("RESOURCE_NOT_FOUND", response.getBody().getError());
        assertEquals("Task with id '123' not found", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertEquals("uri=/test/endpoint", response.getBody().getPath());
        assertNull(response.getBody().getFieldErrors());
    }

    @Test
    void shouldHandleAuthenticationException() {
        // Given
        AuthenticationException exception = new AuthenticationException("Invalid token", "TOKEN_EXPIRED");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleAuthenticationException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("AUTHENTICATION_FAILED", response.getBody().getError());
        assertEquals("Invalid token", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertEquals("uri=/test/endpoint", response.getBody().getPath());
        assertNull(response.getBody().getFieldErrors());
    }

    @Test
    void shouldHandleAuthorizationException() {
        // Given
        AuthorizationException exception = new AuthorizationException("Access denied", "ADMIN_REQUIRED", "user123");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleAuthorizationException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACCESS_DENIED", response.getBody().getError());
        assertEquals("Access denied", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertEquals("uri=/test/endpoint", response.getBody().getPath());
        assertNull(response.getBody().getFieldErrors());
    }

    @Test
    void shouldHandleIllegalArgumentException() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid parameter value");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleIllegalArgumentException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_ARGUMENT", response.getBody().getError());
        assertEquals("Invalid parameter value", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertEquals("uri=/test/endpoint", response.getBody().getPath());
        assertNull(response.getBody().getFieldErrors());
    }

    @Test
    void shouldHandleGenericException() {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected server error");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleGlobalException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().getError());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertEquals("uri=/test/endpoint", response.getBody().getPath());
        assertNull(response.getBody().getFieldErrors());
    }

    @Test
    void shouldHandleValidationErrors() {
        // Given
        FieldError fieldError1 = new FieldError("testObject", "name", "Name is required");
        FieldError fieldError2 = new FieldError("testObject", "email", "Invalid email format");

        when(methodArgumentNotValidException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError1, fieldError2));
        when(methodArgumentNotValidException.getMessage()).thenReturn("Validation failed");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleValidationExceptions(methodArgumentNotValidException, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("VALIDATION_ERROR", response.getBody().getError());
        assertEquals("Validation failed for one or more fields", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertEquals("uri=/test/endpoint", response.getBody().getPath());
        assertNotNull(response.getBody().getFieldErrors());
        assertEquals("Name is required", response.getBody().getFieldErrors().get("name"));
        assertEquals("Invalid email format", response.getBody().getFieldErrors().get("email"));
    }

    @Test
    void shouldHandleMalformedJson() {
        // Given
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
        when(exception.getMessage()).thenReturn("JSON parse error");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleHttpMessageNotReadable(exception, webRequest);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MALFORMED_JSON", response.getBody().getError());
        assertEquals("Malformed JSON in request body", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
        assertEquals("uri=/test/endpoint", response.getBody().getPath());
        assertNull(response.getBody().getFieldErrors());
    }

    @Test
    void shouldHandleUnsupportedMediaType() {
        // Given
        HttpMediaTypeNotSupportedException exception = mock(HttpMediaTypeNotSupportedException.class);
        when(exception.getContentType()).thenReturn(MediaType.TEXT_PLAIN);

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleHttpMediaTypeNotSupported(exception, webRequest);

        // Then
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UNSUPPORTED_MEDIA_TYPE", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("text/plain"));
        assertTrue(response.getBody().getMessage().contains("not supported"));
        assertNotNull(response.getBody().getTimestamp());
        assertEquals("uri=/test/endpoint", response.getBody().getPath());
        assertNull(response.getBody().getFieldErrors());
    }

    @Test
    void shouldHandleMethodNotAllowed() {
        // Given
        HttpRequestMethodNotSupportedException exception = mock(HttpRequestMethodNotSupportedException.class);
        when(exception.getMethod()).thenReturn("POST");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleHttpRequestMethodNotSupported(exception, webRequest);

        // Then
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("METHOD_NOT_ALLOWED", response.getBody().getError());
        assertTrue(response.getBody().getMessage().contains("POST"));
        assertTrue(response.getBody().getMessage().contains("not supported"));
        assertNotNull(response.getBody().getTimestamp());
        assertEquals("uri=/test/endpoint", response.getBody().getPath());
        assertNull(response.getBody().getFieldErrors());
    }

    @Test
    void shouldHandleResourceNotFoundExceptionWithMessage() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Custom resource not found message");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleResourceNotFoundException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("RESOURCE_NOT_FOUND", response.getBody().getError());
        assertEquals("Custom resource not found message", response.getBody().getMessage());
    }

    @Test
    void shouldHandleAuthenticationExceptionWithoutReason() {
        // Given
        AuthenticationException exception = new AuthenticationException("Token validation failed");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleAuthenticationException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("AUTHENTICATION_FAILED", response.getBody().getError());
        assertEquals("Token validation failed", response.getBody().getMessage());
    }

    @Test
    void shouldHandleAuthorizationExceptionWithoutDetails() {
        // Given
        AuthorizationException exception = new AuthorizationException("Insufficient permissions");

        // When
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = 
            globalExceptionHandler.handleAuthorizationException(exception, webRequest);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACCESS_DENIED", response.getBody().getError());
        assertEquals("Insufficient permissions", response.getBody().getMessage());
    }
}