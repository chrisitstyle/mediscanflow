package com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak;

import com.chrisitstyle.mediscanflow.medicalplatform.auth.UserRole;
import com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak.representation.KeycloakCreateUserRequest;
import com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak.representation.KeycloakUserRepresentation;
import com.chrisitstyle.mediscanflow.medicalplatform.users.UserAccount;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Application-facing facade for identity management operations backed by Keycloak.
 *
 * <p>This component hides Keycloak-specific HTTP clients from the users module and
 * exposes operations in terms of application user accounts and roles.</p>
 */
@Component
public class KeycloakIdentityProvider {

    private final KeycloakAdminTokenClient tokenClient;
    private final KeycloakUsersClient usersClient;
    private final KeycloakRolesClient rolesClient;
    private final KeycloakUserMapper userMapper;

    KeycloakIdentityProvider(
            KeycloakAdminTokenClient tokenClient,
            KeycloakUsersClient usersClient,
            KeycloakRolesClient rolesClient,
            KeycloakUserMapper userMapper
    ) {
        this.tokenClient = tokenClient;
        this.usersClient = usersClient;
        this.rolesClient = rolesClient;
        this.userMapper = userMapper;
    }

    public List<UserAccount> getUsers() {
        String accessToken = tokenClient.getAccessToken();

        return usersClient.getUsers(accessToken).stream()
                .map(user -> toUserAccount(accessToken, user))
                .toList();
    }

    public UserAccount getUser(String userId) {
        String accessToken = tokenClient.getAccessToken();
        KeycloakUserRepresentation user = usersClient.getUser(accessToken, userId);

        return toUserAccount(accessToken, user);
    }

    public long countEnabledAdmins() {
        String accessToken = tokenClient.getAccessToken();

        return usersClient.getUsers(accessToken).stream()
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
        String accessToken = tokenClient.getAccessToken();

        KeycloakCreateUserRequest request = KeycloakCreateUserRequest.withTemporaryPassword(
                firstName,
                lastName,
                email,
                temporaryPassword
        );

        String userId = usersClient.createUser(accessToken, request);
        rolesClient.assignRealmRole(accessToken, userId, role);

        return userId;
    }

    public void updateUserEnabled(String userId, boolean enabled) {
        String accessToken = tokenClient.getAccessToken();

        usersClient.updateUserEnabled(accessToken, userId, enabled);
    }

    private UserAccount toUserAccount(String accessToken, KeycloakUserRepresentation user) {
        return userMapper.toUserAccount(
                user,
                rolesClient.getApplicationRoles(accessToken, user.id())
        );
    }
}
