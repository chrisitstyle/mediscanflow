package com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties required to access the Keycloak Admin API.
 *
 * <p>The values are bound from properties prefixed with
 * {@code mediscanflow.keycloak-admin}.</p>
 */
@ConfigurationProperties(prefix = "mediscanflow.keycloak-admin")
public record KeycloakAdminProperties(
        String serverUrl,
        String realm,
        String clientId,
        String clientSecret
) {
}
