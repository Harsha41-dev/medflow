package com.medflow.dto;

import com.medflow.entity.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "Public patient registration request. Role is not accepted because public registration always creates PATIENT users.")
public record RegisterRequest(
        @Schema(example = "patient@example.com")
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Schema(example = "Password123!", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @Schema(example = "Aarav")
        @NotBlank
        @Size(max = 100)
        String firstName,

        @Schema(example = "Sharma")
        @NotBlank
        @Size(max = 100)
        String lastName,

        @Schema(example = "2001-05-15")
        @NotNull
        @Past
        LocalDate dateOfBirth,

        @Schema(example = "MALE")
        @NotNull
        Gender gender,

        @Schema(example = "9876543210")
        @NotBlank
        @Size(max = 20)
        String phone,

        @Schema(example = "Synthetic patient address")
        @NotBlank
        @Size(max = 500)
        String address,

        @Schema(example = "9876500000")
        @NotBlank
        @Size(max = 100)
        String emergencyContact
) {
}
