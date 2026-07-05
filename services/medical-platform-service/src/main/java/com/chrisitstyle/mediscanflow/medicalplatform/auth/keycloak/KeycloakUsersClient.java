package com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak;

import com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak.representation.KeycloakCreateUserRequest;
import com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak.representation.KeycloakUserRepresentation;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.ResourceNotFoundException;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.UserManagementException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Client responsible for user-related operations against the Keycloak Admin API.
 *
 * <p>It loads users, creates new users and updates the enabled status of existing
 * Keycloak accounts.</p>
 */
@Component
class KeycloakUsersClient {

    private static final String USERS_URI = "/admin/realms/{realm}/users";
    private static final String USER_URI = "/admin/realms/{realm}/users/{userId}";
    private static final int USERS_SEARCH_MAX_RESULTS = 100;

    private final RestClient restClient;
    private final KeycloakAdminProperties properties;

    KeycloakUsersClient(
            @Qualifier("keycloakRestClient") RestClient restClient,
            KeycloakAdminProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    List<KeycloakUserRepresentation> getUsers(String accessToken) {
        try {
            KeycloakUserRepresentation[] users = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(USERS_URI)
                            .queryParam("max", USERS_SEARCH_MAX_RESULTS)
                            .build(properties.realm()))
                    .header(HttpHeaders.AUTHORIZATION, KeycloakHttpSupport.bearer(accessToken))
                    .retrieve()
                    .body(KeycloakUserRepresentation[].class);

            if (users == null) {
                return List.of();
            }

            return Arrays.asList(users);
        } catch (RestClientResponseException exception) {
            throw new UserManagementException(
                    "Could not load users from Keycloak: " + exception.getStatusCode(),
                    exception
            );
        } catch (RestClientException exception) {
            throw new UserManagementException(
                    "Could not connect to Keycloak while loading users.",
                    exception
            );
        }
    }

    KeycloakUserRepresentation getUser(String accessToken, String userId) {
        try {
            KeycloakUserRepresentation user = restClient.get()
                    .uri(USER_URI, properties.realm(), userId)
                    .header(HttpHeaders.AUTHORIZATION, KeycloakHttpSupport.bearer(accessToken))
                    .retrieve()
                    .body(KeycloakUserRepresentation.class);

            if (user == null) {
                throw new ResourceNotFoundException("User not found with id: " + userId);
            }

            return user;
        } catch (HttpClientErrorException.NotFound exception) {
            throw KeycloakHttpSupport.userNotFound(userId, exception);
        } catch (RestClientResponseException exception) {
            throw new UserManagementException(
                    "Could not load user from Keycloak: " + exception.getStatusCode(),
                    exception
            );
        } catch (RestClientException exception) {
            throw new UserManagementException(
                    "Could not connect to Keycloak while loading user.",
                    exception
            );
        }
    }

    String createUser(String accessToken, KeycloakCreateUserRequest request) {
        try {
            URI location = restClient.post()
                    .uri(USERS_URI, properties.realm())
                    .header(HttpHeaders.AUTHORIZATION, KeycloakHttpSupport.bearer(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity()
                    .getHeaders()
                    .getLocation();

            if (location == null) {
                throw new UserManagementException("Keycloak did not return created user location.");
            }

            return KeycloakHttpSupport.extractUserId(location);
        } catch (HttpClientErrorException.Conflict exception) {
            throw new UserManagementException("User with this email already exists.", exception);
        } catch (RestClientResponseException exception) {
            throw new UserManagementException(
                    "Keycloak rejected user creation request: " + exception.getStatusCode(),
                    exception
            );
        } catch (RestClientException exception) {
            throw new UserManagementException(
                    "Could not connect to Keycloak while creating user.",
                    exception
            );
        }
    }

    void updateUserEnabled(String accessToken, String userId, boolean enabled) {
        try {
            restClient.put()
                    .uri(USER_URI, properties.realm(), userId)
                    .header(HttpHeaders.AUTHORIZATION, KeycloakHttpSupport.bearer(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("enabled", enabled))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound exception) {
            throw KeycloakHttpSupport.userNotFound(userId, exception);
        } catch (RestClientResponseException exception) {
            throw new UserManagementException(
                    "Keycloak rejected user status update request: " + exception.getStatusCode(),
                    exception
            );
        } catch (RestClientException exception) {
            throw new UserManagementException(
                    "Could not connect to Keycloak while updating user status.",
                    exception
            );
        }
    }
}