package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ranked likely-cause confidence returned by the symptom model")
public record SymptomCauseConfidenceResponse(
        @Schema(example = "FLU_LIKE_VIRAL_ILLNESS")
        String cause,

        @Schema(example = "0.81")
        double confidence
) {
}
