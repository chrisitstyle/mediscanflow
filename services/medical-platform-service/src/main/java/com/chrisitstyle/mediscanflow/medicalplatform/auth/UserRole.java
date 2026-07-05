package com.chrisitstyle.mediscanflow.medicalplatform.auth;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Application roles supported by MediScanFlow.
 *
 * <p>Roles are used both as domain-level user roles and as Spring Security
 * authorities with the {@code ROLE_} prefix.</p>
 */
public enum UserRole {
    ADMIN,
    DOCTOR,
    STAFF;

    private static final String ROLE_PREFIX = "ROLE_";

    public String authority() {
        return ROLE_PREFIX + name();
    }

    public static Optional<UserRole> fromAuthority(String authority) {
        if (authority == null || !authority.startsWith(ROLE_PREFIX)) {
            return Optional.empty();
        }

        return fromName(authority.substring(ROLE_PREFIX.length()));
    }

    public static Optional<UserRole> fromName(String name) {
        return Arrays.stream(values())
                .filter(role -> role.name().equals(name))
                .findFirst();
    }

    public static Set<String> names() {
        return Arrays.stream(values())
                .map(UserRole::name)
                .collect(Collectors.toSet());
    }
}
