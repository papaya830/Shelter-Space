package com.enactus.shelterspace.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Catches exceptions thrown anywhere in the app and turns them into clean
 * JSON responses instead of ugly stack traces.
 *
 * @RestControllerAdvice = global handler across all controllers.
 * Each @ExceptionHandler method handles one exception type.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle "not found" -> respond 404.
     * TODO:
     *   - build a response body (a Map<String, Object> is easy: put a
     *     message, a status code, maybe a timestamp).
     *   - return it wrapped in a ResponseEntity with HttpStatus.NOT_FOUND.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        // TODO: implement
        return null;
    }

    /**
     * Handle validation failures (from @Valid) -> respond 400.
     * TODO:
     *   - ex.getBindingResult().getFieldErrors() gives you each bad field.
     *   - collect them into a map of field -> error message.
     *   - return 400 Bad Request with that map.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        // TODO: implement
        return null;
    }
}
