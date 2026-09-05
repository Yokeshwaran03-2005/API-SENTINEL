package com.apisentinel.common.exception;

/**
 * Exception thrown when a requested resource is not found in the persistence layer.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(String.format("%s with identifier '%s' was not found", resourceName, identifier));
    }
}
