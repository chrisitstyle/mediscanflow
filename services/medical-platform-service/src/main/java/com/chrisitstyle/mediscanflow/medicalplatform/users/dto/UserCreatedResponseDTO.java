package com.chrisitstyle.mediscanflow.medicalplatform.users.dto;

import com.chrisitstyle.mediscanflow.medicalplatform.auth.UserRole;

public record UserCreatedResponseDTO(
        String id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        boolean enabled
) {
}
