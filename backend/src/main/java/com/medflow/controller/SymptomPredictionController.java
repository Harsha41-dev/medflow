package com.medflow.controller;

import com.medflow.dto.ApiErrorResponse;
import com.medflow.dto.SymptomModelInfoResponse;
import com.medflow.dto.SymptomPredictionHealthResponse;
import com.medflow.dto.SymptomPredictionRequest;
import com.medflow.dto.SymptomPredictionResponse;
import com.medflow.service.SymptomPredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ml")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Symptom Pattern Classifier", description = "Doctor-only educational ML-assisted symptom pattern API")
public class SymptomPredictionController {

    private final SymptomPredictionService symptomPredictionService;

    @GetMapping("/symptom-pattern/health")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
            summary = "Check symptom pattern ML service health",
            description = "DOCTOR only. Checks whether the FastAPI symptom-pattern service is reachable through the backend."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Health checked",
                    content = @Content(schema = @Schema(implementation = SymptomPredictionHealthResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Only doctors can check the classifier service",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<SymptomPredictionHealthResponse> getHealth() {
        return ResponseEntity.ok(symptomPredictionService.getHealth());
    }

    @GetMapping("/symptom-pattern/model-info")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
            summary = "Get symptom pattern ML model metadata",
            description = "DOCTOR only. Returns metadata about the saved model loaded by the FastAPI ML service."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Model metadata returned",
                    content = @Content(schema = @Schema(implementation = SymptomModelInfoResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Only doctors can view classifier model metadata",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "ML service unavailable",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<SymptomModelInfoResponse> getModelInfo() {
        return ResponseEntity.ok(symptomPredictionService.getModelInfo());
    }

    @PostMapping("/symptom-pattern")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
            summary = "Classify broad symptom pattern",
            description = "DOCTOR only. Educational prediction only. Not a medical diagnosis or treatment recommendation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prediction returned"),
            @ApiResponse(responseCode = "400", description = "Invalid symptom request",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Only doctors can use the classifier",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "ML service unavailable",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<SymptomPredictionResponse> predict(
            @Valid @RequestBody SymptomPredictionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(symptomPredictionService.predict(request, authentication));
    }
}
