package com.medflow.dto;

import com.medflow.entity.Role;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        Role role,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
