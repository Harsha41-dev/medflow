package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Educational possible cause to consider for a symptom-pattern prediction")
public record SymptomPossibleCauseResponse(
        @Schema(example = "Viral-like illness pattern")
        String title,

        @Schema(example = "Fever, fatigue, body ache, headache, and sore throat often appear together in viral-like presentations.")
        String reason
) {
}
