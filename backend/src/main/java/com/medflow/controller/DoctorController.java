package com.medflow.controller;

import com.medflow.dto.ApiErrorResponse;
import com.medflow.dto.DoctorResponse;
import com.medflow.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Doctors", description = "Doctor lookup APIs")
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List active doctors", description = "Returns active doctors. Supports page, size, and optional specialization filter.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Doctors listed"),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public Page<DoctorResponse> getDoctors(
            @Parameter(description = "Optional specialization filter", example = "Dermatology")
            @RequestParam(required = false) String specialization,
            @ParameterObject Pageable pageable
    ) {
        return doctorService.getActiveDoctors(specialization, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get doctor by ID", description = "Returns an active doctor, or a doctor's own inactive profile.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Doctor found",
                    content = @Content(schema = @Schema(implementation = DoctorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "User cannot access this doctor profile",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Doctor not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public DoctorResponse getDoctor(
            @Parameter(description = "Doctor profile ID") @PathVariable Long id,
            Authentication authentication
    ) {
        return doctorService.getDoctorForCurrentUser(id, authentication);
    }
}
