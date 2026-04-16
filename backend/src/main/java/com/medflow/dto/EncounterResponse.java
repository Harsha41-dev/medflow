package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Clinical encounter response")
public record EncounterResponse(
        @Schema(example = "30")
        Long id,

        @Schema(example = "20")
        Long appointmentId,

        @Schema(example = "1")
        Long patientId,

        @Schema(example = "Aarav Sharma")
        String patientName,

        @Schema(example = "4")
        Long doctorId,

        @Schema(example = "Ananya Rao")
        String doctorName,

        @Schema(example = "2026-09-10T10:30:00")
        LocalDateTime visitDate,

        @Schema(example = "Skin irritation for three weeks")
        String chiefComplaint,

        @Schema(example = "Mild redness observed")
        String notes
) {
}
