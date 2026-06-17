package com.finance.tracker.exception;

import com.finance.tracker.dto.response.ApiResponse;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import com.finance.tracker.dto.response.AiChatResponse;

/**
 * FIXES APPLIED:
 *
 * 1. Added AccessDeniedException handler → returns 403 with a clear JSON
 * message.
 * Without this, Spring Security's default 403 handler returns an HTML error
 * page
 * (or empty body), which the frontend interprets as a broken API and shows a
 * blank page.
 *
 * 2. Added AuthenticationException handler → returns 401 with JSON.
 * Without this, expired/invalid tokens return HTML, not JSON.
 *
 * 3. Added generic Exception safety net remains in place.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * FIX: Handle 403 Forbidden (wrong role / missing role).
     * Returns JSON instead of HTML so the frontend can show a proper message.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied: you do not have permission to access this resource."));
    }

    /**
     * FIX: Handle 401 Unauthorized (missing or invalid JWT).
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Authentication failed: " + ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            errors.put(field, error.getDefaultMessage());
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .build());
    }

    /**
     * FIX: Handle JSON deserialization errors (e.g. invalid ENUM values sent from
     * frontend).
     * Returns 400 Bad Request instead of 500 Internal Server Error.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleJsonMappingException(HttpMessageNotReadableException ex) {
        String msg = "Invalid request payload. Please verify your data formats and enum values.";
        if (ex.getCause() instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException formatEx) {
            if (formatEx.getTargetType() != null && formatEx.getTargetType().isEnum()) {
                msg = "Invalid value provided for " + formatEx.getTargetType().getSimpleName() + ". " +
                        "Accepted values are: "
                        + java.util.Arrays.toString(formatEx.getTargetType().getEnumConstants());
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error: " + ex.getMessage()));
    }

    /**
     * FIX: Handle Gemini API Key missing graceful error
     */
    @ExceptionHandler(ApiKeyMissingException.class)
    public ResponseEntity<AiChatResponse> handleApiKeyMissing(ApiKeyMissingException ex) {
        return ResponseEntity.ok(
                AiChatResponse.builder()
                        .success(false)
                        .message("Gemini API key missing")
                        .build()
        );
    }

    /**
     * FIX: Handle Gemini API generic failure
     */
    @ExceptionHandler(GeminiApiException.class)
    public ResponseEntity<AiChatResponse> handleGeminiApiError(GeminiApiException ex) {
        return ResponseEntity.ok(
                AiChatResponse.builder()
                        .success(false)
                        .message(ex.getMessage())
                        .build()
        );
    }
}
