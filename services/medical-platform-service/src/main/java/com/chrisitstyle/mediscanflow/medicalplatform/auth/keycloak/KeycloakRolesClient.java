package com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak;

import com.chrisitstyle.mediscanflow.medicalplatform.auth.UserRole;
import com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak.representation.KeycloakRoleRepresentation;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.UserManagementException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
class KeycloakRolesClient {

    private static final String USER_REALM_ROLE_MAPPING_URI =
            "/admin/realms/{realm}/users/{userId}/role-mappings/realm";
    private static final String REALM_ROLE_URI =
            "/admin/realms/{realm}/roles/{roleName}";

    private final RestClient restClient;
    private final KeycloakAdminProperties properties;

    KeycloakRolesClient(
            @Qualifier("keycloakRestClient") RestClient restClient,
            KeycloakAdminProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    Set<UserRole> getApplicationRoles(String accessToken, String userId) {
        try {
            KeycloakRoleRepresentation[] roles = restClient.get()
                    .uri(USER_REALM_ROLE_MAPPING_URI, properties.realm(), userId)
                    .header(HttpHeaders.AUTHORIZATION, KeycloakHttpSupport.bearer(accessToken))
                    .retrieve()
                    .body(KeycloakRoleRepresentation[].class);

            if (roles == null) {
                return Set.of();
            }

            return Arrays.stream(roles)
                    .map(KeycloakRoleRepresentation::name)
                    .map(UserRole::fromName)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(UserRole.class)));
        } catch (HttpClientErrorException.NotFound exception) {
            throw KeycloakHttpSupport.userNotFound(userId, exception);
        } catch (RestClientResponseException exception) {
            throw new UserManagementException(
                    "Could not load user roles from Keycloak: " + exception.getStatusCode(),
                    exception
            );
        } catch (RestClientException exception) {
            throw new UserManagementException(
                    "Could not connect to Keycloak while loading user roles.",
                    exception
            );
        }
    }

    void assignRealmRole(String accessToken, String userId, UserRole role) {
        KeycloakRoleRepresentation roleRepresentation = getRealmRole(accessToken, role);

        try {
            restClient.post()
                    .uri(USER_REALM_ROLE_MAPPING_URI, properties.realm(), userId)
                    .header(HttpHeaders.AUTHORIZATION, KeycloakHttpSupport.bearer(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(roleRepresentation))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw new UserManagementException(
                    "Could not assign role " + role.name() + " to user: " + exception.getStatusCode(),
                    exception
            );
        } catch (RestClientException exception) {
            throw new UserManagementException(
                    "Could not connect to Keycloak while assigning role " + role.name() + " to user.",
                    exception
            );
        }
    }

    private KeycloakRoleRepresentation getRealmRole(String accessToken, UserRole role) {
        try {
            KeycloakRoleRepresentation roleRepresentation = restClient.get()
                    .uri(REALM_ROLE_URI, properties.realm(), role.name())
                    .header(HttpHeaders.AUTHORIZATION, KeycloakHttpSupport.bearer(accessToken))
                    .retrieve()
                    .body(KeycloakRoleRepresentation.class);

            if (roleRepresentation == null) {
                throw new UserManagementException("Keycloak role not found: " + role.name());
            }

            return roleRepresentation;
        } catch (RestClientResponseException exception) {
            throw new UserManagementException(
                    "Could not load Keycloak role " + role.name() + ": " + exception.getStatusCode(),
                    exception
            );
        } catch (RestClientException exception) {
            throw new UserManagementException(
                    "Could not connect to Keycloak while loading role " + role.name() + ".",
                    exception
            );
        }
    }
}
