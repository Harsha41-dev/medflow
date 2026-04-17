package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "ADMIN-only doctor account creation request. Role is not accepted because this API always creates DOCTOR users.")
public record CreateDoctorRequest(
        @Schema(example = "doctor@example.com")
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Schema(example = "Password123!", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @Schema(example = "Ananya")
        @NotBlank
        @Size(max = 100)
        String firstName,

        @Schema(example = "Rao")
        @NotBlank
        @Size(max = 100)
        String lastName,

        @Schema(example = "Dermatology")
        @NotBlank
        @Size(max = 100)
        String specialization,

        @Schema(example = "MED100")
        @NotBlank
        @Size(max = 100)
        String licenseNumber
) {
}
