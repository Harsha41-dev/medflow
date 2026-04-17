package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Diagnosis response")
public record DiagnosisResponse(
        @Schema(example = "40")
        Long id,

        @Schema(example = "30")
        Long encounterId,

        @Schema(example = "L30.9")
        String diagnosisCode,

        @Schema(example = "Dermatitis")
        String diagnosisName,

        @Schema(example = "Unspecified dermatitis")
        String description
) {
}
