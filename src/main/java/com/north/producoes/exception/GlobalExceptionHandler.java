package com.north.producoes.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePostNotFound(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "status", 404,
                "error", "Not Found",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body(Map.of(
                "status", 409,
                "error", "Conflict",
                "message", "Operacao viola restricao de integridade",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.valueOf(422)).body(Map.of(
                "status", 422,
                "error", "Unprocessable Entity",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", 400,
                "error", "Bad Request",
                "message", "Validation failed",
                "errors", errors,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>>
    handleAlreadyExists(ResourceAlreadyExistsException e) {
        return ResponseEntity.status(409).body(Map.of(
                "status", 409,
                "error", "Conflict",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(AiIntegrationException.class)
    public ResponseEntity<Map<String, Object>> handleAiIntegration(AiIntegrationException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "status", 502,
                "error", "Bad Gateway",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(MetaGraphIntegrationException.class)
    public ResponseEntity<Map<String, Object>> handleMetaGraphIntegration(MetaGraphIntegrationException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "status", 502,
                "error", "Bad Gateway",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(EvolutionApiIntegrationException.class)
    public ResponseEntity<Map<String, Object>> handleEvolutionApiIntegration(EvolutionApiIntegrationException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "status", 502,
                "error", "Bad Gateway",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", 403,
                "error", "Forbidden",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRefreshToken(InvalidRefreshTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "status", 401,
                "error", "Unauthorized",
                "message", e.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        ));
    }

}
