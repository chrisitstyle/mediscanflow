package com.chrisitstyle.mediscanflow.medicalplatform.users.dto;

import com.chrisitstyle.mediscanflow.medicalplatform.auth.UserRole;

import java.util.Set;

public record UserDTO(
        String id,
        String email,
        String firstName,
        String lastName,
        Set<UserRole> roles,
        UserStatusDTO status
) {
}
