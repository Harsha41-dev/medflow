package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Encounter creation request. Patient and doctor are derived from the appointment.")
public record EncounterRequest(
        @Schema(example = "20")
        @NotNull
        Long appointmentId,

        @Schema(example = "Skin irritation for three weeks")
        @NotBlank
        @Size(max = 500)
        String chiefComplaint,

        @Schema(example = "Mild redness observed")
        @Size(max = 2000)
        String notes
) {
}
