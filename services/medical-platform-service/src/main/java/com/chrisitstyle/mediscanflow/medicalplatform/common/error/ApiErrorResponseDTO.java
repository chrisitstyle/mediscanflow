package com.chrisitstyle.mediscanflow.medicalplatform.common.error;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponseDTO(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
    public static ApiErrorResponseDTO of(
            int status,
            String error,
            String message,
            String path
    ) {
        return new ApiErrorResponseDTO(
                Instant.now(),
                status,
                error,
                message,
                path,
                Map.of()
        );
    }

    public static ApiErrorResponseDTO withValidationErrors(
            int status,
            String error,
            String message,
            String path,
            Map<String, String> validationErrors
    ) {
        return new ApiErrorResponseDTO(
                Instant.now(),
                status,
                error,
                message,
                path,
                validationErrors
        );
    }
}