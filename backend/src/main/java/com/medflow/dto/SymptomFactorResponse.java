package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Model factor that influenced the educational symptom-pattern prediction")
public record SymptomFactorResponse(
        @Schema(example = "fever")
        String field,

        @Schema(example = "Fever")
        String label,

        @Schema(example = "Present")
        String value,

        @Schema(example = "1.248")
        double contribution
) {
}
