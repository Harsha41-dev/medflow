package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Audit log response. Contains action metadata only, not password, token, notes, or prescription contents.")
public record AuditLogResponse(
        @Schema(example = "60")
        Long id,

        @Schema(example = "1")
        Long userId,

        @Schema(example = "admin@example.com")
        String userEmail,

        @Schema(example = "APPOINTMENT_CREATED")
        String action,

        @Schema(example = "Appointment")
        String entityType,

        @Schema(example = "20")
        Long entityId,

        @Schema(example = "2026-08-26T20:00:00")
        LocalDateTime timestamp
) {
}
