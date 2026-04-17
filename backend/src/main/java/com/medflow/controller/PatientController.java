package com.medflow.controller;

import com.medflow.dto.ApiErrorResponse;
import com.medflow.dto.PatientResponse;
import com.medflow.dto.PatientUpdateRequest;
import com.medflow.service.AuthorizationService;
import com.medflow.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Patients", description = "Patient profile APIs")
public class PatientController {

    private final PatientService patientService;
    private final AuthorizationService authorizationService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    @Operation(summary = "Get patient profile", description = "PATIENT users may access only their own profile.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patient profile found",
                    content = @Content(schema = @Schema(implementation = PatientResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "User cannot access this patient profile",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Patient not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public PatientResponse getPatient(
            @Parameter(description = "Patient profile ID") @PathVariable Long id,
            Authentication authentication
    ) {
        authorizationService.checkPatientProfileAccess(id, authentication);
        return patientService.getPatient(id, authentication);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    @Operation(summary = "Update patient profile", description = "PATIENT users may update only their own editable profile fields.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patient profile updated",
                    content = @Content(schema = @Schema(implementation = PatientResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "User cannot update this patient profile",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Patient not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public PatientResponse updatePatient(
            @Parameter(description = "Patient profile ID") @PathVariable Long id,
            @Valid @RequestBody PatientUpdateRequest request,
            Authentication authentication
    ) {
        authorizationService.checkPatientProfileAccess(id, authentication);
        return patientService.updatePatient(id, request, authentication);
    }
}
