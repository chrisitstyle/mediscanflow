package com.chrisitstyle.mediscanflow.medicalplatform.users.dto;

import com.chrisitstyle.mediscanflow.medicalplatform.auth.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequestDTO(
        @NotBlank(message = "First name is required")
        @Size(min = 1)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 1)
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotNull(message = "Role is required")
        UserRole role,

        @NotBlank(message = "Temporary password is required")
        @Size(min = 8, message = "Temporary password must be at least 8 characters")
        String temporaryPassword
) {
}
