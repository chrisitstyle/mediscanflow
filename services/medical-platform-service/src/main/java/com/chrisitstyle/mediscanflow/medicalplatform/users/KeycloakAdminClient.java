package com.chrisitstyle.mediscanflow.medicalplatform.users;

import com.chrisitstyle.mediscanflow.medicalplatform.auth.UserRole;
import com.chrisitstyle.mediscanflow.medicalplatform.common.exception.ResourceNotFoundException;
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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class KeycloakAdminClient {
    private static final String TOKEN_URI = "/realms/{realm}/protocol/openid-connect/token";
    private static final String USERS_URI = "/admin/realms/{realm}/users";
    private static final String USER_URI = "/admin/realms/{realm}/users/{userId}";
    private static final String USER_REALM_ROLE_MAPPING_URI = "/admin/realms/{realm}/users/{userId}/role-mappings/realm";
    private static final String REALM_ROLE_URI = "/admin/realms/{realm}/roles/{roleName}";
    private static final String CLIENT_CREDENTIALS_GRANT_TYPE = "client_credentials";
    private static final int USERS_SEARCH_MAX_RESULTS = 100;

    private final RestClient restClient;
    private final KeycloakAdminProperties properties;

    public KeycloakAdminClient(KeycloakAdminProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.serverUrl())
                .build();
    }

    public List<UserAccount> getUsers() {
        String accessToken = getAdminAccessToken();

        return getKeycloakUsers(accessToken).stream()
                .map(user -> toUserAccount(accessToken, user))
                .toList();
    }

    public UserAccount getUser(String userId) {
        String accessToken = getAdminAccessToken();
        KeycloakUserRepresentation user = getKeycloakUser(accessToken, userId);

        return toUserAccount(accessToken, user);
    }

    public long countEnabledAdmins() {
        String accessToken = getAdminAccessToken();

        return getKeycloakUsers(accessToken).stream()
                .map(user -> toUserAccount(accessToken, user))
                .filter(UserAccount::enabled)
                .filter(user -> user.hasRole(UserRole.ADMIN))
                .count();
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

    public void updateUserEnabled(String userId, boolean enabled) {
        String accessToken = getAdminAccessToken();

        try {
            restClient.put()
                    .uri(USER_URI, properties.realm(), userId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("enabled", enabled))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound exception) {
            throw userNotFound(userId, exception);
        } catch (HttpClientErrorException exception) {
            throw new UserManagementException(
                    "Keycloak rejected user status update request: " + exception.getStatusCode(),
                    exception
            );
        }
    }

    private List<KeycloakUserRepresentation> getKeycloakUsers(String accessToken) {
        try {
            KeycloakUserRepresentation[] users = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(USERS_URI)
                            .queryParam("max", USERS_SEARCH_MAX_RESULTS)
                            .build(properties.realm()))
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .retrieve()
                    .body(KeycloakUserRepresentation[].class);

            if (users == null) {
                return List.of();
            }

            return Arrays.asList(users);
        } catch (HttpClientErrorException exception) {
            throw new UserManagementException(
                    "Could not load users from Keycloak: " + exception.getStatusCode(),
                    exception
            );
        }
    }

    private KeycloakUserRepresentation getKeycloakUser(String accessToken, String userId) {
        try {
            KeycloakUserRepresentation user = restClient.get()
                    .uri(USER_URI, properties.realm(), userId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .retrieve()
                    .body(KeycloakUserRepresentation.class);

            if (user == null) {
                throw new ResourceNotFoundException("User not found with id: " + userId);
            }

            return user;
        } catch (HttpClientErrorException.NotFound exception) {
            throw userNotFound(userId, exception);
        } catch (HttpClientErrorException exception) {
            throw new UserManagementException(
                    "Could not load user from Keycloak: " + exception.getStatusCode(),
                    exception
            );
        }
    }

    private UserAccount toUserAccount(String accessToken, KeycloakUserRepresentation user) {
        return new UserAccount(
                user.id(),
                resolveEmail(user),
                user.firstName(),
                user.lastName(),
                getApplicationRoles(accessToken, user.id()),
                Boolean.TRUE.equals(user.enabled())
        );
    }

    private String resolveEmail(KeycloakUserRepresentation user) {
        if (user.email() != null && !user.email().isBlank()) {
            return user.email();
        }

        return user.username();
    }

    private Set<UserRole> getApplicationRoles(String accessToken, String userId) {
        try {
            KeycloakRoleRepresentation[] roles = restClient.get()
                    .uri(USER_REALM_ROLE_MAPPING_URI, properties.realm(), userId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
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
            throw userNotFound(userId, exception);
        } catch (HttpClientErrorException exception) {
            throw new UserManagementException(
                    "Could not load user roles from Keycloak: " + exception.getStatusCode(),
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

    private ResourceNotFoundException userNotFound(String userId, Throwable cause) {
        return new ResourceNotFoundException("User not found with id: " + userId, cause);
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

    private record KeycloakUserRepresentation(
            String id,
            String username,
            String email,
            String firstName,
            String lastName,
            Boolean enabled
    ) {
    }
}
