package com.ameya.inventory.exception;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {
    }

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }

    public static ApiError ofFieldErrors(int status, String error, String message, String path, Map<String, String> fields) {
        List<FieldError> errors = fields.entrySet().stream()
                .map(e -> new FieldError(e.getKey(), e.getValue()))
                .toList();
        return new ApiError(Instant.now(), status, error, message, path, errors);
    }
}
