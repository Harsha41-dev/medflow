package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "Appointment booking request. patientId is not accepted because patient identity comes from the JWT.")
public record AppointmentRequest(
        @Schema(example = "4")
        @NotNull
        Long doctorId,

        @Schema(example = "2026-09-10T10:00:00", description = "Future appointment time using a 30-minute slot")
        @NotNull
        @Future
        LocalDateTime appointmentDateTime,

        @Schema(example = "Skin irritation")
        @NotBlank
        @Size(max = 500)
        String reason
) {
}
