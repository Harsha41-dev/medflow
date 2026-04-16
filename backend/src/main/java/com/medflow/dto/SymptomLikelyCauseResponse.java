package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Educational likely cause to review for a symptom-pattern prediction")
public record SymptomLikelyCauseResponse(
        @Schema(example = "FLU_LIKE_VIRAL_ILLNESS")
        String code,

        @Schema(example = "Flu-like viral illness cause")
        String title,

        @Schema(example = "0.81")
        double confidence,

        @Schema(example = "HIGH")
        String confidenceLevel,

        List<String> evidence,

        List<String> uncertaintyNotes,

        List<String> nextSteps
) {
}
