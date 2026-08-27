package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Diagnosis request. MedFlow stores the code but does not validate official ICD correctness.")
public record DiagnosisRequest(
        @Schema(example = "L30.9")
        @NotBlank
        @Size(max = 50)
        String diagnosisCode,

        @Schema(example = "Dermatitis")
        @NotBlank
        @Size(max = 150)
        String diagnosisName,

        @Schema(example = "Unspecified dermatitis")
        @Size(max = 1000)
        String description
) {
}
