package com.enactus.shelterspace.exception;

/**
 * Thrown when a requested resource (e.g. a shelter id) doesn't exist.
 * Your GlobalExceptionHandler will catch this and turn it into a 404.
 *
 * This one is simple enough that it's given to you complete — study how
 * a custom exception just extends RuntimeException and passes a message up.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
