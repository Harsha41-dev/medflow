package com.medflow.dto;

import com.medflow.entity.AppointmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Appointment response")
public record AppointmentResponse(
        @Schema(example = "12")
        Long id,

        @Schema(example = "1")
        Long patientId,

        @Schema(example = "Aarav Sharma")
        String patientName,

        @Schema(example = "4")
        Long doctorId,

        @Schema(example = "Ananya Rao")
        String doctorName,

        @Schema(example = "Dermatology")
        String doctorSpecialization,

        @Schema(example = "2026-09-10T10:00:00")
        LocalDateTime appointmentDateTime,

        @Schema(example = "SCHEDULED")
        AppointmentStatus status,

        @Schema(example = "Skin irritation")
        String reason,

        @Schema(example = "2026-08-26T20:00:00")
        LocalDateTime createdAt
) {
}
