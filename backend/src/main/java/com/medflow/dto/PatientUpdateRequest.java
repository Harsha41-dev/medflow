package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Patient profile update request for editable profile fields")
public record PatientUpdateRequest(
        @Schema(example = "Aarav")
        @NotBlank
        @Size(max = 100)
        String firstName,

        @Schema(example = "Sharma")
        @NotBlank
        @Size(max = 100)
        String lastName,

        @Schema(example = "9876543210")
        @NotBlank
        @Size(max = 20)
        String phone,

        @Schema(example = "Updated synthetic patient address")
        @NotBlank
        @Size(max = 500)
        String address,

        @Schema(example = "9876500000")
        @NotBlank
        @Size(max = 100)
        String emergencyContact
) {
}
