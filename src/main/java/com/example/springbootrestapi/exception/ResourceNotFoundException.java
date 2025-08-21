package com.example.springbootrestapi.exception;

/**
 * Exception thrown when a requested resource is not found
 * Results in HTTP 404 Not Found response
 */
public class ResourceNotFoundException extends RuntimeException {
    
    private final String resourceType;
    private final String resourceId;

    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceType = null;
        this.resourceId = null;
    }

    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s with id '%s' not found", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public ResourceNotFoundException(String resourceType, String resourceId, String message) {
        super(message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }
}