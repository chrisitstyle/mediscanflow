package com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak.representation;

public record KeycloakUserRepresentation(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        Boolean enabled
) {
}
