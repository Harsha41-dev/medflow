package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Alternative symptom-pattern prediction confidence")
public record PatternConfidenceResponse(
        @Schema(example = "RESPIRATORY_PATTERN")
        String pattern,

        @Schema(example = "0.21")
        double confidence
) {
}
