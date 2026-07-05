package com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak.representation;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KeycloakTokenResponse(
        @JsonProperty("access_token")
        String accessToken
) {
}
