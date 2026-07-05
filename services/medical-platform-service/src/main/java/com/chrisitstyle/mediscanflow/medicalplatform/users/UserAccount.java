package com.chrisitstyle.mediscanflow.medicalplatform.users;

import com.chrisitstyle.mediscanflow.medicalplatform.auth.UserRole;
import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.UserStatusDTO;

import java.util.Set;

public record UserAccount(
        String id,
        String email,
        String firstName,
        String lastName,
        Set<UserRole> roles,
        boolean enabled
) {

    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }

    public UserStatusDTO status() {
        return enabled ? UserStatusDTO.ENABLED : UserStatusDTO.DISABLED;
    }

    public UserAccount withEnabled(boolean enabled) {
        return new UserAccount(
                id,
                email,
                firstName,
                lastName,
                roles,
                enabled
        );
    }
}