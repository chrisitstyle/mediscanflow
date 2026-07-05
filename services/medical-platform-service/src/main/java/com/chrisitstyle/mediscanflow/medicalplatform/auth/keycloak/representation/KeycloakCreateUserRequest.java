package com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak.representation;

import java.util.List;

/**
 * Request payload used to create a user account in Keycloak.
 *
 * <p>The request is tailored to the user-management flow in MediScanFlow and
 * creates enabled, email-verified users with an initial temporary password.</p>
 */
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

    /**
     * Keycloak credential representation used for the initial user password.
     */
    public record Credential(
            String type,
            String value,
            boolean temporary
    ) {
    }
}
