package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Prescription request. MedFlow stores prescription data but does not provide medical decision support.")
public record PrescriptionRequest(
        @Schema(example = "Cetirizine")
        @NotBlank
        @Size(max = 150)
        String medicationName,

        @Schema(example = "10 mg")
        @NotBlank
        @Size(max = 100)
        String dosage,

        @Schema(example = "Once daily")
        @NotBlank
        @Size(max = 100)
        String frequency,

        @Schema(example = "5 days")
        @NotBlank
        @Size(max = 100)
        String duration,

        @Schema(example = "Take after food")
        @Size(max = 1000)
        String instructions
) {
}
