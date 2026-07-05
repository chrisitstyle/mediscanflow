package com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak.representation;

/**
 * Minimal Keycloak realm role representation returned by the Admin API.
 *
 * <p>Only the fields required for application role mapping and role assignment
 * are modeled here.</p>
 */
public record KeycloakRoleRepresentation(
        String id,
        String name
) {
}
