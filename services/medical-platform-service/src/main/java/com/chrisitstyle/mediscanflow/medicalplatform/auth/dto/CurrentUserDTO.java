package com.chrisitstyle.mediscanflow.medicalplatform.auth.dto;

import java.util.Set;

public record CurrentUserDTO(
        String id,
        String email,
        String firstName,
        String lastName,
        Set<String> roles
) {
}
