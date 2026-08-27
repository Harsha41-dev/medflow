package com.medflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Login request")
public record LoginRequest(
        @Schema(example = "patient@example.com")
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @Schema(example = "Password123!", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank
        @Size(max = 100)
        String password
) {
}
