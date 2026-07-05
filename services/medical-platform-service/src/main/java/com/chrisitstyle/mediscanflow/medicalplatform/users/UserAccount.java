package com.chrisitstyle.mediscanflow.medicalplatform.users;

import com.chrisitstyle.mediscanflow.medicalplatform.auth.UserRole;
import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.UserStatusDTO;

import java.util.Set;

record UserAccount(
        String id,
        String email,
        String firstName,
        String lastName,
        Set<UserRole> roles,
        boolean enabled
) {
    UserAccount {
        roles = Set.copyOf(roles);
    }

    boolean hasRole(UserRole role) {
        return roles.contains(role);
    }

    UserStatusDTO status() {
        return UserStatusDTO.fromEnabled(enabled);
    }

    UserAccount withEnabled(boolean enabled) {
        return new UserAccount(id, email, firstName, lastName, roles, enabled);
    }
}
