package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Prescription response")
public record PrescriptionResponse(
        @Schema(example = "50")
        Long id,

        @Schema(example = "30")
        Long encounterId,

        @Schema(example = "Cetirizine")
        String medicationName,

        @Schema(example = "10 mg")
        String dosage,

        @Schema(example = "Once daily")
        String frequency,

        @Schema(example = "5 days")
        String duration,

        @Schema(example = "Take after food")
        String instructions
) {
}
