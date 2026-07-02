package com.chrisitstyle.mediscanflow.medicalplatform.auth;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public enum UserRole {
    ADMIN,
    DOCTOR,
    STAFF;

    public String authority() {
        return "ROLE_" + name();
    }

    public static Optional<UserRole> fromAuthority(String authority) {
        if (authority == null || !authority.startsWith("ROLE_")) {
            return Optional.empty();
        }

        return fromName(authority.substring("ROLE_".length()));
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
