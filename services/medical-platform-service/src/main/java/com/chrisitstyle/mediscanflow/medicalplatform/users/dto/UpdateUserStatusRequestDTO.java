package com.chrisitstyle.mediscanflow.medicalplatform.users.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequestDTO(
        @NotNull(message = "Status is required")
        UserStatusDTO status
) {
}
