package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Educational safety flag returned by the symptom-pattern ML service")
public record SymptomSafetyFlagResponse(
        @Schema(example = "BREATHING_OR_CHEST_SYMPTOM")
        String code,

        @Schema(example = "REVIEW")
        String severity,

        @Schema(example = "Breathing difficulty or chest discomfort was selected.")
        String message
) {
}
