package com.medflow.dto;

import com.medflow.entity.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Patient profile response. User security fields are not exposed.")
public record PatientResponse(
        @Schema(example = "1")
        Long id,

        @Schema(example = "Aarav")
        String firstName,

        @Schema(example = "Sharma")
        String lastName,

        @Schema(example = "2001-05-15")
        LocalDate dateOfBirth,

        @Schema(example = "MALE")
        Gender gender,

        @Schema(example = "9876543210")
        String phone,

        @Schema(example = "Synthetic patient address")
        String address,

        @Schema(example = "9876500000")
        String emergencyContact
) {
}
