package com.medflow.controller;

import com.medflow.dto.ApiErrorResponse;
import com.medflow.dto.AppointmentRequest;
import com.medflow.dto.AppointmentResponse;
import com.medflow.dto.AppointmentSlotResponse;
import com.medflow.entity.AppointmentStatus;
import com.medflow.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Appointments", description = "Appointment booking, listing, viewing, and cancellation")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(
            summary = "Book appointment",
            description = "PATIENT only. appointmentDateTime must be a future 30-minute slot. Patient identity comes from the JWT, so the request must not contain patientId."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Appointment booked",
                    content = @Content(schema = @Schema(implementation = AppointmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or inactive doctor",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Only PATIENT users can book appointments",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Doctor not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Doctor already has a scheduled appointment at this time",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<AppointmentResponse> bookAppointment(
            @Valid @RequestBody AppointmentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.bookAppointment(request, authentication));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    @Operation(summary = "List appointments", description = "Returns appointments visible to the authenticated role. Supports page, size, and optional status filter.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointments listed"),
            @ApiResponse(responseCode = "400", description = "Invalid status filter",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public Page<AppointmentResponse> getAppointments(
            @Parameter(description = "Optional appointment status filter", example = "SCHEDULED")
            @RequestParam(required = false) AppointmentStatus status,
            @ParameterObject Pageable pageable,
            Authentication authentication
    ) {
        return appointmentService.getAppointments(status, pageable, authentication);
    }

    @GetMapping("/available-slots")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "List available appointment slots", description = "PATIENT only. Returns future 30-minute slots for an active doctor on the selected date.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Available slots listed"),
            @ApiResponse(responseCode = "400", description = "Invalid date or inactive doctor",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Only PATIENT users can view bookable slots",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Doctor not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public List<AppointmentSlotResponse> getAvailableSlots(
            @Parameter(description = "Doctor profile ID", example = "4")
            @RequestParam Long doctorId,
            @Parameter(description = "Appointment date", example = "2026-09-10")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam LocalDate date
    ) {
        return appointmentService.getAvailableSlots(doctorId, date);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    @Operation(summary = "Get appointment by ID", description = "Patients and doctors can access only their own related appointments. ADMIN can access all appointments.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment found",
                    content = @Content(schema = @Schema(implementation = AppointmentResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "User cannot access this appointment",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Appointment not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public AppointmentResponse getAppointment(
            @Parameter(description = "Appointment ID") @PathVariable Long id,
            Authentication authentication
    ) {
        return appointmentService.getAppointment(id, authentication);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Cancel appointment", description = "PATIENT only. Patients can cancel only their own scheduled appointments.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment cancelled",
                    content = @Content(schema = @Schema(implementation = AppointmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Appointment is not scheduled",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "User cannot cancel this appointment",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Appointment not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public AppointmentResponse cancelAppointment(
            @Parameter(description = "Appointment ID") @PathVariable Long id,
            Authentication authentication
    ) {
        return appointmentService.cancelAppointment(id, authentication);
    }
}
