package com.medflow.controller;

import com.medflow.dto.ApiErrorResponse;
import com.medflow.dto.EncounterRequest;
import com.medflow.dto.EncounterResponse;
import com.medflow.service.EncounterService;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Encounters", description = "Clinical encounter APIs")
public class EncounterController {

    private final EncounterService encounterService;

    @PostMapping("/encounters")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
            summary = "Create encounter",
            description = "DOCTOR only. Patient and doctor are derived from the appointment; do not send patientId or doctorId."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Encounter created and appointment marked COMPLETED",
                    content = @Content(schema = @Schema(implementation = EncounterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or appointment is not scheduled",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Doctor cannot create an encounter for this appointment",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Appointment not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Encounter already exists for this appointment",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<EncounterResponse> createEncounter(
            @Valid @RequestBody EncounterRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(encounterService.createEncounter(request, authentication));
    }

    @GetMapping("/encounters/{id}")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
    @Operation(summary = "Get encounter by ID", description = "Patients can view their own encounters. Doctors need a SCHEDULED or COMPLETED appointment relationship with the patient.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encounter found",
                    content = @Content(schema = @Schema(implementation = EncounterResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "User cannot access this encounter",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Encounter not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public EncounterResponse getEncounter(
            @Parameter(description = "Encounter ID") @PathVariable Long id,
            Authentication authentication
    ) {
        return encounterService.getEncounter(id, authentication);
    }

    @GetMapping("/patients/{patientId}/encounters")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
    @Operation(summary = "List patient encounters", description = "Patients can view their own encounter history. Doctors need a SCHEDULED or COMPLETED appointment relationship with the patient.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Encounter history listed"),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "User cannot access this patient's clinical records",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public Page<EncounterResponse> getPatientEncounters(
            @Parameter(description = "Patient profile ID") @PathVariable Long patientId,
            @ParameterObject Pageable pageable,
            Authentication authentication
    ) {
        return encounterService.getPatientEncounters(patientId, pageable, authentication);
    }
}
