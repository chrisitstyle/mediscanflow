package com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak.representation;

import java.util.List;

public record KeycloakCreateUserRequest(
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        boolean emailVerified,
        List<Credential> credentials
) {

    private static final String PASSWORD_CREDENTIAL_TYPE = "password";

    public static KeycloakCreateUserRequest withTemporaryPassword(
            String firstName,
            String lastName,
            String email,
            String temporaryPassword
    ) {
        return new KeycloakCreateUserRequest(
                email,
                email,
                firstName,
                lastName,
                true,
                true,
                List.of(new Credential(PASSWORD_CREDENTIAL_TYPE, temporaryPassword, true))
        );
    }

    public record Credential(
            String type,
            String value,
            boolean temporary
    ) {
    }
}
