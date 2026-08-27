package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Available appointment slot response")
public record AppointmentSlotResponse(
        @Schema(example = "2026-09-10T10:00:00")
        LocalDateTime appointmentDateTime,

        @Schema(example = "10:00 AM")
        String displayTime
) {
}
