package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Doctor profile response. Password data is never exposed.")
public record DoctorResponse(
        @Schema(example = "4")
        Long id,

        @Schema(example = "8")
        Long userId,

        @Schema(example = "doctor@example.com")
        String email,

        @Schema(example = "Ananya")
        String firstName,

        @Schema(example = "Rao")
        String lastName,

        @Schema(example = "Dermatology")
        String specialization,

        @Schema(example = "MED100")
        String licenseNumber,

        @Schema(example = "true")
        boolean active
) {
}
