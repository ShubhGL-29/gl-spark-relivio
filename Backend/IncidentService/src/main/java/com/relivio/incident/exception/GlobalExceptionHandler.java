package com.relivio.incident.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ErrorResponse buildErrorResponse(HttpStatus status, String message, HttpServletRequest request, Map<String, String> validationErrors) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(status.value());
        errorResponse.setError(status.getReasonPhrase());
        errorResponse.setMessage(message);
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setValidationErrors(validationErrors);
        return errorResponse;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {} at path {}", ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, null), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidIncidentStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidIncidentStateException(InvalidIncidentStateException ex, HttpServletRequest request) {
        log.warn("Conflict error: {} at path {}", ex.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request, null), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument in request on path {}: {}", request.getRequestURI(), ex.getMessage());
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation failed for request on path {}: {}", request.getRequestURI(), errors);
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, errors), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("An unexpected error occurred at path {}", request.getRequestURI(), ex);
        return new ResponseEntity<>(buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please contact support.", request, null), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
