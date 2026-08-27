package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Symptom pattern ML service health response")
public record SymptomPredictionHealthResponse(
        @Schema(example = "UP")
        String status,

        @Schema(example = "Symptom prediction service is available")
        String message
) {
}
