package com.chrisitstyle.mediscanflow.medicalplatform.users;

import com.chrisitstyle.mediscanflow.medicalplatform.auth.UserRole;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.UserManagementException;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Component
public class KeycloakAdminClient {

    private static final String TOKEN_URI =
            "/realms/{realm}/protocol/openid-connect/token";

    private static final String USERS_URI =
            "/admin/realms/{realm}/users";

    private static final String USER_REALM_ROLE_MAPPING_URI =
            "/admin/realms/{realm}/users/{userId}/role-mappings/realm";

    private static final String REALM_ROLE_URI =
            "/admin/realms/{realm}/roles/{roleName}";

    private static final String CLIENT_CREDENTIALS_GRANT_TYPE =
            "client_credentials";

    private final RestClient restClient;
    private final KeycloakAdminProperties properties;

    public KeycloakAdminClient(KeycloakAdminProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.serverUrl())
                .build();
    }

    public String createUser(
            String firstName,
            String lastName,
            String email,
            String temporaryPassword,
            UserRole role
    ) {
        String accessToken = getAdminAccessToken();

        try {
            URI location = restClient.post()
                    .uri(USERS_URI, properties.realm())
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(createUserPayload(firstName, lastName, email, temporaryPassword))
                    .retrieve()
                    .toBodilessEntity()
                    .getHeaders()
                    .getLocation();

            if (location == null) {
                throw new UserManagementException("Keycloak did not return created user location.");
            }

            String userId = extractUserId(location);

            assignRealmRole(accessToken, userId, role);

            return userId;
        } catch (HttpClientErrorException.Conflict exception) {
            throw new UserManagementException("User with this email already exists.", exception);
        } catch (HttpClientErrorException exception) {
            throw new UserManagementException(
                    "Keycloak rejected user creation request: " + exception.getStatusCode(),
                    exception
            );
        }
    }

    private String getAdminAccessToken() {
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
        } catch (HttpClientErrorException exception) {
            throw new UserManagementException(
                    "Could not obtain Keycloak admin access token: " + exception.getStatusCode(),
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

    private Map<String, Object> createUserPayload(
            String firstName,
            String lastName,
            String email,
            String temporaryPassword
    ) {
        return Map.of(
                "username", email,
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "enabled", true,
                "emailVerified", true,
                "credentials", List.of(
                        Map.of(
                                "type", "password",
                                "value", temporaryPassword,
                                "temporary", true
                        )
                )
        );
    }

    private void assignRealmRole(String accessToken, String userId, UserRole role) {
        KeycloakRoleRepresentation roleRepresentation = getRealmRole(accessToken, role);

        try {
            restClient.post()
                    .uri(
                            USER_REALM_ROLE_MAPPING_URI,
                            properties.realm(),
                            userId
                    )
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(roleRepresentation))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException exception) {
            throw new UserManagementException(
                    "Could not assign role " + role.name() + " to user.",
                    exception
            );
        }
    }

    private KeycloakRoleRepresentation getRealmRole(String accessToken, UserRole role) {
        try {
            KeycloakRoleRepresentation roleRepresentation = restClient.get()
                    .uri(REALM_ROLE_URI, properties.realm(), role.name())
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .retrieve()
                    .body(KeycloakRoleRepresentation.class);

            if (roleRepresentation == null) {
                throw new UserManagementException("Keycloak role not found: " + role.name());
            }

            return roleRepresentation;
        } catch (HttpClientErrorException exception) {
            throw new UserManagementException(
                    "Could not load Keycloak role: " + role.name(),
                    exception
            );
        }
    }

    private String extractUserId(URI location) {
        String path = location.getPath();
        int lastSlashIndex = path.lastIndexOf('/');

        if (lastSlashIndex < 0 || lastSlashIndex == path.length() - 1) {
            throw new UserManagementException("Could not extract created user id from Keycloak location.");
        }

        return path.substring(lastSlashIndex + 1);
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private record KeycloakTokenResponse(

            @JsonProperty("access_token")
            String accessToken
    ) {
    }

    private record KeycloakRoleRepresentation(
            String id,
            String name
    ) {
    }
}