package com.medflow.controller;

import com.medflow.dto.ApiErrorResponse;
import com.medflow.dto.PrescriptionRequest;
import com.medflow.dto.PrescriptionResponse;
import com.medflow.service.PrescriptionService;
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
@Tag(name = "Prescriptions", description = "Prescription APIs")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping("/encounters/{encounterId}/prescriptions")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Add prescription", description = "DOCTOR only. MedFlow stores prescription data but does not provide medical decision support.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Prescription added",
                    content = @Content(schema = @Schema(implementation = PrescriptionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Doctor cannot modify this encounter",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Encounter not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<PrescriptionResponse> addPrescription(
            @Parameter(description = "Encounter ID") @PathVariable Long encounterId,
            @Valid @RequestBody PrescriptionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(prescriptionService.addPrescription(encounterId, request, authentication));
    }

    @GetMapping("/patients/{patientId}/prescriptions")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
    @Operation(summary = "List patient prescriptions", description = "Patients can view their own prescriptions. Doctors need a SCHEDULED or COMPLETED appointment relationship with the patient.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prescriptions listed"),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "User cannot access this patient's clinical records",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public Page<PrescriptionResponse> getPatientPrescriptions(
            @Parameter(description = "Patient profile ID") @PathVariable Long patientId,
            @ParameterObject Pageable pageable,
            Authentication authentication
    ) {
        return prescriptionService.getPatientPrescriptions(patientId, pageable, authentication);
    }
}
