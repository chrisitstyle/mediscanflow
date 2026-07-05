package com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak.representation;

/**
 * Minimal Keycloak user representation used by the application user-management flow.
 *
 * <p>The record intentionally models only the fields needed to expose application
 * users and map them to {@code UserAccount}.</p>
 */
public record KeycloakUserRepresentation(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        Boolean enabled
) {
}
