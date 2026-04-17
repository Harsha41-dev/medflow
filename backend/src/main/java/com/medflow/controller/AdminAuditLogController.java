package com.medflow.controller;

import com.medflow.dto.ApiErrorResponse;
import com.medflow.dto.AuditLogResponse;
import com.medflow.entity.AuditAction;
import com.medflow.service.AuditService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Audit Logs", description = "ADMIN-only audit log viewing")
public class AdminAuditLogController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List audit logs", description = "ADMIN only. Audit logs contain action metadata, not raw passwords, JWTs, clinical notes, or prescription contents.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit logs listed"),
            @ApiResponse(responseCode = "400", description = "Invalid action filter",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Only ADMIN users can view audit logs",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public Page<AuditLogResponse> getAuditLogs(
            @Parameter(description = "Optional action filter", example = "APPOINTMENT_CREATED")
            @RequestParam(required = false) AuditAction action,
            @Parameter(description = "Optional user ID filter", example = "1")
            @RequestParam(required = false) Long userId,
            @ParameterObject Pageable pageable
    ) {
        return auditService.getAuditLogs(action, userId, pageable);
    }
}
