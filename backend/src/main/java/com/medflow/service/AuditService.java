package com.medflow.service;

import com.medflow.dto.AuditLogResponse;
import com.medflow.entity.AuditAction;
import com.medflow.entity.AuditLog;
import com.medflow.entity.User;
import com.medflow.exception.BadRequestException;
import com.medflow.repository.AuditLogRepository;
import com.medflow.repository.UserRepository;
import com.medflow.security.CustomUserDetails;
import com.medflow.util.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final DtoMapper dtoMapper;

    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable)
                .map(dtoMapper::toAuditLogResponse);
    }

    public Page<AuditLogResponse> getAuditLogs(AuditAction action, Long userId, Pageable pageable) {
        if (action != null && userId != null) {
            return auditLogRepository
                    .findByUserIdAndActionOrderByTimestampDesc(userId, action.name(), pageable)
                    .map(dtoMapper::toAuditLogResponse);
        }

        if (action != null) {
            return auditLogRepository.findByActionOrderByTimestampDesc(action.name(), pageable)
                    .map(dtoMapper::toAuditLogResponse);
        }

        if (userId != null) {
            return getAuditLogsForUser(userId, pageable);
        }

        return getAuditLogs(pageable);
    }

    private Page<AuditLogResponse> getAuditLogsForUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable)
                .map(dtoMapper::toAuditLogResponse);
    }

    @Transactional
    public void record(Authentication authentication, AuditAction action, String entityType, Long entityId) {
        record(getAuthenticatedUser(authentication), action, entityType, entityId);
    }

    @Transactional
    public void record(User user, AuditAction action, String entityType, Long entityId) {
        if (user == null) {
            throw new BadRequestException("Audit user is required");
        }
        if (action == null) {
            throw new BadRequestException("Audit action is required");
        }
        if (entityType == null || entityType.isBlank()) {
            throw new BadRequestException("Audit entity type is required");
        }
        if (entityId == null) {
            throw new BadRequestException("Audit entity id is required");
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setUser(user);
        auditLog.setAction(action.name());
        auditLog.setEntityType(entityType.trim());
        auditLog.setEntityId(entityId);
        auditLogRepository.save(auditLog);
    }

    @Transactional
    public void recordFailedLoginIfUserExists(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail.isBlank()) {
            return;
        }

        userRepository.findByEmail(normalizedEmail)
                .ifPresent(user -> record(user, AuditAction.USER_LOGIN_FAILED, "User", user.getId()));
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("Authenticated audit user is required");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return userRepository.getReferenceById(customUserDetails.getId());
        }

        throw new BadRequestException("Authenticated audit user details are required");
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase();
    }
}
