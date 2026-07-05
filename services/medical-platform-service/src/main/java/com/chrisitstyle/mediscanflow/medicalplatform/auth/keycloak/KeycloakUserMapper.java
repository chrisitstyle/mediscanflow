package com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak;

import com.chrisitstyle.mediscanflow.medicalplatform.auth.UserRole;
import com.chrisitstyle.mediscanflow.medicalplatform.auth.keycloak.representation.KeycloakUserRepresentation;
import com.chrisitstyle.mediscanflow.medicalplatform.users.UserAccount;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
class KeycloakUserMapper {

    UserAccount toUserAccount(KeycloakUserRepresentation user, Set<UserRole> roles) {
        return new UserAccount(
                user.id(),
                resolveEmail(user),
                user.firstName(),
                user.lastName(),
                roles,
                Boolean.TRUE.equals(user.enabled())
        );
    }

    private String resolveEmail(KeycloakUserRepresentation user) {
        if (user.email() != null && !user.email().isBlank()) {
            return user.email();
        }

        return user.username();
    }
}