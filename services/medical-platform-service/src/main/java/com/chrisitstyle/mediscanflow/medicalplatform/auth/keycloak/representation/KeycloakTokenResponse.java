package com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak.representation;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response payload returned by Keycloak when requesting an admin access token.
 */
public record KeycloakTokenResponse(
        @JsonProperty("access_token")
        String accessToken
) {
}
