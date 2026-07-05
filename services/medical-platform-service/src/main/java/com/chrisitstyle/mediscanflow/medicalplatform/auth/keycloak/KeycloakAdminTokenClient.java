package com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak;

import com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak.representation.KeycloakTokenResponse;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.UserManagementException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Client responsible for obtaining access tokens for Keycloak Admin API calls.
 *
 * <p>It uses the client credentials grant configured in {@link KeycloakAdminProperties}
 * and returns bearer tokens used by the other Keycloak admin clients.</p>
 */
@Component
class KeycloakAdminTokenClient {

    private static final String TOKEN_URI = "/realms/{realm}/protocol/openid-connect/token";
    private static final String CLIENT_CREDENTIALS_GRANT_TYPE = "client_credentials";

    private final RestClient restClient;
    private final KeycloakAdminProperties properties;

    KeycloakAdminTokenClient(
            @Qualifier("keycloakRestClient") RestClient restClient,
            KeycloakAdminProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    String getAccessToken() {
        try {
            KeycloakTokenResponse response = restClient.post()
                    .uri(TOKEN_URI, properties.realm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(createTokenRequestBody())
                    .retrieve()
                    .body(KeycloakTokenResponse.class);

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new UserManagementException("Could not obtain Keycloak admin access token.");
            }

            return response.accessToken();
        } catch (RestClientResponseException exception) {
            throw new UserManagementException(
                    "Could not obtain Keycloak admin access token: " + exception.getStatusCode(),
                    exception
            );
        } catch (RestClientException exception) {
            throw new UserManagementException(
                    "Could not connect to Keycloak while obtaining admin access token.",
                    exception
            );
        }
    }

    private MultiValueMap<String, String> createTokenRequestBody() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", CLIENT_CREDENTIALS_GRANT_TYPE);
        body.add("client_id", properties.clientId());
        body.add("client_secret", properties.clientSecret());

        return body;
    }
}
