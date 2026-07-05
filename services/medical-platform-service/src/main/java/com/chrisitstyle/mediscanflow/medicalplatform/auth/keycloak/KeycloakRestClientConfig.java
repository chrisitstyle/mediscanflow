package com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Spring configuration for the {@link RestClient} used to call the Keycloak Admin API.
 */
@Configuration
class KeycloakRestClientConfig {

    @Bean
    RestClient keycloakRestClient(KeycloakAdminProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.serverUrl())
                .build();
    }
}
