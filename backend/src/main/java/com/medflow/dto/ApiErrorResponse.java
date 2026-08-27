package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;

@Schema(description = "Standard error response returned by validation, authorization, conflict, and server error handlers")
public record ApiErrorResponse(
        @Schema(example = "2026-08-26T20:00:00")
        LocalDateTime timestamp,

        @Schema(example = "400")
        int status,

        @Schema(example = "Bad Request")
        String error,

        @Schema(example = "Validation failed")
        String message,

        @Schema(example = "/api/v1/appointments")
        String path,

        @Schema(description = "Field-level validation errors when present", example = "{\"doctorId\":\"Doctor is required\"}")
        Map<String, String> fieldErrors
) {

    public ApiErrorResponse {
        if (fieldErrors == null) {
            fieldErrors = Map.of();
        }
    }

    public ApiErrorResponse(LocalDateTime timestamp, int status, String message) {
        this(
                timestamp,
                status,
                HttpStatus.valueOf(status).getReasonPhrase(),
                message,
                null,
                Map.of()
        );
    }

    public ApiErrorResponse(
            LocalDateTime timestamp,
            HttpStatus status,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {
        this(timestamp, status.value(), status.getReasonPhrase(), message, path, fieldErrors);
    }
}
