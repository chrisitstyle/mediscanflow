package com.chrisitstyle.mediscanflow.medicalplatform.users;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mediscanflow.keycloak-admin")
public record KeycloakAdminProperties(
        String serverUrl,
        String realm,
        String clientId,
        String clientSecret
) {
}
