package com.medflow.dto;

import com.medflow.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response containing the JWT access token")
public record AuthResponse(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9.example-token")
        String accessToken,

        @Schema(example = "Bearer")
        String tokenType,

        @Schema(example = "1")
        Long userId,

        @Schema(example = "1", description = "Patient or doctor profile ID. ADMIN users receive null.")
        Long profileId,

        @Schema(example = "patient@example.com")
        String email,

        @Schema(example = "PATIENT")
        Role role
) {
}
